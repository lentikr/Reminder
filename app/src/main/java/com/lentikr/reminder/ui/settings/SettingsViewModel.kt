package com.lentikr.reminder.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderRepository
import com.lentikr.reminder.data.ReminderType
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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
            backupFile.writeText(reminders.toJsonArray().toString())
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
            val json = JSONArray(latestFile.readText())
            val reminders = mutableListOf<ReminderItem>()
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                reminders.add(item.toReminderItem())
            }
            reminderRepository.deleteAllReminders()
            reminders.forEach { reminderRepository.insertReminder(it.copy(id = 0)) }
            "恢复完成，共导入 ${reminders.size} 条记录"
        } catch (e: Exception) {
            "恢复失败：${e.localizedMessage ?: "未知错误"}"
        }
    }
}

private fun List<ReminderItem>.toJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { reminder ->
        val obj = JSONObject().apply {
            put("id", reminder.id)
            put("title", reminder.title)
            put("date", reminder.date.toString())
            put("type", reminder.type.name)
            put("isLunar", reminder.isLunar)
            put("category", reminder.category)
            put("isPinned", reminder.isPinned)
        }
        array.put(obj)
    }
    return array
}

private fun JSONObject.toReminderItem(): ReminderItem {
    val date = LocalDate.parse(getString("date"))
    val type = enumValueOf<ReminderType>(getString("type"))
    return ReminderItem(
        id = optInt("id", 0),
        title = getString("title"),
        date = date,
        type = type,
        isLunar = optBoolean("isLunar", false),
        category = optString("category", ""),
        isPinned = optBoolean("isPinned", false)
    )
}

