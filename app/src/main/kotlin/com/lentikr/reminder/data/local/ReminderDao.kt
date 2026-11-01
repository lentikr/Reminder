package com.lentikr.reminder.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
  @Query("SELECT * FROM reminders") fun observeAll(): Flow<List<ReminderEntity>>

  @Query("SELECT * FROM reminders WHERE id = :id") fun observe(id: Long): Flow<ReminderEntity?>

  @Query("SELECT * FROM reminders WHERE id = :id") suspend fun get(id: Long): ReminderEntity?

  @Query("SELECT * FROM reminders ORDER BY id") suspend fun getAll(): List<ReminderEntity>

  @Query("SELECT * FROM reminders WHERE id IN (:ids)")
  suspend fun getByIds(ids: Collection<Long>): List<ReminderEntity>

  @Query("SELECT DISTINCT category FROM reminders WHERE category != ''")
  fun observeCategories(): Flow<List<String>>

  @Insert suspend fun insert(reminder: ReminderEntity): Long

  @Insert suspend fun insertAll(reminders: List<ReminderEntity>)

  @Update suspend fun update(reminder: ReminderEntity)

  @Query("UPDATE reminders SET is_pinned = :pinned WHERE id IN (:ids)")
  suspend fun setPinned(ids: Collection<Long>, pinned: Boolean)

  @Query("DELETE FROM reminders WHERE id IN (:ids)") suspend fun deleteByIds(ids: Collection<Long>)

  @Query("DELETE FROM reminders") suspend fun deleteAll()

  @Transaction
  suspend fun replaceAll(reminders: List<ReminderEntity>) {
    deleteAll()
    insertAll(reminders)
  }

  /** Deletes the rows and returns them, so the caller can offer undo. */
  @Transaction
  suspend fun deleteAndReturn(ids: Collection<Long>): List<ReminderEntity> {
    val rows = getByIds(ids)
    deleteByIds(ids)
    return rows
  }
}
