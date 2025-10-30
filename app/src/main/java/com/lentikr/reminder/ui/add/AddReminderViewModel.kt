package com.lentikr.reminder.ui.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderRepository
import com.lentikr.reminder.data.ReminderType
import java.time.LocalDate

class AddReminderViewModel(private val reminderRepository: ReminderRepository) : ViewModel() {

    /**
     * Holds current reminder ui state
     */
    var reminderUiState by mutableStateOf(ReminderUiState())
        private set

    /**
     * Updates the [reminderUiState] with the value provided in the argument.
     */
    fun updateUiState(newReminderUiState: ReminderUiState) {
        reminderUiState = newReminderUiState
    }

    suspend fun saveReminder() {
        if (validateInput()) {
            reminderRepository.insertReminder(reminderUiState.toReminderItem())
        }
    }

    private fun validateInput(uiState: ReminderUiState = reminderUiState): Boolean {
        return with(uiState) {
            title.isNotBlank()
        }
    }
}

/**
 * Represents Ui State for a Reminder.
 */
data class ReminderUiState(
    val id: Int = 0,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val type: ReminderType = ReminderType.ANNUAL,
    val isLunar: Boolean = false,
    val category: String = "",
    val isPinned: Boolean = false
)

fun ReminderUiState.toReminderItem(): ReminderItem = ReminderItem(
    id = id,
    title = title,
    date = date,
    type = type,
    isLunar = isLunar,
    category = category,
    isPinned = isPinned
)
