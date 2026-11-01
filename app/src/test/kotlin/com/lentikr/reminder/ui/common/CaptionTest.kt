package com.lentikr.reminder.ui.common

import com.lentikr.reminder.domain.DayCount
import com.lentikr.reminder.domain.LunarCalendar
import com.lentikr.reminder.domain.LunarDate
import com.lentikr.reminder.domain.Reminder
import com.lentikr.reminder.domain.ReminderType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptionTest {

  private fun reminder(lunar: Boolean) =
    Reminder(title = "t", date = LocalDate.parse("2015-11-15"), type = ReminderType.COUNT_UP, isLunar = lunar)

  @Test
  fun solarIsIsoDate() {
    val count = DayCount.Upcoming(3, LocalDate.parse("2026-10-17"))
    assertEquals("2026-10-17", count.caption(reminder(lunar = false)))
  }

  @Test
  fun lunarUsesNumericYearAndLunarMonthDay() {
    val count = DayCount.Elapsed(4000, LocalDate.parse("2015-11-15"))
    assertEquals("2015年十月初四", count.caption(reminder(lunar = true)))
  }

  @Test
  fun lunarKeepsLeapMonth() {
    val date = LunarCalendar.toSolar(LunarDate(2025, 6, 1, isLeapMonth = true))
    assertEquals("2025年闰六月初一", DayCount.Upcoming(1, date).caption(reminder(lunar = true)))
  }

  @Test
  fun lunarYearIsTheLunarOneInEarlyJanuary() {
    val date = LunarCalendar.toSolar(LunarDate(2024, 12, 20))
    assertEquals(2025, date.year)
    assertEquals("2024年腊月二十", DayCount.Upcoming(1, date).caption(reminder(lunar = true)))
  }
}
