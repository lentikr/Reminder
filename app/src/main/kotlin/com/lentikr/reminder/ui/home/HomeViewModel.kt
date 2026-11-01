package com.lentikr.reminder.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lentikr.reminder.data.DateProvider
import com.lentikr.reminder.data.RecentlyDeleted
import com.lentikr.reminder.data.ReminderRepository
import com.lentikr.reminder.data.settings.ListLayout
import com.lentikr.reminder.data.settings.SettingsRepository
import com.lentikr.reminder.domain.DayCount
import com.lentikr.reminder.domain.DayCounter
import com.lentikr.reminder.domain.Reminder
import com.lentikr.reminder.domain.ReminderType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Collator
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderItemUi(val reminder: Reminder, val count: DayCount) {
  val id: Long
    get() = reminder.id
}

sealed interface SectionTitle {
  data object Pinned : SectionTitle

  data object Uncategorized : SectionTitle

  data class Category(val name: String) : SectionTitle
}

/** [title] is null when the list has a single unnamed group, which needs no header. */
data class ReminderSection(val key: String, val title: SectionTitle?, val items: List<ReminderItemUi>)

data class HomeUiState(
  val loading: Boolean = true,
  val today: LocalDate = LocalDate.now(),
  val countdown: List<ReminderSection> = emptyList(),
  val countUp: List<ReminderSection> = emptyList(),
  val countdownTotal: Int = 0,
  val countUpTotal: Int = 0,
  val listLayout: ListLayout = ListLayout.CARDS,
  val selectedIds: Set<Long> = emptySet(),
) {
  val inSelection: Boolean
    get() = selectedIds.isNotEmpty()

  fun sections(type: ReminderType): List<ReminderSection> =
    if (type == ReminderType.COUNTDOWN) countdown else countUp

  /** Whether every selected reminder is pinned, which turns the pin action into "unpin". */
  val selectionAllPinned: Boolean
    get() {
      val selected = (countdown + countUp).flatMap { it.items }.filter { it.id in selectedIds }
      return selected.isNotEmpty() && selected.all { it.reminder.isPinned }
    }
}

@HiltViewModel
class HomeViewModel
@Inject
constructor(
  private val repository: ReminderRepository,
  private val settings: SettingsRepository,
  private val recentlyDeleted: RecentlyDeleted,
  dateProvider: DateProvider,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val selection = savedStateHandle.getMutableStateFlow(KEY_SELECTION, longArrayOf())

  /** Deletions from any screen, for the undo snackbar. */
  val deletions: Flow<List<Reminder>> = recentlyDeleted.events

  val uiState: StateFlow<HomeUiState> =
    combine(
        repository.observeReminders(),
        dateProvider.todayFlow,
        settings.settings,
        selection,
      ) { reminders, today, appSettings, selected ->
        val items = reminders.map { ReminderItemUi(it, DayCounter.count(it, today)) }
        val (countdown, countUp) = items.partition { it.reminder.type == ReminderType.COUNTDOWN }
        val validIds = reminders.mapTo(HashSet()) { it.id }
        HomeUiState(
          loading = false,
          today = today,
          countdown = buildSections(countdown),
          countUp = buildSections(countUp),
          countdownTotal = countdown.size,
          countUpTotal = countUp.size,
          listLayout = appSettings.listLayout,
          selectedIds = selected.filterTo(HashSet()) { it in validIds },
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

  fun toggleSelection(id: Long) =
    selection.update { current ->
      if (id in current) current.filter { it != id }.toLongArray() else current + id
    }

  fun selectAll(type: ReminderType) {
    selection.value =
      uiState.value.sections(type).flatMap { it.items }.map { it.id }.distinct().toLongArray()
  }

  fun clearSelection() {
    selection.value = longArrayOf()
  }

  fun setLayout(layout: ListLayout) {
    viewModelScope.launch { settings.setListLayout(layout) }
  }

  fun pinSelected() {
    val state = uiState.value
    val ids = state.selectedIds
    if (ids.isEmpty()) return
    viewModelScope.launch {
      repository.setPinned(ids, pinned = !state.selectionAllPinned)
      clearSelection()
    }
  }

  fun deleteSelected() {
    val ids = uiState.value.selectedIds
    if (ids.isEmpty()) return
    viewModelScope.launch {
      recentlyDeleted.post(repository.delete(ids))
      clearSelection()
    }
  }

  fun undoDelete(reminders: List<Reminder>) {
    viewModelScope.launch { repository.restoreDeleted(reminders) }
  }

  private companion object {
    const val KEY_SELECTION = "selection"
  }
}

private val collator: Collator = Collator.getInstance(Locale.CHINA)

/**
 * Pinned reminders first, then one section per category in collation order, then uncategorized.
 * Inside a section the nearest date comes first.
 */
internal fun buildSections(items: List<ReminderItemUi>): List<ReminderSection> {
  if (items.isEmpty()) return emptyList()
  val order =
    compareBy<ReminderItemUi> { DayCounter.sortKey(it.count) }
      .thenComparator { a, b -> collator.compare(a.reminder.title, b.reminder.title) }
      .thenBy { it.id }

  val (pinned, others) = items.partition { it.reminder.isPinned }
  val byCategory = others.groupBy { it.reminder.category }
  val categories = byCategory.keys.filter { it.isNotEmpty() }.sortedWith(collator)

  val sections = buildList {
    if (pinned.isNotEmpty()) add(ReminderSection("pinned", SectionTitle.Pinned, pinned.sortedWith(order)))
    for (category in categories) {
      add(ReminderSection("c:$category", SectionTitle.Category(category), byCategory.getValue(category).sortedWith(order)))
    }
    byCategory[""]?.let { add(ReminderSection("uncategorized", SectionTitle.Uncategorized, it.sortedWith(order))) }
  }
  // A single uncategorized group reads better without a header.
  return if (sections.size == 1 && sections[0].title == SectionTitle.Uncategorized) {
    listOf(sections[0].copy(title = null))
  } else {
    sections
  }
}
