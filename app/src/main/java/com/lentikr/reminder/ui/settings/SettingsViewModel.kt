@file:OptIn(ExperimentalSerializationApi::class)

package com.lentikr.reminder.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderRepository
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

class SettingsViewModel(private val reminderRepository: ReminderRepository) : ViewModel() {

    suspend fun backupToLocal(context: Context): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val reminders = reminderRepository.getAllRemindersStream().first()
            if (reminders.isEmpty()) {
                return@withContext "没有可备份的数据"
            }
            val backupDir = context.getExternalFilesDir(null) ?: context.filesDir
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val timestamp = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.getDefault()))
            val backupFile = File(backupDir, "reminder-backup-$timestamp.json")
            backupFile.writeText(Json.encodeToString(reminders))
            "备份完成：${backupFile.absolutePath}"
        } catch (e: Exception) {
            "备份失败：${e.localizedMessage ?: "未知错误"}"
        }
    }

    suspend fun restoreFromLocal(context: Context): String = withContext(Dispatchers.IO) {
        val backupDir = context.getExternalFilesDir(null) ?: context.filesDir
        val latestFile = backupDir
            .listFiles { file -> file.name.startsWith("reminder-backup") && file.extension == "json" }
            ?.maxByOrNull { it.lastModified() }

        if (latestFile == null || !latestFile.exists()) {
            return@withContext "未找到备份文件"
        }

        return@withContext try {
            val reminders = Json.decodeFromString<List<ReminderItem>>(latestFile.readText())
            reminderRepository.deleteAllReminders()
            reminders.forEach { reminderRepository.insertReminder(it.copy(id = 0)) }
            "恢复完成，共导入 ${reminders.size} 条记录"
        } catch (e: Exception) {
            "恢复失败：${e.localizedMessage ?: "未知错误"}"
        }
    }
}

