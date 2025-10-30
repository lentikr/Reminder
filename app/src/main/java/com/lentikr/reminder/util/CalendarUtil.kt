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
        // 1. Convert the original solar date back to its lunar representation to get the correct lunar month and day.
        val originalTymeSolar = SolarDay.fromYmd(originalSolarDate.year, originalSolarDate.monthValue, originalSolarDate.dayOfMonth)
        val originalTymeLunar = originalTymeSolar.getLunarDay()

        val targetLunarMonth = originalTymeLunar.getMonth()
        val targetLunarDay = originalTymeLunar.getDay()

        // 2. Use the correct lunar month/day to find the next occurrence.
        val today = LocalDate.now()
        val todaySolar = SolarDay.fromYmd(today.year, today.monthValue, today.dayOfMonth)

        // Find this year's solar date for that lunar month/day.
        val targetLunarThisYear = com.tyme.lunar.LunarDay.fromYmd(today.year, targetLunarMonth, targetLunarDay)
        var nextSolarDay = targetLunarThisYear.getSolarDay()

        // If it has passed this year, find next year's.
        if (nextSolarDay.isBefore(todaySolar)) {
            val targetLunarNextYear = com.tyme.lunar.LunarDay.fromYmd(today.year + 1, targetLunarMonth, targetLunarDay)
            nextSolarDay = targetLunarNextYear.getSolarDay()
        }

        return LocalDate.of(nextSolarDay.getYear(), nextSolarDay.getMonth(), nextSolarDay.getDay())
    }
}
