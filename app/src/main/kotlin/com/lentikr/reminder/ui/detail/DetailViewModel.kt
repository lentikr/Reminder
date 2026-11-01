package com.lentikr.reminder.ui.detail

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lentikr.reminder.data.DateProvider
import com.lentikr.reminder.data.RecentlyDeleted
import com.lentikr.reminder.data.ReminderRepository
import com.lentikr.reminder.data.settings.PosterStyle
import com.lentikr.reminder.data.settings.SettingsRepository
import com.lentikr.reminder.data.share.ImageExporter
import com.lentikr.reminder.domain.DayCount
import com.lentikr.reminder.domain.DayCounter
import com.lentikr.reminder.domain.Reminder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
  data object Loading : DetailUiState

  /** The reminder no longer exists, for example after it was deleted elsewhere. */
  data object Missing : DetailUiState

  data class Loaded(val reminder: Reminder, val count: DayCount, val today: LocalDate) :
    DetailUiState
}

sealed interface DetailEvent {
  data class Share(val uri: Uri) : DetailEvent

  data object Saved : DetailEvent

  data object ExportFailed : DetailEvent

  data object Deleted : DetailEvent
}

@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel
@AssistedInject
constructor(
  @Assisted private val id: Long,
  private val repository: ReminderRepository,
  private val recentlyDeleted: RecentlyDeleted,
  private val imageExporter: ImageExporter,
  private val settings: SettingsRepository,
  dateProvider: DateProvider,
) : ViewModel() {

  val uiState: StateFlow<DetailUiState> =
    combine(repository.observeReminder(id), dateProvider.todayFlow) { reminder, today ->
        if (reminder == null) DetailUiState.Missing
        else DetailUiState.Loaded(reminder, DayCounter.count(reminder, today), today)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

  val posterStyle: StateFlow<PosterStyle> =
    settings.settings.map { it.posterStyle }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PosterStyle.WOOD)

  fun setPosterStyle(style: PosterStyle) {
    viewModelScope.launch { settings.setPosterStyle(style) }
  }

  private val events = Channel<DetailEvent>(Channel.BUFFERED)
  val eventFlow: Flow<DetailEvent> = events.receiveAsFlow()

  /** True while an image is being written, to disable the export buttons. */
  private val exporting = java.util.concurrent.atomic.AtomicBoolean(false)

  fun share(image: Bitmap) = export { events.send(DetailEvent.Share(imageExporter.prepareShare(image))) }

  fun saveImage(image: Bitmap, title: String) = export {
    imageExporter.saveToGallery(image, "Reminder-${title.take(24)}-${System.currentTimeMillis()}")
    events.send(DetailEvent.Saved)
  }

  fun delete() {
    viewModelScope.launch {
      recentlyDeleted.post(repository.delete(listOf(id)))
      events.send(DetailEvent.Deleted)
    }
  }

  private fun export(block: suspend () -> Unit) {
    if (!exporting.compareAndSet(false, true)) return
    viewModelScope.launch {
      try {
        block()
      } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        events.send(DetailEvent.ExportFailed)
      } finally {
        exporting.set(false)
      }
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(id: Long): DetailViewModel
  }
}
