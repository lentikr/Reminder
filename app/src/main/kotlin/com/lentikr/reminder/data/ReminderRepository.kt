package com.lentikr.reminder.data

import com.lentikr.reminder.data.local.ReminderDao
import com.lentikr.reminder.data.local.toDomain
import com.lentikr.reminder.data.local.toEntity
import com.lentikr.reminder.domain.Reminder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ReminderRepository @Inject constructor(private val dao: ReminderDao) {

  fun observeReminders(): Flow<List<Reminder>> =
    dao.observeAll().map { rows -> rows.map { it.toDomain() } }

  fun observeReminder(id: Long): Flow<Reminder?> = dao.observe(id).map { it?.toDomain() }

  /** Distinct categories, sorted, for suggestions in the editor. */
  fun observeCategories(): Flow<List<String>> =
    dao.observeCategories().map { categories -> categories.map(String::trim).distinct().sorted() }

  suspend fun get(id: Long): Reminder? = dao.get(id)?.toDomain()

  suspend fun getAll(): List<Reminder> = dao.getAll().map { it.toDomain() }

  /** Inserts a new reminder (id 0) or updates an existing one, and returns its id. */
  suspend fun save(reminder: Reminder): Long {
    val entity = reminder.toEntity()
    return if (entity.id == 0L) {
      dao.insert(entity)
    } else {
      dao.update(entity)
      entity.id
    }
  }

  suspend fun setPinned(ids: Collection<Long>, pinned: Boolean) = dao.setPinned(ids, pinned)

  /** Deletes the reminders and returns them, so [restoreDeleted] can undo the deletion. */
  suspend fun delete(ids: Collection<Long>): List<Reminder> =
    dao.deleteAndReturn(ids).map { it.toDomain() }

  /** Re-inserts deleted reminders with their original ids. */
  suspend fun restoreDeleted(reminders: List<Reminder>) =
    dao.insertAll(reminders.map { it.toEntity() })

  /** Replaces every reminder, as a backup restore does. Ids are reassigned. */
  suspend fun replaceAll(reminders: List<Reminder>) =
    dao.replaceAll(reminders.map { it.copy(id = 0).toEntity() })
}
