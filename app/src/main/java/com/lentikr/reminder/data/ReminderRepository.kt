package com.lentikr.reminder.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository that provides insert, update, delete, and retrieve of [ReminderItem] from a given data source.
 */
class ReminderRepository(private val reminderDao: ReminderDao) {

    fun getAllRemindersStream(): Flow<List<ReminderItem>> = reminderDao.getAllReminders()

    suspend fun insertReminder(item: ReminderItem) {
        reminderDao.insert(item)
    }

    suspend fun deleteReminder(item: ReminderItem) {
        reminderDao.delete(item)
    }

    suspend fun updateReminder(item: ReminderItem) {
        reminderDao.update(item)
    }
}
