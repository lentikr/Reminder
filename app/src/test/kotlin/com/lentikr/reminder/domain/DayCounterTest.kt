package com.lentikr.reminder.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayCounterTest {

  private fun countdown(date: String, repeat: Repeat? = null, lunar: Boolean = false) =
    Reminder(title = "t", date = LocalDate.parse(date), type = ReminderType.COUNTDOWN, isLunar = lunar, repeat = repeat)

  private fun countUp(date: String) = Reminder(title = "t", date = LocalDate.parse(date), type = ReminderType.COUNT_UP)

  private fun day(s: String) = LocalDate.parse(s)

  @Test
  fun countUp_startDayIsDayOne() {
    assertEquals(DayCount.Elapsed(1, day("2025-10-30")), DayCounter.count(countUp("2025-10-30"), day("2025-10-30")))
    assertEquals(DayCount.Elapsed(8, day("2025-10-30")), DayCounter.count(countUp("2025-10-30"), day("2025-11-06")))
  }

  @Test
  fun countUp_futureStartIsNotStarted() {
    assertEquals(DayCount.NotStarted(5, day("2025-11-04")), DayCounter.count(countUp("2025-11-04"), day("2025-10-30")))
  }

  @Test
  fun countdown_upcomingTodayAndEnded() {
    assertEquals(DayCount.Upcoming(3, day("2025-11-02")), DayCounter.count(countdown("2025-11-02"), day("2025-10-30")))
    assertEquals(DayCount.Today(day("2025-10-30")), DayCounter.count(countdown("2025-10-30"), day("2025-10-30")))
    assertEquals(DayCount.Ended(10, day("2025-10-20")), DayCounter.count(countdown("2025-10-20"), day("2025-10-30")))
  }

  @Test
  fun oneOffPastDate_hasNoNextOccurrence() {
    assertNull(DayCounter.nextOccurrence(countdown("2025-10-20"), day("2025-10-30")))
  }

  @Test
  fun yearlySolar_rollsIntoNextYear() {
    val birthday = countdown("1990-03-15", Repeat(1, RepeatUnit.YEAR))
    assertEquals(day("2026-03-15"), DayCounter.nextOccurrence(birthday, day("2025-10-30")))
    assertEquals(DayCount.Today(day("2026-03-15")), DayCounter.count(birthday, day("2026-03-15")))
  }

  @Test
  fun yearlySolar_leapDayFallsBackAndReturns() {
    val leapDay = countdown("2024-02-29", Repeat(1, RepeatUnit.YEAR))
    assertEquals(day("2025-02-28"), DayCounter.nextOccurrence(leapDay, day("2025-01-01")))
    // Anchored on the original date, so it doesn't stay on the 28th.
    assertEquals(day("2028-02-29"), DayCounter.nextOccurrence(leapDay, day("2028-01-01")))
  }

  @Test
  fun monthlySolar_monthEndDoesNotDrift() {
    val monthEnd = countdown("2025-01-31", Repeat(1, RepeatUnit.MONTH))
    assertEquals(day("2025-02-28"), DayCounter.nextOccurrence(monthEnd, day("2025-02-15")))
    assertEquals(day("2025-03-31"), DayCounter.nextOccurrence(monthEnd, day("2025-03-15")))
  }

  @Test
  fun everyTwoWeeks_landsOnTheCycle() {
    val biweekly = countdown("2025-01-01", Repeat(2, RepeatUnit.WEEK))
    assertEquals(day("2025-01-29"), DayCounter.nextOccurrence(biweekly, day("2025-01-20")))
    assertEquals(day("2025-01-29"), DayCounter.nextOccurrence(biweekly, day("2025-01-29")))
  }

  @Test
  fun everyThreeDays_countsFromStart() {
    val r = countdown("2025-01-01", Repeat(3, RepeatUnit.DAY))
    assertEquals(day("2025-01-10"), DayCounter.nextOccurrence(r, day("2025-01-09")))
  }

  @Test
  fun yearlyLunar_followsTheLunarDate() {
    // 2020-10-01 was 八月十五; Mid-Autumn 2025 is 2025-10-06.
    val midAutumn = countdown("2020-10-01", Repeat(1, RepeatUnit.YEAR), lunar = true)
    assertEquals(day("2025-10-06"), DayCounter.nextOccurrence(midAutumn, day("2025-10-01")))
    assertEquals(day("2025-10-06"), DayCounter.nextOccurrence(midAutumn, day("2025-10-06")))
  }

  @Test
  fun yearlyLunar_dayThirtyFallsBackInShortMonth() {
    // 2024-02-09 was 癸卯年腊月三十. 甲辰年腊月 has 29 days, so 2025's 除夕 is 腊月廿九 = 2025-01-28.
    val newYearsEve = countdown("2024-02-09", Repeat(1, RepeatUnit.YEAR), lunar = true)
    assertEquals(day("2025-01-28"), DayCounter.nextOccurrence(newYearsEve, day("2025-01-01")))
  }

  @Test
  fun lunarRepeat_dayAndWeekStepsStaySolar() {
    val r = countdown("2025-01-01", Repeat(10, RepeatUnit.DAY), lunar = true)
    assertEquals(day("2025-01-21"), DayCounter.nextOccurrence(r, day("2025-01-15")))
  }

  @Test
  fun sortKey_ordersTodayUpcomingRunningEnded() {
    val keys =
      listOf(
        DayCount.Ended(1, day("2025-01-01")),
        DayCount.Elapsed(5, day("2025-01-01")),
        DayCount.Upcoming(30, day("2025-01-01")),
        DayCount.Today(day("2025-01-01")),
        DayCount.Upcoming(2, day("2025-01-01")),
      )
    val sorted = keys.sortedBy(DayCounter::sortKey)
    assertEquals(DayCount.Today(day("2025-01-01")), sorted[0])
    assertEquals(DayCount.Upcoming(2, day("2025-01-01")), sorted[1])
    assertEquals(DayCount.Upcoming(30, day("2025-01-01")), sorted[2])
    assertTrue(sorted[3] is DayCount.Elapsed)
    assertTrue(sorted[4] is DayCount.Ended)
  }

  @Test
  fun normalized_trimsAndDropsRepeatForCountUp() {
    val r = Reminder(title = "  a ", date = day("2025-01-01"), type = ReminderType.COUNT_UP, category = " b ", repeat = Repeat(1, RepeatUnit.YEAR))
    val n = r.normalized()
    assertEquals("a", n.title)
    assertEquals("b", n.category)
    assertNull(n.repeat)
  }

  @Test(expected = IllegalArgumentException::class)
  fun repeat_rejectsZeroInterval() {
    Repeat(0, RepeatUnit.DAY)
  }
}
