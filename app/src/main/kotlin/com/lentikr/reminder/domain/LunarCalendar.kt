package com.lentikr.reminder.domain

import com.tyme.lunar.LunarDay
import com.tyme.lunar.LunarMonth
import com.tyme.lunar.LunarYear
import com.tyme.solar.SolarDay
import java.time.LocalDate

/** A date in the Chinese lunisolar calendar. [month] is 1..12 and [isLeapMonth] marks 闰月. */
data class LunarDate(val year: Int, val month: Int, val day: Int, val isLeapMonth: Boolean = false) {
  /** tyme encodes leap months as negative month numbers. */
  internal val monthWithLeap: Int
    get() = if (isLeapMonth) -month else month

  /** For example "闰四月初一". */
  val monthDayName: String
    get() = LunarCalendar.monthName(month, isLeapMonth) + LunarCalendar.dayName(day)

  /** For example "乙巳年九月初十". */
  val fullName: String
    get() = LunarCalendar.yearName(year) + monthDayName

  /** For example "2025年九月初十": the lunar year as a number, short enough for a card footer. */
  val shortName: String
    get() = "${year}年$monthDayName"
}

/** Thin wrapper over tyme4kt, so the rest of the app never touches its API directly. */
object LunarCalendar {
  private val MONTH_NAMES =
    arrayOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")

  fun fromSolar(date: LocalDate): LunarDate {
    val lunarDay = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth).getLunarDay()
    val month = lunarDay.getLunarMonth()
    return LunarDate(month.year, month.month, lunarDay.day, month.isLeap())
  }

  fun toSolar(date: LunarDate): LocalDate {
    val solar = LunarDay.fromYmd(date.year, date.monthWithLeap, date.day).getSolarDay()
    return LocalDate.of(solar.year, solar.month, solar.day)
  }

  /** The leap month of [year], or 0 if the year has none. */
  fun leapMonthOf(year: Int): Int = LunarYear.fromYear(year).getLeapMonth()

  /** Months of [year] in order, with the leap month (if any) right after its regular month. */
  fun monthsOf(year: Int): List<Pair<Int, Boolean>> {
    val leap = leapMonthOf(year)
    return (1..12).flatMap { m -> if (m == leap) listOf(m to false, m to true) else listOf(m to false) }
  }

  fun daysInMonth(year: Int, month: Int, isLeapMonth: Boolean): Int =
    LunarMonth.fromYm(year, if (isLeapMonth) -month else month).getDayCount()

  /**
   * Same month and day [years] later. A leap month falls back to the regular month when the target
   * year has no such leap month, and day 30 falls back to 29 in short months.
   */
  fun plusYears(date: LunarDate, years: Int): LunarDate {
    val year = date.year + years
    val isLeap = date.isLeapMonth && leapMonthOf(year) == date.month
    return date.copy(year = year, isLeapMonth = isLeap).clampDay()
  }

  /** [months] lunar months later, counting leap months as months of their own. */
  fun plusMonths(date: LunarDate, months: Int): LunarDate {
    val target = LunarMonth.fromYm(date.year, date.monthWithLeap).next(months)
    return LunarDate(target.year, target.month, date.day, target.isLeap()).clampDay()
  }

  fun monthName(month: Int, isLeapMonth: Boolean): String =
    (if (isLeapMonth) "闰" else "") + MONTH_NAMES[month - 1]

  fun dayName(day: Int): String = LunarDay.NAMES[day - 1]

  /** Sexagenary year name, for example "乙巳年". */
  fun yearName(year: Int): String = LunarYear.fromYear(year).getSixtyCycle().getName() + "年"

  /** Chinese zodiac animal of the lunar year, for example "蛇". */
  fun zodiacOf(year: Int): String =
    LunarYear.fromYear(year).getSixtyCycle().getEarthBranch().getZodiac().getName()

  private fun LunarDate.clampDay(): LunarDate =
    copy(day = day.coerceAtMost(daysInMonth(year, month, isLeapMonth)))
}
