package com.lentikr.reminder.ui.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderRepository
import com.lentikr.reminder.data.ReminderType
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddReminderViewModel(
    private val reminderRepository: ReminderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val reminderId: Int? = savedStateHandle.get<Int>("reminderId")

    /**
     * Holds current reminder ui state
     */
    var reminderUiState by mutableStateOf(ReminderUiState())
        private set

    init {
        reminderId?.let { id ->
            viewModelScope.launch {
                reminderRepository.getReminderStream(id).firstOrNull()?.let { reminder ->
                    reminderUiState = reminder.toReminderUiState()
                }
            }
        }
    }

    /**
     * Updates the [reminderUiState] with the value provided in the argument.
     */
    fun updateUiState(newReminderUiState: ReminderUiState) {
        reminderUiState = newReminderUiState
    }

    suspend fun saveReminder() {
        if (!validateInput()) return

        val reminder = reminderUiState.toReminderItem()
        if (reminder.id == 0) {
            reminderRepository.insertReminder(reminder)
        } else {
            reminderRepository.updateReminder(reminder)
        }
    }

    suspend fun deleteReminder(): Boolean {
        val id = reminderId ?: return false
        val reminder = reminderUiState.toReminderItem().copy(id = id)
        reminderRepository.deleteReminder(reminder)
        return true
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

fun ReminderItem.toReminderUiState(): ReminderUiState = ReminderUiState(
    id = id,
    title = title,
    date = date,
    type = type,
    isLunar = isLunar,
    category = category,
    isPinned = isPinned
)
