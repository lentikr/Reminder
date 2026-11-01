package com.lentikr.reminder.data.local

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverter
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.lentikr.reminder.domain.Reminder
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.domain.Repeat
import com.lentikr.reminder.domain.RepeatUnit
import java.time.LocalDate

@Entity(tableName = "reminders")
data class ReminderEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  /** ISO-8601 text, so rows sort by date and stay readable in a database inspector. */
  val date: LocalDate,
  val type: ReminderType,
  @ColumnInfo(name = "is_lunar") val isLunar: Boolean,
  val category: String,
  @ColumnInfo(name = "is_pinned") val isPinned: Boolean,
  @ColumnInfo(name = "repeat_interval") val repeatInterval: Int?,
  @ColumnInfo(name = "repeat_unit") val repeatUnit: RepeatUnit?,
)

class Converters {
  @ColumnTypeConverter fun fromDate(date: LocalDate): String = date.toString()

  @ColumnTypeConverter fun toDate(value: String): LocalDate = LocalDate.parse(value)
}

fun ReminderEntity.toDomain(): Reminder =
  Reminder(
    id = id,
    title = title,
    date = date,
    type = type,
    isLunar = isLunar,
    category = category,
    isPinned = isPinned,
    repeat =
      if (repeatInterval != null && repeatUnit != null) {
        Repeat(repeatInterval.coerceIn(1, Repeat.MAX_INTERVAL), repeatUnit)
      } else {
        null
      },
  )

fun Reminder.toEntity(): ReminderEntity =
  normalized().let {
    ReminderEntity(
      id = it.id,
      title = it.title,
      date = it.date,
      type = it.type,
      isLunar = it.isLunar,
      category = it.category,
      isPinned = it.isPinned,
      repeatInterval = it.repeat?.interval,
      repeatUnit = it.repeat?.unit,
    )
  }
