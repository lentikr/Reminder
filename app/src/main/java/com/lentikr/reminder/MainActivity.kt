package com.lentikr.reminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderType
import com.lentikr.reminder.ui.add.AddReminderScreen
import com.lentikr.reminder.ui.common.AppViewModelProvider
import com.lentikr.reminder.ui.list.ReminderListViewModel
import com.lentikr.reminder.ui.theme.ReminderTheme
import com.lentikr.reminder.util.CalendarUtil
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReminderTheme {
                ReminderApp()
            }
        }
    }
}

object Routes {
    const val REMINDER_LIST = "reminder_list"
    const val ADD_REMINDER = "add_reminder"
}

@Composable
fun ReminderApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.REMINDER_LIST,
    ) {
        composable(Routes.REMINDER_LIST) {
            ReminderListScreen(navController = navController)
        }
        composable(Routes.ADD_REMINDER) {
            AddReminderScreen(onNavigateUp = { navController.navigateUp() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ReminderListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val reminderListUiState by viewModel.reminderListUiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("提醒") },
                actions = {
                    IconButton(onClick = { /* TODO: Handle settings click */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.ADD_REMINDER) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加提醒"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        modifier = modifier
    ) { innerPadding ->
        if (reminderListUiState.itemList.isEmpty()) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("你的提醒会在这里显示。")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(reminderListUiState.itemList) { reminder ->
                    ReminderListItem(reminder = reminder)
                }
            }
        }
    }
}

@Composable
fun ReminderListItem(reminder: ReminderItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.padding(4.dp))
                val (unit, value) = when (reminder.type) {
                    ReminderType.ANNUAL -> {
                        val today = LocalDate.now()
                        val nextDate = if (reminder.isLunar) {
                            CalendarUtil.getNextLunarDate(reminder.date)
                        } else {
                            var nextSolarDate = reminder.date.withYear(today.year)
                            if (nextSolarDate.isBefore(today)) {
                                nextSolarDate = nextSolarDate.plusYears(1)
                            }
                            nextSolarDate
                        }
                        Pair("还有", ChronoUnit.DAYS.between(today, nextDate))
                    }
                    ReminderType.COUNT_UP -> Pair("已经", ChronoUnit.DAYS.between(reminder.date, LocalDate.now()))
                }
                Text(
                    text = "$unit $value 天",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (reminder.category.isNotBlank()) {
                Text(
                    text = reminder.category,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReminderListScreenPreview() {
    ReminderTheme {
        val navController = rememberNavController()
        ReminderListScreen(navController = navController)
    }
}

@Preview(showBackground = true)
@Composable
fun ReminderListItemPreview() {
    ReminderTheme {
        ReminderListItem(
            reminder = ReminderItem(
                id = 1,
                title = "结婚纪念日",
                date = LocalDate.now().plusDays(10),
                type = ReminderType.ANNUAL,
                isLunar = false,
                category = "家庭",
                isPinned = true
            )
        )
    }
}
