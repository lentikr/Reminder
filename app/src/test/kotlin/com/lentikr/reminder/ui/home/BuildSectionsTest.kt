package com.lentikr.reminder.ui.home

import com.lentikr.reminder.domain.DayCounter
import com.lentikr.reminder.domain.Reminder
import com.lentikr.reminder.domain.ReminderType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildSectionsTest {

  private val today = LocalDate.parse("2025-10-30")
  private var nextId = 1L

  private fun item(title: String, inDays: Long, category: String = "", pinned: Boolean = false): ReminderItemUi {
    val r = Reminder(id = nextId++, title = title, date = today.plusDays(inDays), type = ReminderType.COUNTDOWN, category = category, isPinned = pinned)
    return ReminderItemUi(r, DayCounter.count(r, today))
  }

  @Test
  fun empty() {
    assertTrue(buildSections(emptyList()).isEmpty())
  }

  @Test
  fun singleUncategorizedGroupHasNoHeader() {
    val sections = buildSections(listOf(item("a", 3), item("b", 1)))
    assertEquals(1, sections.size)
    assertNull(sections[0].title)
    assertEquals(listOf("b", "a"), sections[0].items.map { it.reminder.title })
  }

  @Test
  fun pinnedFirstThenCategoriesThenUncategorized() {
    val sections =
      buildSections(
        listOf(item("x", 1), item("p", 9, category = "B", pinned = true), item("b1", 2, category = "B"), item("a1", 5, category = "A"))
      )
    assertEquals(
      listOf(SectionTitle.Pinned, SectionTitle.Category("A"), SectionTitle.Category("B"), SectionTitle.Uncategorized),
      sections.map { it.title },
    )
    // A pinned reminder appears only in the pinned section.
    assertEquals(listOf("b1"), sections[2].items.map { it.reminder.title })
  }

  @Test
  fun endedRemindersSinkToTheBottom() {
    val sections = buildSections(listOf(item("past", -3), item("soon", 2), item("today", 0)))
    assertEquals(listOf("today", "soon", "past"), sections.single().items.map { it.reminder.title })
  }
}
