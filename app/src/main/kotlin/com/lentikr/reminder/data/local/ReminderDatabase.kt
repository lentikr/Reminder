package com.lentikr.reminder.data.local

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = true)
@ColumnTypeConverters(Converters::class)
abstract class ReminderDatabase : RoomDatabase() {
  abstract fun reminderDao(): ReminderDao

  companion object {
    const val NAME = "reminders.db"
  }
}
