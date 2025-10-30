package com.lentikr.reminder.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lentikr.reminder.ReminderApplication
import com.lentikr.reminder.ui.add.AddReminderViewModel
import com.lentikr.reminder.ui.list.ReminderListViewModel

/**
 * Provides Factory to create instance of ViewModel for the entire Reminder app
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        // Initializer for AddReminderViewModel
        initializer {
            AddReminderViewModel(reminderApplication().container.reminderRepository)
        }

        // Initializer for ReminderListViewModel
        initializer {
            ReminderListViewModel(reminderApplication().container.reminderRepository)
        }
    }
}

/**
 * Extension function to queries for [Application] object and returns an instance of
 * [ReminderApplication].
 */
fun CreationExtras.reminderApplication(): ReminderApplication = 
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReminderApplication)
