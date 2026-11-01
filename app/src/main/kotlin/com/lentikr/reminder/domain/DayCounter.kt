package com.lentikr.reminder.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** What a reminder's day number means on a given day. */
sealed interface DayCount {
  /** The date the number refers to: the next occurrence for countdowns, the start for count-ups. */
  val date: LocalDate

  /** [days] days left until [date]. */
  data class Upcoming(val days: Long, override val date: LocalDate) : DayCount

  /** [date] is today. */
  data class Today(override val date: LocalDate) : DayCount

  /** A one-off countdown that ended [days] days ago. */
  data class Ended(val days: Long, override val date: LocalDate) : DayCount

  /** Today is day [days] since [date] (the start date is day 1). */
  data class Elapsed(val days: Long, override val date: LocalDate) : DayCount

  /** A count-up whose start [date] is still [days] days away. */
  data class NotStarted(val days: Long, override val date: LocalDate) : DayCount
}

object DayCounter {

  fun count(reminder: Reminder, today: LocalDate): DayCount =
    when (reminder.type) {
      ReminderType.COUNT_UP -> {
        val since = ChronoUnit.DAYS.between(reminder.date, today)
        if (since >= 0) DayCount.Elapsed(since + 1, reminder.date)
        else DayCount.NotStarted(-since, reminder.date)
      }
      ReminderType.COUNTDOWN -> {
        val next = nextOccurrence(reminder, today)
        if (next == null) {
          DayCount.Ended(ChronoUnit.DAYS.between(reminder.date, today), reminder.date)
        } else {
          val days = ChronoUnit.DAYS.between(today, next)
          if (days == 0L) DayCount.Today(next) else DayCount.Upcoming(days, next)
        }
      }
    }

  /**
   * The first occurrence on or after [today], or null for a one-off date in the past. Each
   * occurrence is computed from the original date rather than from the previous occurrence, so
   * month-end and lunar day-30 dates don't drift after a short month.
   */
  fun nextOccurrence(reminder: Reminder, today: LocalDate): LocalDate? {
    val start = reminder.date
    if (!start.isBefore(today)) return start
    val repeat = reminder.repeat ?: return null
    return if (reminder.isLunar && repeat.unit.isLunarAware) {
      nextLunarOccurrence(LunarCalendar.fromSolar(start), repeat, today)
    } else {
      nextSolarOccurrence(start, repeat, today)
    }
  }

  private fun nextSolarOccurrence(start: LocalDate, repeat: Repeat, today: LocalDate): LocalDate {
    val step = repeat.interval.toLong()
    val stepDays =
      when (repeat.unit) {
        RepeatUnit.DAY -> step
        RepeatUnit.WEEK -> step * 7
        else -> 0
      }
    if (stepDays > 0) {
      val gap = ChronoUnit.DAYS.between(start, today)
      val steps = (gap + stepDays - 1) / stepDays
      return start.plusDays(steps * stepDays)
    }
    val unit = if (repeat.unit == RepeatUnit.MONTH) ChronoUnit.MONTHS else ChronoUnit.YEARS
    // Jump close to today, then step forward; one or two iterations at most.
    var k = (unit.between(start, today) / step).coerceAtLeast(0)
    while (true) {
      val candidate = start.plus(k * step, unit)
      if (!candidate.isBefore(today)) return candidate
      k++
    }
  }

  private fun nextLunarOccurrence(start: LunarDate, repeat: Repeat, today: LocalDate): LocalDate {
    val step = repeat.interval
    val lunarToday = LunarCalendar.fromSolar(today)
    val elapsed =
      when (repeat.unit) {
        RepeatUnit.YEAR -> lunarToday.year - start.year
        // A lunar month is never longer than 30 days, so this never overshoots.
        else -> (ChronoUnit.DAYS.between(LunarCalendar.toSolar(start), today) / 30).toInt()
      }
    var k = (elapsed / step - 1).coerceAtLeast(0)
    while (true) {
      val lunar =
        if (repeat.unit == RepeatUnit.YEAR) LunarCalendar.plusYears(start, k * step)
        else LunarCalendar.plusMonths(start, k * step)
      val candidate = LunarCalendar.toSolar(lunar)
      if (!candidate.isBefore(today)) return candidate
      k++
    }
  }

  /**
   * Sort key within one list: today and upcoming dates first (nearest first), then running
   * count-ups (most recent start first), then ended one-off countdowns.
   */
  fun sortKey(count: DayCount): Long =
    when (count) {
      is DayCount.Today -> 0
      is DayCount.Upcoming -> count.days
      is DayCount.NotStarted -> count.days
      is DayCount.Elapsed -> SORT_GROUP + count.days
      is DayCount.Ended -> 2 * SORT_GROUP + count.days
    }

  private const val SORT_GROUP = 1_000_000_000L
}
