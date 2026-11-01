package com.lentikr.reminder.ui.edit

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.lentikr.reminder.data.DateProvider
import com.lentikr.reminder.data.RecentlyDeleted
import com.lentikr.reminder.data.ReminderRepository
import com.lentikr.reminder.domain.Reminder
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.domain.Repeat
import com.lentikr.reminder.domain.RepeatUnit
import com.lentikr.reminder.ui.navigation.Edit
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

/** Everything on the form except the two text fields, which keep their own state. */
@Serializable
data class Draft(
  /** False until an existing reminder has been copied into the form. */
  val loaded: Boolean = false,
  @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
  val type: ReminderType,
  val isLunar: Boolean = false,
  val isPinned: Boolean = false,
  val repeat: DraftRepeat? = null,
  /** Set after a save attempt so validation errors only show once they matter. */
  val showErrors: Boolean = false,
)

@Serializable data class DraftRepeat(val interval: Int, val unit: RepeatUnit)

sealed interface EditEvent {
  data object Saved : EditEvent

  data object Deleted : EditEvent
}

@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel(assistedFactory = EditViewModel.Factory::class)
class EditViewModel
@AssistedInject
constructor(
  @Assisted private val key: Edit,
  private val repository: ReminderRepository,
  private val recentlyDeleted: RecentlyDeleted,
  dateProvider: DateProvider,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {

  val isNew: Boolean = key.id == null

  val title: TextFieldState by savedStateHandle.saveable(saver = TextFieldState.Saver) { TextFieldState() }
  val category: TextFieldState by
    savedStateHandle.saveable(saver = TextFieldState.Saver) { TextFieldState() }

  var draft: Draft by
    savedStateHandle.saveable(stateSaver = DraftSaver) {
      mutableStateOf(Draft(loaded = isNew, date = dateProvider.today(), type = key.type))
    }
    private set

  val categorySuggestions: StateFlow<List<String>> =
    repository.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private val events = Channel<EditEvent>(Channel.BUFFERED)
  val eventFlow: Flow<EditEvent> = events.receiveAsFlow()

  val titleError: Boolean
    get() = draft.showErrors && title.text.isBlank()

  init {
    val id = key.id
    if (id != null && !draft.loaded) {
      viewModelScope.launch {
        val reminder = repository.get(id) ?: return@launch
        title.setTextAndPlaceCursorAtEnd(reminder.title)
        category.setTextAndPlaceCursorAtEnd(reminder.category)
        draft =
          Draft(
            loaded = true,
            date = reminder.date,
            type = reminder.type,
            isLunar = reminder.isLunar,
            isPinned = reminder.isPinned,
            repeat = reminder.repeat?.let { DraftRepeat(it.interval, it.unit) },
          )
      }
    }
  }

  fun setType(type: ReminderType) = update { copy(type = type) }

  fun setDate(date: LocalDate) = update { copy(date = date) }

  fun setLunar(lunar: Boolean) = update { copy(isLunar = lunar) }

  fun setPinned(pinned: Boolean) = update { copy(isPinned = pinned) }

  fun setRepeat(repeat: Repeat?) = update { copy(repeat = repeat?.let { DraftRepeat(it.interval, it.unit) }) }

  fun pickCategory(name: String) = category.setTextAndPlaceCursorAtEnd(name)

  fun save() {
    if (title.text.isBlank()) {
      update { copy(showErrors = true) }
      return
    }
    val d = draft
    val reminder =
      Reminder(
        id = key.id ?: 0,
        title = title.text.toString(),
        date = d.date,
        type = d.type,
        isLunar = d.isLunar,
        category = category.text.toString(),
        isPinned = d.isPinned,
        repeat = d.repeat?.let { Repeat(it.interval, it.unit) },
      )
    viewModelScope.launch {
      repository.save(reminder)
      events.send(EditEvent.Saved)
    }
  }

  fun delete() {
    val id = key.id ?: return
    viewModelScope.launch {
      recentlyDeleted.post(repository.delete(listOf(id)))
      events.send(EditEvent.Deleted)
    }
  }

  private inline fun update(block: Draft.() -> Draft) {
    draft = draft.block()
  }

  @AssistedFactory
  interface Factory {
    fun create(key: Edit): EditViewModel
  }
}

private val draftJson = Json { ignoreUnknownKeys = true }

private val DraftSaver =
  Saver<Draft, String>(
    save = { draftJson.encodeToString(Draft.serializer(), it) },
    restore = { draftJson.decodeFromString(Draft.serializer(), it) },
  )

object LocalDateSerializer : KSerializer<LocalDate> {
  override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

  override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}
