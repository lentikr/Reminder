package com.lentikr.reminder.ui.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lentikr.reminder.data.ReminderType
import com.lentikr.reminder.ui.common.AppViewModelProvider
import com.lentikr.reminder.ui.theme.ReminderTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddReminderViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    val uiState = viewModel.reminderUiState
    val isEditing = uiState.id != 0
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val disabledTextFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑提醒" else "新增提醒") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateUiState(uiState.copy(title = it)) },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = uiState.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    onValueChange = { },
                    label = { Text("日期") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false,
                    colors = disabledTextFieldColors
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.date.atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val newDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                    viewModel.updateUiState(uiState.copy(date = newDate))
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("取消")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("类型", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(16.dp))
                ReminderType.entries.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.updateUiState(uiState.copy(type = type)) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = uiState.type == type,
                            onClick = { viewModel.updateUiState(uiState.copy(type = type)) }
                        )
                        val text = when (type) {
                            ReminderType.ANNUAL -> "倒数日"
                            ReminderType.COUNT_UP -> "正数日"
                        }
                        Text(text)
                    }
                }
            }

            SettingSwitch(
                title = "农历",
                checked = uiState.isLunar,
                onCheckedChange = { viewModel.updateUiState(uiState.copy(isLunar = it)) }
            )

            OutlinedTextField(
                value = uiState.category,
                onValueChange = { viewModel.updateUiState(uiState.copy(category = it)) },
                label = { Text("分类（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            SettingSwitch(
                title = "置顶",
                checked = uiState.isPinned,
                onCheckedChange = { viewModel.updateUiState(uiState.copy(isPinned = it)) }
            )

            Spacer(Modifier.weight(1f))

            if (isEditing) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("删除提醒")
                }

                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        title = { Text("确认删除") },
                        text = { Text("确定要删除此提醒吗？") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    coroutineScope.launch {
                                        if (viewModel.deleteReminder()) {
                                            onNavigateUp()
                                        }
                                    }
                                }
                            ) {
                                Text("确认")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                Text("取消")
                            }
                        }
                    )
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.saveReminder()
                        onNavigateUp()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.title.isNotBlank()
            ) {
                Text(if (isEditing) "保存修改" else "保存")
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true)
@Composable
private fun AddReminderScreenPreview() {
    ReminderTheme {
        AddReminderScreen(onNavigateUp = {})
    }
}

