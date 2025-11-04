package com.lentikr.reminder.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lentikr.reminder.data.RepeatInfo
import com.lentikr.reminder.data.RepeatUnit
import com.lentikr.reminder.ui.theme.ReminderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatSettingDialog(
    repeatInfo: RepeatInfo?,
    availableUnits: List<RepeatUnit>,
    onDismissRequest: () -> Unit,
    onConfirm: (RepeatInfo?) -> Unit
) {
    var sliderValue by remember { mutableStateOf(repeatInfo?.interval?.toFloat() ?: 1f) }
    var textValue by remember { mutableStateOf(repeatInfo?.interval?.toString() ?: "1") }

    var unitExpanded by remember { mutableStateOf(false) }
    val unitOptions = remember(availableUnits) {
        availableUnits.map { unit ->
            when (unit) {
                RepeatUnit.DAY -> "天"
                RepeatUnit.WEEK -> "周"
                RepeatUnit.MONTH -> "月"
                RepeatUnit.YEAR -> "年"
            }
        }
    }
    var selectedUnitText by remember {
        mutableStateOf(
            if (repeatInfo != null && availableUnits.contains(repeatInfo.unit)) {
                unitOptions[availableUnits.indexOf(repeatInfo.unit)]
            } else {
                unitOptions.firstOrNull() ?: ""
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("设置重复周期") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 第一行：不重复按钮
                Button(
                    onClick = { onConfirm(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text("不重复")
                }

                // 第二行：Slider + TextField
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            textValue = it.toInt().toString()
                        },
                        valueRange = 1f..30f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            val intVal = newValue.toIntOrNull()
                            if (intVal != null && intVal in 1..30) {
                                textValue = newValue
                                sliderValue = intVal.toFloat()
                            } else if (newValue.isEmpty()) {
                                textValue = ""
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                }

                // 第三行：单位选择 - 居中布局 + 样式优化
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "单位：",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.width(120.dp)
                    ) {
                        TextField(
                            value = selectedUnitText,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            unitOptions.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        selectedUnitText = item
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val interval = textValue.toIntOrNull() ?: 1
                val unit = availableUnits[unitOptions.indexOf(selectedUnitText)]
                onConfirm(RepeatInfo(interval, unit))
            }) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun RepeatSettingDialogPreview() {
    ReminderTheme {
        RepeatSettingDialog(
            repeatInfo = RepeatInfo(1, RepeatUnit.DAY),
            availableUnits = RepeatUnit.entries.toList(),
            onDismissRequest = {},
            onConfirm = {}
        )
    }
}
