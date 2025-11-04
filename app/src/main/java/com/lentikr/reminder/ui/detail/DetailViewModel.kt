package com.lentikr.reminder.ui.detail

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val reminderId: Int = checkNotNull(savedStateHandle["reminderId"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _saveResult = MutableSharedFlow<SaveResult>()
    val saveResult = _saveResult.asSharedFlow()

    init {
        viewModelScope.launch {
            reminderRepository.getReminderStream(reminderId)
                .filterNotNull()
                .first()
                .let {
                    _uiState.value = DetailUiState(reminderItem = it)
                }
        }
    }

    fun shareReminder(bitmap: Bitmap, context: Context) {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "reminder_share.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享提醒")
        context.startActivity(chooser)
    }

    fun saveReminderAsImage(bitmap: Bitmap, context: Context) {
        viewModelScope.launch {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "Reminder_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Reminders")
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    _saveResult.emit(SaveResult.Success)
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    _saveResult.emit(SaveResult.Failure)
                }
            } else {
                _saveResult.emit(SaveResult.Failure)
            }
        }
    }
}

data class DetailUiState(
    val reminderItem: ReminderItem? = null
)

sealed class SaveResult {
    object Success : SaveResult()
    object Failure : SaveResult()
}