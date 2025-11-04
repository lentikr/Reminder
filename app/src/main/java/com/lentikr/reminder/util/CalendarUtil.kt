package com.lentikr.reminder.util

import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.RepeatUnit
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

    fun calculateNextTargetDate(reminderItem: ReminderItem): LocalDate? {
        val repeatInfo = reminderItem.repeatInfo
        val today = LocalDate.now()

        if (repeatInfo == null) {
            return if (reminderItem.date.isBefore(today)) null else reminderItem.date
        }

        var currentDate = reminderItem.date

        if (!reminderItem.isLunar) {
            // Gregorian calculation
            while (currentDate.isBefore(today)) {
                currentDate = when (repeatInfo.unit) {
                    RepeatUnit.DAY -> currentDate.plusDays(repeatInfo.interval.toLong())
                    RepeatUnit.WEEK -> currentDate.plusWeeks(repeatInfo.interval.toLong())
                    RepeatUnit.MONTH -> currentDate.plusMonths(repeatInfo.interval.toLong())
                    RepeatUnit.YEAR -> currentDate.plusYears(repeatInfo.interval.toLong())
                }
            }
            return currentDate
        } else {
            // Lunar calculation
            while (currentDate.isBefore(today)) {
                currentDate = when (repeatInfo.unit) {
                    RepeatUnit.YEAR -> getNextLunarYearDate(currentDate, repeatInfo.interval)
                    RepeatUnit.MONTH -> getNextLunarMonthDate(currentDate, repeatInfo.interval)
                    // Lunar day/week repeats are not standard, treat them as gregorian.
                    RepeatUnit.DAY -> currentDate.plusDays(repeatInfo.interval.toLong())
                    RepeatUnit.WEEK -> currentDate.plusWeeks(repeatInfo.interval.toLong())
                }
            }
            return currentDate
        }
    }

    private fun getNextLunarYearDate(currentSolarDate: LocalDate, interval: Int): LocalDate {
        val currentLunar = SolarDay.fromYmd(currentSolarDate.year, currentSolarDate.monthValue, currentSolarDate.dayOfMonth).getLunarDay()
        val targetYear = currentLunar.getYear() + interval
        var targetDay = currentLunar.getDay()
        var nextLunar: com.tyme.lunar.LunarDay? = null
        while (nextLunar == null && targetDay > 0) {
            try {
                nextLunar = com.tyme.lunar.LunarDay.fromYmd(targetYear, currentLunar.getMonth(), targetDay)
            } catch (e: IllegalArgumentException) {
                targetDay--
            }
        }
        if (nextLunar == null) {
            return currentSolarDate.plusYears(interval.toLong())
        }
        val nextSolar = nextLunar.getSolarDay()
        return LocalDate.of(nextSolar.getYear(), nextSolar.getMonth(), nextSolar.getDay())
    }

    private fun getNextLunarMonthDate(currentSolarDate: LocalDate, interval: Int): LocalDate {
        val currentLunarDay = SolarDay.fromYmd(currentSolarDate.year, currentSolarDate.monthValue, currentSolarDate.dayOfMonth).getLunarDay()
        val currentLunarMonth = currentLunarDay.getLunarMonth()
        val nextLunarMonth = currentLunarMonth.next(interval)
        var targetDay = currentLunarDay.getDay()
        var nextLunar: com.tyme.lunar.LunarDay? = null
        while (nextLunar == null && targetDay > 0) {
            try {
                nextLunar = com.tyme.lunar.LunarDay.fromYmd(nextLunarMonth.getYear(), nextLunarMonth.getMonth(), targetDay)
            } catch (e: IllegalArgumentException) {
                targetDay--
            }
        }
        if (nextLunar == null) {
            return currentSolarDate.plusMonths(interval.toLong())
        }
        val nextSolar = nextLunar.getSolarDay()
        return LocalDate.of(nextSolar.getYear(), nextSolar.getMonth(), nextSolar.getDay())
    }


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

        val thisYearSolar = try {
            com.tyme.lunar.LunarDay.fromYmd(
                today.year,
                originalLunar.getMonth(),
                originalLunar.getDay()
            ).getSolarDay()
        } catch (e: IllegalArgumentException) {
            // Handle cases like leap months that don't exist this year, or day 30 on a 29-day month.
            com.tyme.lunar.LunarDay.fromYmd(
                today.year,
                originalLunar.getMonth(),
                originalLunar.getDay() - 1
            ).getSolarDay()
        }

        val nextSolarDay = if (thisYearSolar.isBefore(todaySolar)) {
            try {
                com.tyme.lunar.LunarDay.fromYmd(
                    today.year + 1,
                    originalLunar.getMonth(),
                    originalLunar.getDay()
                ).getSolarDay()
            } catch (e: IllegalArgumentException) {
                com.tyme.lunar.LunarDay.fromYmd(
                    today.year + 1,
                    originalLunar.getMonth(),
                    originalLunar.getDay() - 1
                ).getSolarDay()
            }
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
