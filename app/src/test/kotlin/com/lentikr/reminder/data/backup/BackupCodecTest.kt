package com.lentikr.reminder.data.backup

import com.lentikr.reminder.domain.Reminder
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.domain.Repeat
import com.lentikr.reminder.domain.RepeatUnit
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

  private val reminders =
    listOf(
      Reminder(title = "春节", date = LocalDate.parse("2025-01-29"), type = ReminderType.COUNTDOWN, isLunar = true, category = "节日", isPinned = true, repeat = Repeat(1, RepeatUnit.YEAR)),
      Reminder(title = "在一起", date = LocalDate.parse("2015-11-15"), type = ReminderType.COUNT_UP),
    )

  @Test
  fun roundTripKeepsEveryField() {
    val text = BackupCodec.encode(reminders, exportedAt = "2025-10-30T10:00")
    assertTrue(text.contains("\"version\": ${BackupCodec.FORMAT_VERSION}"))
    // Ids are assigned by the database on restore.
    assertEquals(reminders.map { it.copy(id = 0) }, BackupCodec.decode(text))
  }

  @Test
  fun ignoresUnknownFields() {
    val text = """{"version":2,"future":1,"reminders":[{"title":"a","date":"2025-01-01","type":"COUNT_UP","color":"red"}]}"""
    assertEquals("a", BackupCodec.decode(text).single().title)
  }

  @Test
  fun dropsInvalidRepeatInsteadOfFailing() {
    val text = """{"version":2,"reminders":[{"title":"a","date":"2025-01-01","type":"COUNTDOWN","repeat":{"interval":0,"unit":"DAY"}}]}"""
    assertNull(BackupCodec.decode(text).single().repeat)
  }

  @Test
  fun rejectsNewerVersion() {
    val e = assertThrows(BackupFormatException::class.java) { BackupCodec.decode("""{"version":99,"reminders":[]}""") }
    assertTrue(e.message!!.contains("升级"))
  }

  @Test
  fun rejectsNonJson() {
    assertThrows(BackupFormatException::class.java) { BackupCodec.decode("not json") }
  }

  @Test
  fun rejectsWrongShape() {
    assertThrows(BackupFormatException::class.java) { BackupCodec.decode("42") }
    assertThrows(BackupFormatException::class.java) { BackupCodec.decode("[1,2,3]") }
    assertThrows(BackupFormatException::class.java) { BackupCodec.decode("""{"version":2}""") }
  }

  @Test
  fun rejectsUnknownType() {
    val text = """{"version":2,"reminders":[{"title":"a","date":"2025-01-01","type":"ANNUAL"}]}"""
    assertThrows(BackupFormatException::class.java) { BackupCodec.decode(text) }
  }

  @Test
  fun rejectsBadDate() {
    val text = """{"version":2,"reminders":[{"title":"a","date":"2025-13-45","type":"COUNT_UP"}]}"""
    assertThrows(BackupFormatException::class.java) { BackupCodec.decode(text) }
  }

  /** Exactly what Reminder 1.x wrote: Json.encodeToString(List<ReminderItem>), defaults included. */
  @Test
  fun importsVersionOneBackup() {
    val text =
      """[{"id":3,"title":"妈妈生日","date":"2025-09-10","type":"ANNUAL","isLunar":true,"category":"家人","isPinned":true,""" +
        """"repeatInfo":{"interval":1,"unit":"YEAR","endDate":null}},""" +
        """{"id":5,"title":"开发Reminder","date":"2025-10-30","type":"COUNT_UP","isLunar":false,"category":"","isPinned":false,"repeatInfo":null}]"""
    val restored = BackupCodec.decode(text)
    assertEquals(
      listOf(
        Reminder(title = "妈妈生日", date = LocalDate.parse("2025-09-10"), type = ReminderType.COUNTDOWN, isLunar = true, category = "家人", isPinned = true, repeat = Repeat(1, RepeatUnit.YEAR)),
        Reminder(title = "开发Reminder", date = LocalDate.parse("2025-10-30"), type = ReminderType.COUNT_UP),
      ),
      restored,
    )
  }

  @Test
  fun versionOneOneOffCountdownHasNoRepeat() {
    val text = """[{"id":1,"title":"a","date":"2026-01-01","type":"ANNUAL","isLunar":false,"category":"","isPinned":false}]"""
    val r = BackupCodec.decode(text).single()
    assertEquals(ReminderType.COUNTDOWN, r.type)
    assertNull(r.repeat)
  }

  @Test
  fun versionOneUnknownTypeFails() {
    val text = """[{"id":1,"title":"a","date":"2026-01-01","type":"WEEKLY","isLunar":false,"category":"","isPinned":false}]"""
    assertThrows(BackupFormatException::class.java) { BackupCodec.decode(text) }
  }
}
