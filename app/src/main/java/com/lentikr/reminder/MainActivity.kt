@file:OptIn(ExperimentalSerializationApi::class)

package com.lentikr.reminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
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

private enum class ReminderTab(val title: String, val filter: (ReminderItem) -> Boolean) {
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
    val pagerState = rememberPagerState { ReminderTab.entries.size }
    val coroutineScope = rememberCoroutineScope()
    val tabs = ReminderTab.entries.toTypedArray()
    val tabCounts = tabs.map { tab -> reminderListUiState.itemList.count(tab.filter) }
    val segmentedHeight = 54.dp
    val segmentedBottomSpacing = 20.dp
    val bottomRowVerticalPadding = 12.dp
    val listBottomPadding = segmentedHeight + segmentedBottomSpacing + bottomRowVerticalPadding + 16.dp

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
        floatingActionButton = {},
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val filteredItems = reminderListUiState.itemList.filter(tabs[page].filter)
                if (filteredItems.isEmpty()) {
                    EmptyStateCard(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                            .padding(bottom = listBottomPadding)
                            .fillMaxWidth()
                    )
                } else {
                    val groupedSections = buildReminderSections(filteredItems)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = listBottomPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        groupedSections.forEach { section ->
                            item(key = section.key) {
                                ReminderSection(
                                    title = section.title,
                                    reminders = section.items,
                                    onReminderClick = { reminderId ->
                                        navController.navigate(Routes.editReminder(reminderId))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = segmentedBottomSpacing, start = 24.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(segmentedHeight))

                    Box(
                        modifier = Modifier
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingSegmentedTabs(
                            tabs = tabs,
                            counts = tabCounts,
                            selectedIndex = pagerState.currentPage,
                            onTabSelected = { index ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            modifier = Modifier
                                .height(segmentedHeight)
                                .widthIn(min = 200.dp, max = 260.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = { navController.navigate(Routes.ADD_REMINDER) },
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(segmentedHeight),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加提醒"
                        )
                    }
                }
            }
        }
    }
}



private data class ReminderSectionData(
    val key: String,
    val title: String,
    val items: List<ReminderItem>
)

private fun buildReminderSections(reminders: List<ReminderItem>): List<ReminderSectionData> {
    if (reminders.isEmpty()) return emptyList()

    val locale = Locale.getDefault()
    compareBy<ReminderItem> { normalizeCategory(it.category).lowercase(locale) }
        .thenBy { it.title.lowercase(locale) }
        .thenBy { it.id }

    val result = mutableListOf<ReminderSectionData>()
    val pinned = reminders.filter { it.isPinned }.sortedWith(
        compareBy<ReminderItem> { reminderSortValue(it) }.thenBy { it.id }
    )
    if (pinned.isNotEmpty()) {
        result += ReminderSectionData(
            key = "pinned",
            title = "置顶",
            items = pinned
        )
    }

    val nonPinned = reminders.filterNot { it.isPinned }
    val grouped = nonPinned.groupBy { normalizeCategory(it.category) }

    val sortedGroups = grouped.keys.sortedWith(
        compareBy<String> { groupSortKey(it).lowercase(locale) }
            .thenBy { it.lowercase(locale) }
    )

    sortedGroups.forEach { category ->
        val items = grouped[category]
            .orEmpty()
            .sortedWith(compareBy<ReminderItem> { reminderSortValue(it) }
                .thenBy { it.title.lowercase(locale) }
                .thenBy { it.id })
        if (items.isNotEmpty()) {
            val title = category.ifBlank { "未分类" }
            val key = if (category.isBlank()) "group_uncategorized" else "group_${category.lowercase(locale)}"
            result += ReminderSectionData(
                key = key,
                title = title,
                items = items
            )
        }
    }

    return result
}

private fun normalizeCategory(category: String): String = category.trim()

private fun groupSortKey(category: String): String {
    if (category.isBlank()) return "#"
    return category.first().toString()
}

private fun reminderSortValue(reminder: ReminderItem): Int {
    val today = LocalDate.now()
    return when (reminder.type) {
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
            ChronoUnit.DAYS.between(today, nextDate).toInt()
        }

        ReminderType.COUNT_UP -> {
            ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
        }
    }
}

@Composable
private fun ReminderSection(
    title: String,
    reminders: List<ReminderItem>,
    onReminderClick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val rows = reminders.chunked(2)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { reminder ->
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            ReminderSummaryCard(
                                reminder = reminder,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp),
                                onClick = { onReminderClick(reminder.id) }
                            )
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingSegmentedTabs(
    tabs: Array<ReminderTab>,
    counts: List<Int>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableIntStateOf(0) }
    val segmentCount = tabs.size.coerceAtLeast(1)
    val indicatorWidthPx = if (containerWidthPx > 0) containerWidthPx / segmentCount else 0
    val indicatorOffsetPx by animateIntAsState(
        targetValue = indicatorWidthPx * selectedIndex,
        animationSpec = tween(durationMillis = 250),
        label = "indicatorOffset"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .onSizeChanged { containerWidthPx = it.width }
        ) {
            if (indicatorWidthPx > 0) {
                val indicatorWidthDp = with(density) { indicatorWidthPx.toDp() }
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(indicatorWidthDp)
                        .offset { IntOffset(indicatorOffsetPx, 0) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.primary)
                ) {}
            }

            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val textColor = if (selected) {
                        contentColorFor(MaterialTheme.colorScheme.primary)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val label = buildString {
                        append(tab.title)
                        if (index in counts.indices) {
                            append(" (")
                            append(counts[index])
                            append(')')
                        }
                    }
                    val interactionSource = remember(index) { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                if (!selected) {
                                    onTabSelected(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
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




