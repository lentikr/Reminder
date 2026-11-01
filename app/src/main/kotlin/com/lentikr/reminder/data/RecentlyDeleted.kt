package com.lentikr.reminder.data

import com.lentikr.reminder.domain.Reminder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Hands the most recent deletion to the home screen so it can offer undo, even when the deletion
 * happened on another screen. A channel keeps the event until home is visible to consume it.
 */
@Singleton
class RecentlyDeleted @Inject constructor() {
  private val channel = Channel<List<Reminder>>(Channel.CONFLATED)

  val events: Flow<List<Reminder>> = channel.receiveAsFlow()

  fun post(reminders: List<Reminder>) {
    if (reminders.isNotEmpty()) channel.trySend(reminders)
  }
}
