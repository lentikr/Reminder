package com.lentikr.reminder.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lentikr.reminder.R
import com.lentikr.reminder.ReminderCardVisuals
import com.lentikr.reminder.Routes
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderType
import com.lentikr.reminder.reminderDisplayInfo
import com.lentikr.reminder.ui.common.AppViewModelProvider
import com.lentikr.reminder.ui.common.AutoResizeText
import com.lentikr.reminder.ui.theme.ReminderTheme
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch
import java.time.LocalDate

@ExperimentalComposeUiApi
 @Composable
 fun DetailScreen(
    navController: NavController,
    viewModel: DetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val reminderItem = uiState.reminderItem
    val captureController = rememberCaptureController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { result ->
            val message = when (result) {
                is SaveResult.Success -> "已保存到相册"
                is SaveResult.Failure -> "保存失败"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopAppBar(
                onBackClick = { navController.navigateUp() },
                onEditClick = {
                    reminderItem?.let {
                        navController.navigate(Routes.editReminder(it.id))
                    }
                }
            )
        }
    ) { paddingValues ->
        // The invisible composable for capture
        if (reminderItem != null) {
            Box(modifier = Modifier.offset(y = (10000).dp)) {
                ShareableReminderImage(
                    reminderItem = reminderItem,
                    modifier = Modifier.capturable(captureController)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (reminderItem != null) {
                ReminderDetailCard(
                    reminderItem = reminderItem
                )
            } else {
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.height(24.dp))
            ActionButtonsRow(
                onShareClick = {
                    coroutineScope.launch {
                        try {
                            val imageBitmap = captureController.captureAsync().await()
                            viewModel.shareReminder(imageBitmap.asAndroidBitmap(), context)
                        } catch (_: Throwable) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("截图失败，请重试")
                            }
                        }
                    }
                },
                onSaveClick = {
                    coroutineScope.launch {
                        try {
                            val imageBitmap = captureController.captureAsync().await()
                            viewModel.saveReminderAsImage(imageBitmap.asAndroidBitmap(), context)
                        } catch (_: Throwable) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("截图失败，请重试")
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopAppBar(onBackClick: () -> Unit, onEditClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("详情") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit"
                )
            }
        }
    )
}

@Composable
fun ShareableReminderImage(
    reminderItem: ReminderItem,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.reminder),
                contentDescription = "Reminder",
                modifier = Modifier.padding(vertical = 16.dp)
            )
            ReminderDetailCard(
                reminderItem = reminderItem,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(
                        id = if (reminderItem.type == ReminderType.COUNT_UP) {
                            R.drawable.count_up
                        } else {
                            R.drawable.annual
                        }
                    ),
                    contentDescription = null
                )
            }
        }
    }
}


@Composable
private fun DayCountRow(dayCount: Int, visuals: ReminderCardVisuals) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        AutoResizeText(
            text = dayCount.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 140.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                color = visuals.numberColor
            ),
            modifier = Modifier
                .alignByBaseline(),
            checkHeight = true
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "天",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 30.sp
            ),
            color = visuals.secondaryTextColor,
            modifier = Modifier.alignByBaseline()
        )
    }
}


@Composable
fun ReminderDetailCard(
    reminderItem: ReminderItem,
    modifier: Modifier = Modifier
) {
    val displayInfo = reminderDisplayInfo(reminderItem, isDetailView = true)
    val visuals = displayInfo.visuals

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = visuals.cardBackground)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .background(visuals.headerColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayInfo.headerTitle,
                    color = visuals.headerContentColor,
                    fontSize = 20.sp,
                    maxLines = 1
                )
            }

            // Middle content section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .background(visuals.cardBackground)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DayCountRow(dayCount = displayInfo.dayCount, visuals = visuals)
            }

            // Bottom date section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .background(visuals.footerBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "目标日: ${displayInfo.referenceText}",
                    color = visuals.secondaryTextColor,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ActionButtonsRow(onShareClick: () -> Unit, onSaveClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onShareClick) {
            Text("分享")
        }
        Spacer(modifier = Modifier.width(16.dp))
        OutlinedButton(onClick = onSaveClick) {
            Text("存为图片")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    ReminderTheme {
        Surface {
            ReminderDetailCard(
                reminderItem = ReminderItem(
                    id = 1,
                    title = "示例事件",
                    date = LocalDate.now().plusDays(4),
                    type = ReminderType.ANNUAL,
                    isLunar = false,
                    category = "Default",
                    isPinned = false
                )
            )
        }
    }
}