package com.lentikr.reminder.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LunarCalendarTest {

  @Test
  fun springFestivals() {
    for (date in listOf("2024-02-10", "2025-01-29", "2026-02-17")) {
      val lunar = LunarCalendar.fromSolar(LocalDate.parse(date))
      assertEquals(date, 1 to 1, lunar.month to lunar.day)
      assertFalse(lunar.isLeapMonth)
    }
  }

  @Test
  fun midAutumn2025() {
    assertEquals(LunarDate(2025, 8, 15), LunarCalendar.fromSolar(LocalDate.parse("2025-10-06")))
    assertEquals(LocalDate.parse("2025-10-06"), LunarCalendar.toSolar(LunarDate(2025, 8, 15)))
  }

  @Test
  fun roundTripsEveryDayOfAYear() {
    var d = LocalDate.parse("2025-01-01")
    while (d.year == 2025) {
      assertEquals(d, LunarCalendar.toSolar(LunarCalendar.fromSolar(d)))
      d = d.plusDays(1)
    }
  }

  @Test
  fun leapMonths() {
    assertEquals(6, LunarCalendar.leapMonthOf(2025))
    assertEquals(2, LunarCalendar.leapMonthOf(2023))
    assertEquals(0, LunarCalendar.leapMonthOf(2026))
    val months = LunarCalendar.monthsOf(2025)
    assertEquals(13, months.size)
    assertEquals(listOf(6 to false, 6 to true, 7 to false), months.subList(5, 8))
    assertEquals(12, LunarCalendar.monthsOf(2026).size)
  }

  @Test
  fun shortTwelfthMonthOf2024() {
    assertEquals(29, LunarCalendar.daysInMonth(2024, 12, isLeapMonth = false))
  }

  @Test
  fun plusYears_leapMonthFallsBackToRegularMonth() {
    val next = LunarCalendar.plusYears(LunarDate(2025, 6, 1, isLeapMonth = true), 1)
    assertEquals(LunarDate(2026, 6, 1, isLeapMonth = false), next)
  }

  @Test
  fun plusYears_dayThirtyClampsInShortMonth() {
    assertEquals(LunarDate(2024, 12, 29), LunarCalendar.plusYears(LunarDate(2023, 12, 30), 1))
  }

  @Test
  fun plusMonths_countsLeapMonthAsItsOwnMonth() {
    assertEquals(LunarDate(2025, 6, 1, isLeapMonth = true), LunarCalendar.plusMonths(LunarDate(2025, 6, 1), 1))
    assertEquals(LunarDate(2025, 7, 1), LunarCalendar.plusMonths(LunarDate(2025, 6, 1), 2))
  }

  @Test
  fun names() {
    assertEquals("闰六月初一", LunarDate(2025, 6, 1, isLeapMonth = true).monthDayName)
    assertEquals("乙巳年八月十五", LunarDate(2025, 8, 15).fullName)
    assertEquals("腊月三十", LunarDate(2023, 12, 30).monthDayName)
    assertEquals("蛇", LunarCalendar.zodiacOf(2025))
  }
}
