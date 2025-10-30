package com.lentikr.reminder.data

import androidx.room.TypeConverter
import java.time.LocalDate

class TypeConverters {
    @TypeConverter
    fun fromString(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toReminderType(value: String) = enumValueOf<ReminderType>(value)

    @TypeConverter
    fun fromReminderType(value: ReminderType) = value.name
}
