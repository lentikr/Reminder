package com.lentikr.reminder.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lentikr.reminder.R
import com.lentikr.reminder.domain.LunarCalendar
import com.lentikr.reminder.domain.LunarDate
import com.lentikr.reminder.domain.Repeat
import com.lentikr.reminder.domain.RepeatUnit
import com.lentikr.reminder.ui.common.label
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Gregorian date picker. The M3 picker works in UTC milliseconds. */
@Composable
fun SolarDatePickerDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
  val state =
    rememberDatePickerState(
      initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
      yearRange = LUNAR_YEARS,
    )
  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          state.selectedDateMillis?.let {
            onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
          }
        },
        enabled = state.selectedDateMillis != null,
      ) {
        Text(stringResource(R.string.action_confirm))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
  ) {
    DatePicker(state = state)
  }
}

/** Years the lunar picker offers; the same range keeps both pickers consistent. */
private val LUNAR_YEARS = 1901..2099

/** Picks a lunar year, month (including leap months) and day with three wheels. */
@Composable
fun LunarDatePickerSheet(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
  val start = remember(initial) { LunarCalendar.fromSolar(initial) }
  var year by rememberSaveable { mutableIntStateOf(start.year) }
  var month by rememberSaveable { mutableIntStateOf(start.month) }
  var leap by rememberSaveable { mutableStateOf(start.isLeapMonth) }
  var day by rememberSaveable { mutableIntStateOf(start.day) }

  val months = remember(year) { LunarCalendar.monthsOf(year) }
  // Changing the year can drop the selected leap month; fall back to the regular month.
  val monthIndex = months.indexOf(month to leap).takeIf { it >= 0 } ?: months.indexOf(month to false)
  val dayCount = remember(year, month, leap) {
    LunarCalendar.daysInMonth(year, month, leap && LunarCalendar.leapMonthOf(year) == month)
  }
  val safeDay = day.coerceAtMost(dayCount)
  val selected = LunarDate(year, months[monthIndex].first, safeDay, months[monthIndex].second)
  val solar = remember(selected) { LunarCalendar.toSolar(selected) }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding()) {
      Text(stringResource(R.string.edit_date), style = MaterialTheme.typography.titleLarge)
      Spacer(Modifier.height(4.dp))
      Text(
        "${selected.fullName} · ${LunarCalendar.zodiacOf(selected.year)}年",
        style = MaterialTheme.typography.headlineSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        solar.toString(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(16.dp))
      Box(contentAlignment = Alignment.Center) {
        // Highlight band behind the centered row of all three wheels.
        Box(
          Modifier.fillMaxWidth()
            .height(WheelItemHeight)
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp))
        )
        Row(Modifier.fillMaxWidth()) {
          val years = remember { LUNAR_YEARS.toList() }
          WheelPicker(
            items = remember { years.map { "${LunarCalendar.yearName(it)}($it)" } },
            selectedIndex = years.indexOf(year),
            onSelected = { year = years[it] },
            modifier = Modifier.weight(1.6f),
          )
          WheelPicker(
            items = months.map { (m, isLeap) -> LunarCalendar.monthName(m, isLeap) },
            selectedIndex = monthIndex,
            onSelected = {
              month = months[it].first
              leap = months[it].second
            },
            modifier = Modifier.weight(1f),
          )
          WheelPicker(
            items = (1..dayCount).map(LunarCalendar::dayName),
            selectedIndex = safeDay - 1,
            onSelected = { day = it + 1 },
            modifier = Modifier.weight(1f),
          )
        }
      }
      Spacer(Modifier.height(24.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }) {
          Text(stringResource(R.string.action_cancel))
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = { onConfirm(solar) }, shapes = ButtonDefaults.shapes()) {
          Text(stringResource(R.string.action_confirm))
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

private val WheelItemHeight = 48.dp
private const val WheelVisibleItems = 5

/**
 * A snapping vertical wheel. The centered item is the selection; it is reported when scrolling
 * stops. Tapping an item scrolls it to the center.
 */
@Composable
private fun WheelPicker(
  items: List<String>,
  selectedIndex: Int,
  onSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
  val scope = rememberCoroutineScope()
  val itemHeightPx = with(LocalDensity.current) { WheelItemHeight.toPx() }
  val centerIndex by remember {
    derivedStateOf {
      val offsetItems = if (listState.firstVisibleItemScrollOffset > itemHeightPx / 2) 1 else 0
      listState.firstVisibleItemIndex + offsetItems
    }
  }
  val currentOnSelected by rememberUpdatedState(onSelected)
  val currentSize by rememberUpdatedState(items.size)

  LaunchedEffect(listState) {
    snapshotFlow { listState.isScrollInProgress }
      .filter { !it }
      .collect { currentOnSelected(centerIndex.coerceIn(0, currentSize - 1)) }
  }
  // Follow external changes, such as the day list shrinking from 30 to 29.
  LaunchedEffect(selectedIndex, items.size) {
    if (!listState.isScrollInProgress && centerIndex != selectedIndex && selectedIndex in items.indices) {
      listState.scrollToItem(selectedIndex)
    }
  }

  LazyColumn(
    state = listState,
    flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center),
    contentPadding = PaddingValues(vertical = WheelItemHeight * (WheelVisibleItems / 2)),
    modifier = modifier.height(WheelItemHeight * WheelVisibleItems),
  ) {
    itemsIndexed(items, contentType = { _, _ -> "wheel" }) { index, label ->
      val distance = abs(index - centerIndex)
      Box(
        Modifier.fillMaxWidth()
          .height(WheelItemHeight)
          .clickable(role = Role.Button) { scope.launch { listState.animateScrollToItem(index) } }
          .graphicsLayer {
            alpha = when (distance) { 0 -> 1f; 1 -> 0.6f; else -> 0.3f }
            val scale = if (distance == 0) 1f else 0.9f
            scaleX = scale
            scaleY = scale
          },
        contentAlignment = Alignment.Center,
      ) {
        Text(
          label,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
          maxLines = 1,
        )
      }
    }
  }
}

