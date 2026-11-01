package com.lentikr.reminder.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lentikr.reminder.data.backup.BackupRepository
import com.lentikr.reminder.data.settings.AppSettings
import com.lentikr.reminder.data.settings.SettingsRepository
import com.lentikr.reminder.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BackupResult {
  data class Exported(val count: Int) : BackupResult

  data object NothingToExport : BackupResult

  data class Restored(val count: Int) : BackupResult

  data class ExportFailed(val reason: String) : BackupResult

  data class RestoreFailed(val reason: String) : BackupResult
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(private val settingsRepository: SettingsRepository, private val backup: BackupRepository) :
  ViewModel() {

  val settings: StateFlow<AppSettings> =
    settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

  private val _busy = MutableStateFlow(false)
  /** True while a backup or restore runs; the screen shows a progress indicator. */
  val busy: StateFlow<Boolean> = _busy.asStateFlow()

  private val results = Channel<BackupResult>(Channel.BUFFERED)
  val resultFlow: Flow<BackupResult> = results.receiveAsFlow()

  fun suggestedBackupName(): String = backup.suggestedFileName()

  fun setThemeMode(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }

  fun setPureBlack(enabled: Boolean) = launch { settingsRepository.setPureBlack(enabled) }

  fun setDynamicColor(enabled: Boolean) = launch { settingsRepository.setDynamicColor(enabled) }

  fun export(uri: Uri) =
    runBackup(onError = BackupResult::ExportFailed) {
      val count = backup.export(uri)
      if (count == 0) BackupResult.NothingToExport else BackupResult.Exported(count)
    }

  fun import(uri: Uri) =
    runBackup(onError = BackupResult::RestoreFailed) { BackupResult.Restored(backup.import(uri)) }

  private fun launch(block: suspend () -> Unit) {
    viewModelScope.launch { block() }
  }

  /** Runs one backup operation at a time and reports its outcome, including failures. */
  private fun runBackup(onError: (String) -> BackupResult, block: suspend () -> BackupResult) {
    if (_busy.value) return
    _busy.value = true
    viewModelScope.launch {
      val result =
        try {
          block()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          onError(e.message ?: e.javaClass.simpleName)
        } finally {
          _busy.value = false
        }
      results.send(result)
    }
  }
}
