package com.lentikr.reminder.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReminderListViewModel(private val reminderRepository: ReminderRepository) : ViewModel() {

    private val selectionMode = MutableStateFlow(false)
    private val selectedIds = MutableStateFlow<Set<Int>>(emptySet())

    private val reminderItemsFlow = reminderRepository.getAllRemindersStream()
        .onEach { items ->
            val validIds = items.map { it.id }.toSet()
            val filteredSelection = selectedIds.value.filter { it in validIds }.toSet()
            if (filteredSelection != selectedIds.value) {
                selectedIds.value = filteredSelection
            }
        }

    val reminderListUiState: StateFlow<ReminderListUiState> =
        combine(reminderItemsFlow, selectionMode, selectedIds) { items, isSelectionMode, selected ->
            ReminderListUiState(
                itemList = items,
                isSelectionMode = isSelectionMode,
                selectedIds = selected
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = ReminderListUiState()
        )

    fun startSelection(initialId: Int) {
        selectionMode.value = true
        selectedIds.value = setOf(initialId)
    }

    fun toggleSelection(id: Int) {
        selectedIds.update { current ->
            current.toMutableSet().apply {
                if (!add(id)) {
                    remove(id)
                }
            }
        }
        if (!selectionMode.value && selectedIds.value.isNotEmpty()) {
            selectionMode.value = true
        }
    }

    fun exitSelectionMode() {
        selectedIds.value = emptySet()
        selectionMode.value = false
    }

    fun isSelected(id: Int): Boolean = selectedIds.value.contains(id)

    fun deleteSelected() {
        val idsToDelete = selectedIds.value
        if (idsToDelete.isEmpty()) return
        viewModelScope.launch {
            reminderRepository.deleteRemindersByIds(idsToDelete)
            exitSelectionMode()
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class ReminderListUiState(
    val itemList: List<ReminderItem> = listOf(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Int> = emptySet()
)
