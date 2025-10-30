package com.lentikr.reminder.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ReminderListViewModel(reminderRepository: ReminderRepository) : ViewModel() {

    val reminderListUiState: StateFlow<ReminderListUiState> =
        reminderRepository.getAllRemindersStream().map { ReminderListUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = ReminderListUiState()
            )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class ReminderListUiState(
    val itemList: List<ReminderItem> = listOf()
)
