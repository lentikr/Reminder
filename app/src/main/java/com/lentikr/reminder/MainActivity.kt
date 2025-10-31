@file:OptIn(ExperimentalSerializationApi::class)

package com.lentikr.reminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderType
import com.lentikr.reminder.ui.add.AddReminderScreen
import com.lentikr.reminder.ui.common.AppViewModelProvider
import com.lentikr.reminder.ui.list.ReminderListViewModel
import com.lentikr.reminder.ui.settings.SettingsScreen
import com.lentikr.reminder.ui.theme.ReminderTheme
import com.lentikr.reminder.util.CalendarUtil
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReminderApp()
                }
            }
        }
    }
}

object Routes {
    const val REMINDER_LIST = "reminder_list"
    const val ADD_REMINDER = "add_reminder"
    private const val EDIT_REMINDER_BASE = "edit_reminder"
    const val EDIT_REMINDER_PATTERN = "$EDIT_REMINDER_BASE/{reminderId}"
    const val SETTINGS = "settings"

    fun editReminder(reminderId: Int): String = "$EDIT_REMINDER_BASE/$reminderId"
}

@Composable
fun ReminderApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.REMINDER_LIST
    ) {
        composable(
            Routes.REMINDER_LIST,
            exitTransition = {
                when {
                    targetState.destination.route == Routes.SETTINGS ->
                        slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { -it })
                    targetState.destination.route == Routes.ADD_REMINDER ||
                            targetState.destination.route?.startsWith("edit_reminder") == true ->
                        scaleOut(animationSpec = tween(400), targetScale = 0.9f) +
                                fadeOut(animationSpec = tween(400), targetAlpha = 0.7f)
                    else -> fadeOut(animationSpec = tween(400))
                }
            },
            popEnterTransition = {
                when {
                    initialState.destination.route == Routes.SETTINGS ->
                        slideInHorizontally(animationSpec = tween(400), initialOffsetX = { -it })
                    initialState.destination.route == Routes.ADD_REMINDER ||
                            initialState.destination.route?.startsWith("edit_reminder") == true ->
                        scaleIn(animationSpec = tween(400), initialScale = 0.9f) +
                                fadeIn(animationSpec = tween(400), initialAlpha = 0.7f)
                    else -> fadeIn(animationSpec = tween(400))
                }
            }
        ) {
            ReminderListScreen(navController = navController)
        }
        composable(
            Routes.ADD_REMINDER,
            enterTransition = {
                scaleIn(
                    animationSpec = tween(400),
                    transformOrigin = TransformOrigin(0.9f, 0.95f)
                ) + fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                scaleOut(
                    animationSpec = tween(400),
                    transformOrigin = TransformOrigin(0.9f, 0.95f)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            AddReminderScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable(
            route = Routes.EDIT_REMINDER_PATTERN,
            arguments = listOf(navArgument("reminderId") { type = NavType.IntType }),
            enterTransition = {
                scaleIn(
                    animationSpec = tween(400),
                    transformOrigin = TransformOrigin(0.9f, 0.95f)
                ) + fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                scaleOut(
                    animationSpec = tween(400),
                    transformOrigin = TransformOrigin(0.9f, 0.95f)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            AddReminderScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable(
            Routes.SETTINGS,
            enterTransition = {
                slideInHorizontally(animationSpec = tween(400), initialOffsetX = { it })
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { it })
            }
        ) {
            SettingsScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}

private val ReminderCardShape = RoundedCornerShape(24.dp)

private enum class ReminderTab constructor(val title: String, val filter: (ReminderItem) -> Boolean) {
    COUNTDOWN("倒数日", { it.type == ReminderType.ANNUAL }),
    COUNTUP("正数日", { it.type == ReminderType.COUNT_UP })
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReminderListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ReminderListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val reminderListUiState by viewModel.reminderListUiState.collectAsState()
    val pagerState = rememberPagerState { ReminderTab.values().size }
    val coroutineScope = rememberCoroutineScope()
    val tabs = ReminderTab.values()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
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
        floatingActionButtonPosition = FabPosition.End,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        height = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(tab.title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val filteredItems = reminderListUiState.itemList.filter(tabs[page].filter)
                if (filteredItems.isEmpty()) {
                    EmptyStateCard(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                            .fillMaxWidth()
                    )
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { reminder ->
                            ReminderSummaryCard(
                                reminder = reminder,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(Routes.editReminder(reminder.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun ReminderSummaryCard(
    reminder: ReminderItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val today = LocalDate.now()
    val highlightColor = categoryColor(reminder.category, MaterialTheme.colorScheme.primary)
    val headerTextColor = contentColorFor(highlightColor)

    val (headerLabelSuffix, dayCount, referenceText) = when (reminder.type) {
        ReminderType.ANNUAL -> {
            val nextDate = if (reminder.isLunar) {
                CalendarUtil.getNextLunarDate(reminder.date)
            } else {
                var candidate = reminder.date.withYear(today.year)
                if (candidate.isBefore(today)) {
                    candidate = candidate.plusYears(1)
                }
                candidate
            }
            val daysRemaining = ChronoUnit.DAYS.between(today, nextDate).toInt()
            val formattedDate = nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
            Triple("还有", daysRemaining.coerceAtLeast(0), formattedDate)
        }

        ReminderType.COUNT_UP -> {
            val daysElapsed = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0) + 1
            val formattedDate = reminder.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
            Triple("第", daysElapsed, formattedDate)
        }
    }

    val headerTitle = "${reminder.title} $headerLabelSuffix"
    val categoryDisplay = reminder.category.takeIf { it.isNotBlank() }

    Card(
        modifier = modifier,
        shape = ReminderCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(highlightColor)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = headerTextColor
                    )
                    if (categoryDisplay != null || reminder.isPinned) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            categoryDisplay?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = headerTextColor.copy(alpha = 0.9f)
                                )
                            }
                            if (reminder.isPinned) {
                                Text(
                                    text = "已置顶",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = headerTextColor.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
               AutoResizeText(
                   text = dayCount.toString(),
                   style = MaterialTheme.typography.displayLarge.copy(
                       fontWeight = FontWeight.Bold,
                       letterSpacing = (-1).sp
                   ),
                   modifier = Modifier.fillMaxWidth()
               )
                Text(
                    text = "天",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
               AutoResizeText(
                   text = referenceText,
                   style = MaterialTheme.typography.bodyMedium,
                   modifier = Modifier.fillMaxWidth()
               )
            }
        }
    }
}

@Composable
fun AutoResizeText(
   text: String,
   style: TextStyle,
   modifier: Modifier = Modifier,
   color: Color = Color.Unspecified
) {
   var resizedTextStyle by remember { mutableStateOf(style) }
   var shouldShrink by remember(text) { mutableStateOf(true) }

   Text(
       text = text,
       color = color,
       modifier = modifier,
       textAlign = TextAlign.Center,
       style = resizedTextStyle,
       softWrap = false,
       onTextLayout = { result ->
           if (shouldShrink && result.didOverflowWidth) {
               resizedTextStyle = resizedTextStyle.copy(
                   fontSize = resizedTextStyle.fontSize * 0.95
               )
           } else {
               shouldShrink = false
           }
       }
   )
}

@Composable
private fun EmptyStateCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = ReminderCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "目前还没有提醒",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "点击右下角的加号添加第一个纪念日吧！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun categoryColor(category: String, fallback: Color): Color {
    if (category.isBlank()) return fallback
    val palette = listOf(
        Color(0xFF1E88E5),
        Color(0xFF43A047),
        Color(0xFFF4511E),
        Color(0xFF6D4C41),
        Color(0xFF8E24AA),
        Color(0xFF00897B)
    )
    val index = kotlin.math.abs(category.lowercase(Locale.getDefault()).hashCode())
    return palette[index % palette.size]
}

@Preview(showBackground = true)
@Composable
private fun ReminderListScreenPreview() {
    ReminderTheme {
        val navController = rememberNavController()
        ReminderListScreen(navController = navController)
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderSummaryCardPreview() {
    ReminderTheme {
        ReminderSummaryCard(
            reminder = ReminderItem(
                id = 1,
                title = "生日还有",
                date = LocalDate.now().plusDays(188),
                type = ReminderType.ANNUAL,
                isLunar = false,
                category = "家人",
                isPinned = true
            ),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            onClick = {}
        )
    }
}




