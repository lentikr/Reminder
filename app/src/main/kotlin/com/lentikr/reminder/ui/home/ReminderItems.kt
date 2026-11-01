package com.lentikr.reminder.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lentikr.reminder.R
import com.lentikr.reminder.ui.common.calendarColors
import com.lentikr.reminder.ui.common.PinBadge
import com.lentikr.reminder.ui.common.CalendarSize
import com.lentikr.reminder.ui.common.CalendarPage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.ui.common.DayNumber
import com.lentikr.reminder.ui.common.ShapedIcon
import com.lentikr.reminder.ui.common.caption
import com.lentikr.reminder.ui.common.isInactive
import com.lentikr.reminder.ui.common.shapeFor
import com.lentikr.reminder.ui.theme.colors

@get:androidx.annotation.DrawableRes
internal val ReminderType.icon: Int
  get() = if (this == ReminderType.COUNTDOWN) R.drawable.ic_hourglass_top else R.drawable.ic_history

/**
 * Calendar-page card for the grid layout. Selection morphs the corners from extra-large to medium
 * and outlines the card, using the expressive motion scheme.
 */
@Composable
fun ReminderCard(
  item: ReminderItemUi,
  selected: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val reminder = item.reminder
  val scheme = MaterialTheme.colorScheme
  val motion = MaterialTheme.motionScheme
  val colors = calendarColors(reminder.type, item.count.isInactive)
  val corner by animateDpAsState(if (selected) 12.dp else 24.dp, motion.fastSpatialSpec(), label = "corner")
  val outline by animateColorAsState(if (selected) scheme.primary else Color.Transparent, motion.defaultEffectsSpec(), label = "outline")

  Surface(
    shape = RoundedCornerShape(corner),
    color = colors.page,
    shadowElevation = 2.dp,
    border = BorderStroke(3.dp, outline),
    modifier = modifier.semantics { this.selected = selected },
  ) {
    Box(
      Modifier.combinedClickable(
        role = Role.Button,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = stringResource(R.string.home_select),
      )
    ) {
      CalendarPage(
        reminder = reminder,
        count = item.count,
        size = CalendarSize.Compact,
        colors = colors,
        headerTrailing = if (reminder.isPinned) ({ PinBadge(colors.onHeader) }) else null,
      )
      AnimatedVisibility(
        visible = selected,
        enter = scaleIn(motion.fastSpatialSpec()),
        exit = scaleOut(motion.fastSpatialSpec()),
        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
      ) {
        Icon(
          painterResource(R.drawable.ic_check_circle_filled),
          contentDescription = null,
          tint = scheme.primary,
          modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}

/** Row for the list layout, rendered as one segment of a grouped list. */
@Composable
fun ReminderRow(
  item: ReminderItemUi,
  index: Int,
  count: Int,
  selected: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val reminder = item.reminder
  val typeColors = reminder.type.colors()
  val scheme = MaterialTheme.colorScheme
  val inactive = item.count.isInactive
  val shapes: ListItemShapes = ListItemDefaults.segmentedShapes(index = index, count = count)

  SegmentedListItem(
    selected = selected,
    onClick = onClick,
    onLongClick = onLongClick,
    shapes = shapes,
    colors =
      ListItemDefaults.segmentedColors(
        containerColor = scheme.surfaceContainer,
        selectedContainerColor = scheme.secondaryContainer,
      ),
    modifier = modifier,
    leadingContent = {
      if (selected) {
        ShapedIcon(
          icon = R.drawable.ic_check,
          shape = MaterialShapes.Circle,
          container = scheme.primary,
          content = scheme.onPrimary,
        )
      } else {
        val colors = calendarColors(reminder.type, inactive)
        ShapedIcon(
          icon = reminder.type.icon,
          shape = shapeFor(reminder.id),
          container = colors.header,
          content = colors.onHeader,
        )
      }
    },
    supportingContent = {
      Text(item.count.caption(reminder), maxLines = 1, overflow = TextOverflow.Ellipsis)
    },
    trailingContent = {
      Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        if (reminder.isPinned) {
          Icon(
            painterResource(R.drawable.ic_keep_filled),
            contentDescription = stringResource(R.string.home_pin),
            modifier = Modifier.padding(end = 8.dp).size(16.dp),
          )
        }
        DayNumber(
          count = item.count,
          numberStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
          labelStyle = MaterialTheme.typography.labelMedium,
          minNumberSize = 14.sp,
          numberColor = if (inactive) scheme.onSurfaceVariant else scheme.onSurface,
        )
      }
    },
  ) {
    Text(reminder.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}
