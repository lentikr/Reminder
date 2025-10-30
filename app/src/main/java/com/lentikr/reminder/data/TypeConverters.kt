package com.lentikr.reminder.data

import androidx.room.TypeConverter
import java.time.LocalDate

class TypeConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun toReminderType(value: String) = enumValueOf<ReminderType>(value)

    @TypeConverter
    fun fromReminderType(value: ReminderType) = value.name
}
