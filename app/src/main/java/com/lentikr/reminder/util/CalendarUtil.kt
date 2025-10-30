package com.lentikr.reminder.util

import com.tyme.solar.SolarDay
import java.time.LocalDate

object CalendarUtil {

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
}
