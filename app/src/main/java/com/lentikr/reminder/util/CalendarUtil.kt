package com.lentikr.reminder.util

import com.tyme.solar.SolarDay
import java.time.LocalDate
import kotlin.math.abs

object CalendarUtil {

    private val LUNAR_DAY_STRINGS = arrayOf(
        "初一", "初二", "初三", "初四", "初五",
        "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五",
        "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五",
        "廿六", "廿七", "廿八", "廿九", "三十"
    )

    private val LUNAR_MONTH_STRINGS = arrayOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    /**
     * Calculates the next occurrence of a given lunar date.
     * @param originalSolarDate The original solar date stored in the database,
     * which corresponds to the initial lunar event (e.g., 2001-04-12 for Lunar 2001-03-20).
     * @return The upcoming solar date for that lunar anniversary.
     */
    fun getNextLunarDate(originalSolarDate: LocalDate): LocalDate {
        val originalLunar = SolarDay.fromYmd(
            originalSolarDate.year,
            originalSolarDate.monthValue,
            originalSolarDate.dayOfMonth
        ).getLunarDay()

        val today = LocalDate.now()
        val todaySolar = SolarDay.fromYmd(today.year, today.monthValue, today.dayOfMonth)

        val thisYearSolar = com.tyme.lunar.LunarDay.fromYmd(
            today.year,
            originalLunar.getMonth(),
            originalLunar.getDay()
        ).getSolarDay()

        val nextSolarDay = if (thisYearSolar.isBefore(todaySolar)) {
            com.tyme.lunar.LunarDay.fromYmd(
                today.year + 1,
                originalLunar.getMonth(),
                originalLunar.getDay()
            ).getSolarDay()
        } else {
            thisYearSolar
        }

        return LocalDate.of(nextSolarDay.getYear(), nextSolarDay.getMonth(), nextSolarDay.getDay())
    }

    fun getLunarMonthDayLabel(date: LocalDate): String {
        val solar = SolarDay.fromYmd(
            date.year,
            date.monthValue,
            date.dayOfMonth
        )
        val lunar = solar.getLunarDay()
        val monthValue = lunar.getMonth()
        val dayValue = lunar.getDay()
        val isLeapMonth = monthValue < 0
        val monthIndex = abs(monthValue) - 1
        val dayIndex = dayValue - 1
        val monthLabel = buildString {
            if (isLeapMonth) append("闰")
            append(LUNAR_MONTH_STRINGS.getOrNull(monthIndex) ?: "${abs(monthValue)}月")
        }
        val dayLabel = LUNAR_DAY_STRINGS.getOrNull(dayIndex) ?: dayValue.toString()
        return "$monthLabel$dayLabel"
    }
}
