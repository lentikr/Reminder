package com.lentikr.reminder.domain

import java.time.LocalDate

enum class ReminderType {
  /** Counts down to [Reminder.date] or to its next repeat. */
  COUNTDOWN,

  /** Counts the days since [Reminder.date], which is day 1. */
  COUNT_UP,
}

enum class RepeatUnit {
  DAY,
  WEEK,
  MONTH,
  YEAR;

  /** Lunar dates only repeat by lunar month or year; day and week steps are calendar-neutral. */
  val isLunarAware: Boolean
    get() = this == MONTH || this == YEAR
}

data class Repeat(val interval: Int, val unit: RepeatUnit) {
  init {
    require(interval in 1..MAX_INTERVAL) { "Repeat interval out of range: $interval" }
  }

  companion object {
    const val MAX_INTERVAL = 999
  }
}

data class Reminder(
  val id: Long = 0,
  val title: String,
  /** The Gregorian date of the event. For lunar reminders the lunar date is derived from it. */
  val date: LocalDate,
  val type: ReminderType,
  val isLunar: Boolean = false,
  val category: String = "",
  val isPinned: Boolean = false,
  /** Only used by [ReminderType.COUNTDOWN]. */
  val repeat: Repeat? = null,
) {
  /** Drops values that the reminder's type doesn't use, so storage stays canonical. */
  fun normalized(): Reminder =
    copy(
      title = title.trim(),
      category = category.trim(),
      repeat = repeat.takeIf { type == ReminderType.COUNTDOWN },
    )
}
