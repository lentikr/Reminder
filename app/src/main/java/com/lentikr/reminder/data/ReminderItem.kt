package com.lentikr.reminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class ReminderType {
    ANNUAL, // For recurring events like birthdays
    COUNT_UP // For counting days since an event
}

@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val date: LocalDate,
    val type: ReminderType,
    val isLunar: Boolean,
    val category: String,
    val isPinned: Boolean
)
