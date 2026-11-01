package com.lentikr.reminder.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.lentikr.reminder.R
import com.lentikr.reminder.domain.DayCount
import com.lentikr.reminder.domain.LunarCalendar
import com.lentikr.reminder.domain.Reminder
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.domain.Repeat
import com.lentikr.reminder.domain.RepeatUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

/** "2026年10月30日 周五". */
fun LocalDate.formatLong(): String =
  format(DateFormat) + " " + dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)

/** "2026年10月30日". */
fun LocalDate.formatShort(): String = format(DateFormat)

/** "乙巳年九月初十". */
fun LocalDate.lunarFullName(): String = LunarCalendar.fromSolar(this).fullName

/** "九月初十". */
fun LocalDate.lunarMonthDay(): String = LunarCalendar.fromSolar(this).monthDayName

/** A reminder's date in the calendar it was entered in. */
fun Reminder.displayDate(date: LocalDate = this.date): String =
  if (isLunar) date.lunarFullName() else date.formatLong()

@Composable
@ReadOnlyComposable
fun Repeat?.label(lunar: Boolean): String {
  if (this == null) return stringResource(R.string.repeat_none)
  val base =
    if (interval == 1) {
      stringResource(
        when (unit) {
          RepeatUnit.DAY -> R.string.repeat_every_day
          RepeatUnit.WEEK -> R.string.repeat_every_week
          RepeatUnit.MONTH -> R.string.repeat_every_month
          RepeatUnit.YEAR -> R.string.repeat_every_year
        }
      )
    } else {
      stringResource(
        when (unit) {
          RepeatUnit.DAY -> R.string.repeat_every_n_days
          RepeatUnit.WEEK -> R.string.repeat_every_n_weeks
          RepeatUnit.MONTH -> R.string.repeat_every_n_months
          RepeatUnit.YEAR -> R.string.repeat_every_n_years
        },
        interval,
      )
    }
  return if (lunar && unit.isLunarAware) base + stringResource(R.string.repeat_lunar_suffix) else base
}

@get:StringRes
val ReminderType.labelRes: Int
  get() =
    when (this) {
      ReminderType.COUNTDOWN -> R.string.type_countdown
      ReminderType.COUNT_UP -> R.string.type_count_up
    }

/** The words around the day number: "还有 | 12 | 天", "第 | 8 | 天", "| 今天 |". */
data class CountText(val prefix: String, val number: String, val suffix: String)

@Composable
@ReadOnlyComposable
fun DayCount.text(): CountText {
  val day = stringResource(R.string.count_unit_day)
  return when (this) {
    is DayCount.Upcoming -> CountText(stringResource(R.string.count_prefix_remaining), "$days", day)
    is DayCount.Today -> CountText("", stringResource(R.string.count_today), "")
    is DayCount.Ended -> CountText(stringResource(R.string.count_prefix_ended), "$days", day)
    is DayCount.Elapsed -> CountText(stringResource(R.string.count_prefix_elapsed), "$days", day)
    is DayCount.NotStarted -> CountText(stringResource(R.string.count_prefix_remaining), "$days", day)
  }
}

/**
 * The date behind a count for cards and list rows, in the calendar the reminder was entered in:
 * "2026-10-17" or "2015年十月初四". The words around the number already say whether the date is
 * ahead, past or the start, so the caption is only the date.
 */
fun DayCount.caption(reminder: Reminder): String =
  if (reminder.isLunar) LunarCalendar.fromSolar(date).shortName else date.format(DateTimeFormatter.ISO_LOCAL_DATE)
