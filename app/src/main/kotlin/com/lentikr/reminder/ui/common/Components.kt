package com.lentikr.reminder.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.lentikr.reminder.domain.DayCount

/** Icon button with a plain tooltip, as the Material 3 samples pair them. */
@Composable
fun TooltipIconButton(
  @DrawableRes icon: Int,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  tint: Color = Color.Unspecified,
) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
    tooltip = { PlainTooltip { Text(label) } },
    state = rememberTooltipState(),
  ) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
      Icon(
        painterResource(icon),
        contentDescription = label,
        tint = tint.takeOrElse { LocalContentColor.current },
      )
    }
  }
}

/** An icon centered on one of the Material 3 Expressive shapes. */
@Composable
fun ShapedIcon(
  @DrawableRes icon: Int,
  shape: RoundedPolygon,
  container: Color,
  content: Color,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
) {
  Surface(modifier = modifier.size(size), shape = shape.toShape(), color = container, contentColor = content) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
      Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(size * 0.5f))
    }
  }
}

/** Large centered illustration with a title and a body line, for empty screens. */
@Composable
fun EmptyState(
  @DrawableRes icon: Int,
  title: String,
  body: String,
  modifier: Modifier = Modifier,
  shape: RoundedPolygon = MaterialShapes.Cookie9Sided,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    ShapedIcon(
      icon = icon,
      shape = shape,
      container = MaterialTheme.colorScheme.secondaryContainer,
      content = MaterialTheme.colorScheme.onSecondaryContainer,
      size = 120.dp,
    )
    Spacer(Modifier.size(24.dp))
    Text(
      title,
      style = MaterialTheme.typography.headlineSmallEmphasized,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.size(8.dp))
    Text(
      body,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

/** Section title above a group of list items or cards. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLargeEmphasized,
    fontWeight = FontWeight.SemiBold,
    color = color,
    modifier = modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
  )
}

/** Picks a stable decorative shape for a reminder so cards don't all look alike. */
fun shapeFor(id: Long): RoundedPolygon = DecorativeShapes[Math.floorMod(id, DecorativeShapes.size)]

private val DecorativeShapes =
  listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Sunny,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.SoftBurst,
    MaterialShapes.Flower,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Pentagon,
    MaterialShapes.Gem,
  )

/** True when the count should read as finished (muted colors). */
val DayCount.isInactive: Boolean
  get() = this is DayCount.Ended
