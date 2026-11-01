package com.lentikr.reminder.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lentikr.reminder.R
import com.lentikr.reminder.data.settings.ListLayout
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.ui.common.EmptyState
import com.lentikr.reminder.ui.common.SectionHeader
import com.lentikr.reminder.ui.common.TooltipIconButton
import com.lentikr.reminder.ui.common.labelRes
import com.lentikr.reminder.ui.common.lunarMonthDay
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.animateColorAsState
import com.lentikr.reminder.ui.common.calendarColors
import kotlinx.coroutines.launch

private val SubtitleDate: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)

/** Space kept free under the list so the floating toolbar never covers the last item. */
private val ToolbarClearance = 104.dp

@Composable
fun HomeScreen(
  onOpenReminder: (Long) -> Unit,
  onCreate: (ReminderType) -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val resources = LocalResources.current

  LaunchedEffect(viewModel) {
    viewModel.deletions.collect { deleted ->
      val result =
        snackbarHostState.showSnackbar(
          message = resources.getString(R.string.home_deleted, deleted.size),
          actionLabel = resources.getString(R.string.action_undo),
          duration = SnackbarDuration.Long,
        )
      if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(deleted)
    }
  }

  BackHandler(enabled = state.inSelection) { viewModel.clearSelection() }

  HomeContent(
    state = state,
    snackbarHostState = snackbarHostState,
    onOpenReminder = onOpenReminder,
    onCreate = onCreate,
    onOpenSettings = onOpenSettings,
    onToggleSelection = viewModel::toggleSelection,
    onSelectAll = viewModel::selectAll,
    onClearSelection = viewModel::clearSelection,
    onPinSelected = viewModel::pinSelected,
    onDeleteSelected = viewModel::deleteSelected,
    onLayoutChange = viewModel::setLayout,
    modifier = modifier,
  )
}

