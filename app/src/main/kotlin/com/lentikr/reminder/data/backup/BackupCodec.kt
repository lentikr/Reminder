package com.lentikr.reminder.data.backup

import com.lentikr.reminder.domain.Reminder
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.domain.Repeat
import com.lentikr.reminder.domain.RepeatUnit
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class BackupFormatException(message: String, cause: Throwable? = null) :
  Exception(message, cause)

/**
 * Encodes reminders as a versioned JSON document. The version lets future releases read older
 * backups.
 */
object BackupCodec {
  const val FORMAT_VERSION = 2

  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  fun encode(reminders: List<Reminder>, exportedAt: String): String =
    json.encodeToString(
      BackupFile(
        version = FORMAT_VERSION,
        exportedAt = exportedAt,
        reminders = reminders.map { it.toBackup() },
      )
    )

  fun decode(text: String): List<Reminder> {
    val root =
      try {
        json.parseToJsonElement(text)
      } catch (e: IllegalArgumentException) {
        throw BackupFormatException("不是有效的 JSON 文件", e)
      }
    return try {
      when (root) {
        is JsonObject -> decodeCurrent(root)
        // Reminder 1.x wrote a bare array of its database rows.
        is JsonArray -> decodeLegacy(root)
        else -> throw BackupFormatException("无法识别的备份格式")
      }
    } catch (e: BackupFormatException) {
      throw e
    } catch (e: IllegalArgumentException) {
      // Covers kotlinx.serialization errors and invalid Repeat values.
      throw BackupFormatException("备份内容已损坏", e)
    } catch (e: java.time.DateTimeException) {
      throw BackupFormatException("备份中的日期无效", e)
    }
  }

  private fun decodeCurrent(root: JsonElement): List<Reminder> {
    val file = json.decodeFromJsonElement<BackupFile>(root)
    if (file.version > FORMAT_VERSION) {
      throw BackupFormatException("备份来自更新版本的应用，请先升级")
    }
    return file.reminders.map { it.toDomain() }
  }

  private fun decodeLegacy(root: JsonArray): List<Reminder> =
    json.decodeFromJsonElement<List<LegacyReminder>>(root).map { it.toDomain() }
}

/** A row from a Reminder 1.x backup. 1.x called countdowns "ANNUAL" and never used endDate. */
@Serializable
private data class LegacyReminder(
  val title: String,
  val date: String,
  val type: String,
  val isLunar: Boolean = false,
  val category: String = "",
  val isPinned: Boolean = false,
  val repeatInfo: BackupRepeat? = null,
) {
  fun toDomain(): Reminder =
    Reminder(
      title = title,
      date = LocalDate.parse(date),
      type =
        when (type) {
          "ANNUAL" -> ReminderType.COUNTDOWN
          "COUNT_UP" -> ReminderType.COUNT_UP
          else -> throw BackupFormatException("无法识别的纪念日类型：$type")
        },
      isLunar = isLunar,
      category = category,
      isPinned = isPinned,
      repeat = repeatInfo?.toDomain(),
    ).normalized()
}

@Serializable
private data class BackupFile(
  val version: Int,
  val exportedAt: String = "",
  val reminders: List<BackupReminder>,
)

@Serializable
private data class BackupReminder(
  val title: String,
  val date: String,
  val type: ReminderType,
  val lunar: Boolean = false,
  val category: String = "",
  val pinned: Boolean = false,
  val repeat: BackupRepeat? = null,
)

@Serializable private data class BackupRepeat(val interval: Int, val unit: RepeatUnit)

private fun Reminder.toBackup() =
  BackupReminder(
    title = title,
    date = date.toString(),
    type = type,
    lunar = isLunar,
    category = category,
    pinned = isPinned,
    repeat = repeat?.let { BackupRepeat(it.interval, it.unit) },
  )

private fun BackupReminder.toDomain() =
  Reminder(
    title = title,
    date = LocalDate.parse(date),
    type = type,
    isLunar = lunar,
    category = category,
    isPinned = pinned,
    repeat = repeat?.toDomain(),
  ).normalized()

private fun BackupRepeat.toDomain(): Repeat? =
  interval.takeIf { it in 1..Repeat.MAX_INTERVAL }?.let { Repeat(it, unit) }