/** Repeat settings: on/off, a unit from a connected button group, and an interval stepper. */
@Composable
fun RepeatSheet(initial: Repeat?, lunar: Boolean, onDismiss: () -> Unit, onConfirm: (Repeat?) -> Unit) {
  var enabled by rememberSaveable { mutableStateOf(initial != null) }
  var unit by rememberSaveable { mutableStateOf(initial?.unit ?: RepeatUnit.YEAR) }
  var interval by rememberSaveable { mutableIntStateOf(initial?.interval ?: 1) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          stringResource(R.string.edit_repeat_enabled),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.weight(1f),
        )
        Switch(checked = enabled, onCheckedChange = { enabled = it })
      }
      Spacer(Modifier.height(20.dp))

      val units = RepeatUnit.entries
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
      ) {
        units.forEachIndexed { index, option ->
          ToggleButton(
            checked = enabled && unit == option,
            onCheckedChange = {
              unit = option
              enabled = true
            },
            shapes =
              when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                units.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
              },
            modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
          ) {
            Text(stringResource(option.labelRes))
          }
        }
      }
      Spacer(Modifier.height(24.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          stringResource(R.string.edit_repeat_interval),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(
          onClick = { interval = (interval - 1).coerceAtLeast(1) },
          enabled = enabled && interval > 1,
          shapes = IconButtonDefaults.shapes(),
        ) {
          Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.edit_repeat_decrease))
        }
        Text(
          "$interval",
          style = MaterialTheme.typography.headlineMediumEmphasized,
          modifier = Modifier.width(72.dp),
          textAlign = TextAlign.Center,
          color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
        )
        FilledTonalIconButton(
          onClick = { interval = (interval + 1).coerceAtMost(Repeat.MAX_INTERVAL) },
          enabled = enabled && interval < Repeat.MAX_INTERVAL,
          shapes = IconButtonDefaults.shapes(),
        ) {
          Icon(painterResource(R.drawable.ic_add), stringResource(R.string.edit_repeat_increase))
        }
      }
      Spacer(Modifier.height(12.dp))
      Text(
        if (enabled) Repeat(interval, unit).label(lunar) else stringResource(R.string.repeat_none),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      if (lunar) {
        Text(
          stringResource(R.string.edit_repeat_lunar_hint),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
      Spacer(Modifier.height(24.dp))
      Button(
        onClick = { onConfirm(if (enabled) Repeat(interval, unit) else null) },
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
      ) {
        Text(stringResource(R.string.action_done), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

private val RepeatUnit.labelRes: Int
  get() =
    when (this) {
      RepeatUnit.DAY -> R.string.repeat_unit_day
      RepeatUnit.WEEK -> R.string.repeat_unit_week
      RepeatUnit.MONTH -> R.string.repeat_unit_month
      RepeatUnit.YEAR -> R.string.repeat_unit_year
    }
