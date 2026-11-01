package com.lentikr.reminder.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.lentikr.reminder.domain.ReminderType
import kotlinx.serialization.Serializable

/** The reminder list; the list pane on wide screens. */
@Serializable data object Home : NavKey

/** One reminder; the detail pane on wide screens. */
@Serializable data class Detail(val id: Long) : NavKey

/** Creates a reminder when [id] is null, otherwise edits it. */
@Serializable
data class Edit(val id: Long? = null, val type: ReminderType = ReminderType.COUNTDOWN) : NavKey

@Serializable data object Settings : NavKey
