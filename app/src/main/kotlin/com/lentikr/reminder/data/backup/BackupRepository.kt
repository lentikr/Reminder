package com.lentikr.reminder.data.backup

import android.content.Context
import android.net.Uri
import com.lentikr.reminder.data.ReminderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class BackupRepository
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val reminders: ReminderRepository,
  private val clock: Clock,
) {

  fun suggestedFileName(): String =
    "reminder-backup-${LocalDateTime.now(clock).format(FILE_STAMP)}.json"

  /** Writes all reminders to [uri] and returns how many were written. */
  suspend fun export(uri: Uri): Int {
    val all = reminders.getAll()
    if (all.isEmpty()) return 0
    val text = BackupCodec.encode(all, exportedAt = LocalDateTime.now(clock).toString())
    withContext(Dispatchers.IO) {
      val stream =
        context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("无法写入文件")
      stream.use { it.write(text.toByteArray()) }
    }
    return all.size
  }

  /** Replaces all reminders with the backup at [uri] and returns how many were restored. */
  suspend fun import(uri: Uri): Int {
    val text =
      withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("无法读取文件")
        stream.use { it.readAtMost(MAX_BACKUP_BYTES) }.decodeToString()
      }
    val restored = withContext(Dispatchers.Default) { BackupCodec.decode(text) }
    reminders.replaceAll(restored)
    return restored.size
  }

  private companion object {
    val FILE_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    const val MAX_BACKUP_BYTES = 8 * 1024 * 1024
  }
}

/**
 * Reads the whole stream, failing once it passes [limit] bytes so a wrong file can't exhaust
 * memory. InputStream.readNBytes would do this but needs API 33.
 */
internal fun InputStream.readAtMost(limit: Int): ByteArray {
  val out = ByteArrayOutputStream()
  val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
  while (true) {
    val read = read(buffer)
    if (read < 0) return out.toByteArray()
    if (out.size() + read > limit) throw BackupFormatException("文件过大，不是有效的备份")
    out.write(buffer, 0, read)
  }
}
