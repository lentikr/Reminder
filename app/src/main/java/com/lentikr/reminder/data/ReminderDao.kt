package com.lentikr.reminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(item: ReminderItem)

    @Update
    suspend fun update(item: ReminderItem)

    @Delete
    suspend fun delete(item: ReminderItem)

    @Query("SELECT * FROM reminders ORDER BY isPinned DESC, date DESC")
    fun getAllReminders(): Flow<List<ReminderItem>>
}