@Composable
internal fun HomeContent(
  state: HomeUiState,
  snackbarHostState: SnackbarHostState,
  onOpenReminder: (Long) -> Unit,
  onCreate: (ReminderType) -> Unit,
  onOpenSettings: () -> Unit,
  onToggleSelection: (Long) -> Unit,
  onSelectAll: (ReminderType) -> Unit,
  onClearSelection: () -> Unit,
  onPinSelected: () -> Unit,
  onDeleteSelected: () -> Unit,
  onLayoutChange: (ListLayout) -> Unit,
  modifier: Modifier = Modifier,
) {
  val types = ReminderType.entries
  val pagerState = rememberPagerState { types.size }
  val currentType = types[pagerState.currentPage]
  val scope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
  val motion = MaterialTheme.motionScheme

  val onItemClick: (ReminderItemUi) -> Unit = { item ->
    if (state.inSelection) onToggleSelection(item.id) else onOpenReminder(item.id)
  }
  val onItemLongClick: (ReminderItemUi) -> Unit = { item ->
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    onToggleSelection(item.id)
  }

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      AnimatedContent(
        targetState = state.inSelection,
        transitionSpec = { fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
        label = "topBar",
      ) { inSelection ->
        if (inSelection) {
          TopAppBar(
            title = { Text(stringResource(R.string.home_selected, state.selectedIds.size)) },
            navigationIcon = {
              TooltipIconButton(R.drawable.ic_close, stringResource(R.string.action_cancel), onClearSelection)
            },
            actions = {
              TooltipIconButton(
                R.drawable.ic_select_all,
                stringResource(R.string.home_select_all),
                { onSelectAll(currentType) },
              )
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
              ),
          )
        } else {
          // A single-row bar: a large one keeps a tall empty band above the title until the list
          // scrolls, and a few reminders rarely scroll.
          TopAppBar(
            title = { Text(stringResource(R.string.app_name), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            subtitle = {
              Text(
                stringResource(R.string.home_today, state.today.format(SubtitleDate), state.today.lunarMonthDay()),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            actions = {
              val cards = state.listLayout == ListLayout.CARDS
              TooltipIconButton(
                icon = if (cards) R.drawable.ic_view_agenda else R.drawable.ic_grid_view,
                label = stringResource(if (cards) R.string.home_layout_list else R.string.home_layout_cards),
                onClick = { onLayoutChange(if (cards) ListLayout.LIST else ListLayout.CARDS) },
              )
              TooltipIconButton(R.drawable.ic_settings, stringResource(R.string.action_settings), onOpenSettings)
            },
            scrollBehavior = scrollBehavior,
          )
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButtonPosition = FabPosition.Center,
    floatingActionButton = {
      AnimatedContent(
        targetState = state.inSelection,
        transitionSpec = {
          (slideInVertically(motion.defaultSpatialSpec()) { it } + fadeIn()) togetherWith
            (slideOutVertically(motion.fastSpatialSpec()) { it } + fadeOut())
        },
        label = "toolbar",
      ) { inSelection ->
        if (inSelection) {
          SelectionToolbar(
            allPinned = state.selectionAllPinned,
            onPin = onPinSelected,
            onDelete = onDeleteSelected,
          )
        } else {
          TypeToolbar(
            expanded = toolbarExpanded,
            current = currentType,
            // 0 on the countdown page, 1 on the count-up page, in between while swiping.
            position = { (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f) },
            countdownTotal = state.countdownTotal,
            countUpTotal = state.countUpTotal,
            onSelect = { type -> scope.launch { pagerState.animateScrollToPage(type.ordinal) } },
            onCreate = { onCreate(currentType) },
          )
        }
      }
    },
  ) { innerPadding ->
    if (state.loading) {
      Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        LoadingIndicator()
      }
      return@Scaffold
    }
    HorizontalPager(
      state = pagerState,
      key = { types[it].name },
      modifier =
        Modifier.fillMaxSize()
          .floatingToolbarVerticalNestedScroll(
            expanded = toolbarExpanded,
            onExpand = { toolbarExpanded = true },
            onCollapse = { toolbarExpanded = false },
          ),
    ) { page ->
      val type = types[page]
      ReminderPage(
        type = type,
        sections = state.sections(type),
        layout = state.listLayout,
        selectedIds = state.selectedIds,
        contentPadding = innerPadding,
        onClick = onItemClick,
        onLongClick = onItemLongClick,
      )
    }
  }
}

@Composable
private fun ReminderPage(
  type: ReminderType,
  sections: List<ReminderSection>,
  layout: ListLayout,
  selectedIds: Set<Long>,
  contentPadding: PaddingValues,
  onClick: (ReminderItemUi) -> Unit,
  onLongClick: (ReminderItemUi) -> Unit,
) {
  val direction = LocalLayoutDirection.current
  val padding =
    PaddingValues(
      start = contentPadding.calculateStartPadding(direction) + 16.dp,
      end = contentPadding.calculateEndPadding(direction) + 16.dp,
      top = contentPadding.calculateTopPadding() + 8.dp,
      bottom = contentPadding.calculateBottomPadding() + ToolbarClearance,
    )

  if (sections.isEmpty()) {
    Box(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (type == ReminderType.COUNTDOWN) {
        EmptyState(
          icon = R.drawable.ic_hourglass_top,
          title = stringResource(R.string.home_empty_countdown_title),
          body = stringResource(R.string.home_empty_countdown_body),
          shape = MaterialShapes.Cookie9Sided,
        )
      } else {
        EmptyState(
          icon = R.drawable.ic_history,
          title = stringResource(R.string.home_empty_count_up_title),
          body = stringResource(R.string.home_empty_count_up_body),
          shape = MaterialShapes.Sunny,
        )
      }
    }
    return
  }

  val cards = layout == ListLayout.CARDS
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyVerticalGrid(
      // 150dp fits two columns on a 360dp phone (328dp after padding) and more on wider windows.
      columns = if (cards) GridCells.Adaptive(minSize = 150.dp) else GridCells.Fixed(1),
      state = rememberLazyGridState(),
      contentPadding = padding,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(if (cards) 12.dp else ListItemDefaults.SegmentedGap),
      // A single-column list stays readable on wide screens instead of stretching edge to edge.
      modifier = if (cards) Modifier.fillMaxSize() else Modifier.fillMaxSize().widthIn(max = 720.dp),
    ) {
      for (section in sections) {
        val title = section.title
        if (title != null) {
          item(key = "h:${section.key}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
            SectionHeader(title.text(), Modifier.animateItem())
          }
        }
        if (cards) {
          items(section.items, key = { it.id }, contentType = { "card" }) { item ->
            ReminderCard(
              item = item,
              selected = item.id in selectedIds,
              onClick = { onClick(item) },
              onLongClick = { onLongClick(item) },
              modifier = Modifier.animateItem(),
            )
          }
        } else {
          itemsIndexed(section.items, key = { _, item -> item.id }, contentType = { _, _ -> "row" }) {
            index,
            item ->
            ReminderRow(
              item = item,
              index = index,
              count = section.items.size,
              selected = item.id in selectedIds,
              onClick = { onClick(item) },
              onLongClick = { onLongClick(item) },
              modifier = Modifier.animateItem(),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SectionTitle.text(): String =
  when (this) {
    SectionTitle.Pinned -> stringResource(R.string.home_section_pinned)
    SectionTitle.Uncategorized -> stringResource(R.string.home_section_uncategorized)
    is SectionTitle.Category -> name
  }

/** Gap between the toolbar edge and the switch indicator, the same on every side. */
private val SwitchInset = 8.dp

/**
 * Floating toolbar with the type switch and the add FAB. The switch indicator slides with the
 * pager while swiping. It is inset by [SwitchInset] on every side, so its corners are concentric
 * with the toolbar's. The indicator and the FAB take the current type's colors, the same ones its
 * cards use.
 */
@Composable
private fun TypeToolbar(
  expanded: Boolean,
  current: ReminderType,
  position: () -> Float,
  countdownTotal: Int,
  countUpTotal: Int,
  onSelect: (ReminderType) -> Unit,
  onCreate: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val motion = MaterialTheme.motionScheme
  val currentColors = calendarColors(current, inactive = false)
  val fabContainer by animateColorAsState(currentColors.header, motion.defaultEffectsSpec(), label = "fab")
  val fabContent by animateColorAsState(currentColors.onHeader, motion.defaultEffectsSpec(), label = "fabContent")
  HorizontalFloatingToolbar(
    expanded = expanded,
    floatingActionButton = {
      TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(stringResource(R.string.home_add)) } },
        state = rememberTooltipState(),
      ) {
        FloatingToolbarDefaults.StandardFloatingActionButton(
          onClick = onCreate,
          containerColor = fabContainer,
          contentColor = fabContent,
        ) {
          Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.home_add))
        }
      }
    },
    colors =
      FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = scheme.surfaceContainerHigh,
        toolbarContentColor = scheme.onSurfaceVariant,
      ),
    contentPadding = PaddingValues(SwitchInset),
  ) {
    TypeSwitch(
      expanded = expanded,
      current = current,
      position = position,
      totals = { if (it == ReminderType.COUNTDOWN) countdownTotal else countUpTotal },
      onSelect = onSelect,
    )
  }
}

/**
 * Two equal-width segments over one sliding indicator. The labels are drawn twice, in the toolbar's
 * content color outside the indicator and in the indicator's content color inside it, so a label
 * changes color exactly where the indicator covers it. [position] is only read while drawing, so
 * swiping doesn't recompose the toolbar.
 */
@Composable
private fun TypeSwitch(
  expanded: Boolean,
  current: ReminderType,
  position: () -> Float,
  totals: (ReminderType) -> Int,
  onSelect: (ReminderType) -> Unit,
) {
  val types = ReminderType.entries
  val motion = MaterialTheme.motionScheme
  // The color follows the page the pager has settled on, which flips halfway through a swipe.
  // Blending blue into orange by position would pass through a muddy grey.
  val currentColors = calendarColors(current, inactive = false)
  val indicatorColor by animateColorAsState(currentColors.header, motion.fastEffectsSpec(), label = "indicator")
  val onIndicator by animateColorAsState(currentColors.onHeader, motion.fastEffectsSpec(), label = "onIndicator")
  val path = remember { Path() }
  fun DrawScope.indicator(): Path {
    val width = size.width / types.size
    val left = position() * (size.width - width)
    return path.apply {
      rewind()
      addRoundRect(RoundRect(left, 0f, left + width, size.height, CornerRadius(size.height / 2)))
    }
  }

  Box(Modifier.height(FloatingToolbarDefaults.ContainerSize - SwitchInset * 2).width(IntrinsicSize.Max)) {
    SwitchLabels(
      types,
      totals,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier =
        Modifier.drawWithContent {
          val indicator = indicator()
          drawPath(indicator, indicatorColor)
          clipPath(indicator, ClipOp.Difference) { this@drawWithContent.drawContent() }
        },
    )
    SwitchLabels(
      types,
      totals,
      color = onIndicator,
      modifier = Modifier.matchParentSize().drawWithContent { clipPath(indicator()) { this@drawWithContent.drawContent() } },
    )
    // Touch targets on top, so the ripple shows over the indicator too.
    Row(Modifier.matchParentSize().selectableGroup()) {
      for (type in types) {
        val label = stringResource(type.labelRes)
        val total = totals(type)
        Box(
          Modifier.weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .focusProperties { canFocus = expanded }
            .semantics { contentDescription = if (total > 0) "$label $total" else label }
            .selectable(selected = type == current, role = Role.Tab, onClick = { onSelect(type) })
        )
      }
    }
  }
}

@Composable
private fun SwitchLabels(
  types: List<ReminderType>,
  totals: (ReminderType) -> Int,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Row(modifier.fillMaxHeight().clearAndSetSemantics {}) {
    for (type in types) {
      val total = totals(type)
      Row(
        Modifier.weight(1f).fillMaxHeight().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(painterResource(type.icon), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(type.labelRes), color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        if (total > 0) {
          Spacer(Modifier.width(6.dp))
          Text("$total", color = color.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun SelectionToolbar(allPinned: Boolean, onPin: () -> Unit, onDelete: () -> Unit) {
  HorizontalFloatingToolbar(
    expanded = true,
    colors =
      FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        toolbarContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
  ) {
    TooltipIconButton(
      icon = if (allPinned) R.drawable.ic_keep_off else R.drawable.ic_keep,
      label = stringResource(if (allPinned) R.string.home_unpin else R.string.home_pin),
      onClick = onPin,
    )
    TooltipIconButton(R.drawable.ic_delete, stringResource(R.string.action_delete), onDelete)
  }
}
