package com.lentikr.reminder.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lentikr.reminder.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import com.lentikr.reminder.data.settings.PosterStyle
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.ui.common.CalendarPage
import com.lentikr.reminder.ui.common.CalendarSize
import com.lentikr.reminder.ui.common.calendarColors
import com.lentikr.reminder.ui.theme.LocalLightColorScheme
import kotlin.random.Random
import com.lentikr.reminder.domain.DayCount
import com.lentikr.reminder.domain.Reminder
import com.lentikr.reminder.ui.common.isInactive
import com.lentikr.reminder.ui.common.labelRes
import com.lentikr.reminder.ui.common.shapeFor

/** The poster is designed at this size; previews scale it, exports render it at [ExportDensity]. */
private val PosterWidth = 360.dp
private val PosterHeight = 480.dp

/** 360 dp × 4 = 1440 px wide, 1920 px tall: sharp on any phone screen or chat preview. */
private const val ExportDensity = 4f

/**
 * The share image, after the original app: the "Reminder" wordmark, a large calendar page and the
 * type label, on one of three backgrounds. It always prints in the light palette.
 */
@Composable
fun SharePoster(reminder: Reminder, count: DayCount, style: PosterStyle, modifier: Modifier = Modifier) {
  MaterialTheme(colorScheme = LocalLightColorScheme.current) {
    val colors = calendarColors(reminder.type, count.isInactive)
    val ink = if (style == PosterStyle.BLOOM) colors.header else Color.White
    Box(modifier.size(PosterWidth, PosterHeight).clip(RectangleShape)) {
      PosterBackground(style, colors.header, reminder.id)
      Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Image(
          painterResource(R.drawable.poster_wordmark),
          contentDescription = stringResource(R.string.app_name),
          colorFilter = ColorFilter.tint(ink),
          modifier = Modifier.width(168.dp).aspectRatio(138f / 34f),
        )
        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = colors.page, shadowElevation = 10.dp) {
          CalendarPage(reminder, count, CalendarSize.Poster, colors)
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            painterResource(R.drawable.ic_app_logo),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
          )
          Spacer(Modifier.width(8.dp))
          Image(
            painterResource(
              if (reminder.type == ReminderType.COUNT_UP) R.drawable.poster_label_count_up
              else R.drawable.poster_label_countdown
            ),
            contentDescription = stringResource(reminder.type.labelRes),
            colorFilter = ColorFilter.tint(ink),
            modifier = Modifier.width(150.dp).aspectRatio(9f),
          )
        }
      }
    }
  }
}

@Composable
private fun BoxScope.PosterBackground(style: PosterStyle, accent: Color, seed: Long) {
  when (style) {
    PosterStyle.WOOD -> {
      Image(
        painterResource(R.drawable.poster_wood),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
      )
      // Darken the edges a little so the white wordmark and label stay legible.
      Box(
        Modifier.matchParentSize()
          .background(
            Brush.verticalGradient(
              0f to Color.Black.copy(alpha = 0.28f),
              0.3f to Color.Transparent,
              0.8f to Color.Transparent,
              1f to Color.Black.copy(alpha = 0.32f),
            )
          )
      )
    }
    PosterStyle.BLOOM -> {
      Box(
        Modifier.matchParentSize()
          .background(Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.80f), lerp(accent, Color.White, 0.94f))))
      )
      Decoration(MaterialShapes.Cookie12Sided.toShape(), accent.copy(alpha = 0.16f), 280.dp, 14f, Modifier.align(Alignment.TopEnd).offset(110.dp, (-96).dp))
      Decoration(shapeFor(seed).toShape(), accent.copy(alpha = 0.12f), 240.dp, -18f, Modifier.align(Alignment.BottomStart).offset((-96).dp, 84.dp))
      Decoration(MaterialShapes.Flower.toShape(), accent.copy(alpha = 0.10f), 96.dp, 8f, Modifier.align(Alignment.BottomEnd).offset((-24).dp, (-56).dp))
    }
    PosterStyle.NIGHT -> {
      Box(
        Modifier.matchParentSize()
          .background(Brush.verticalGradient(listOf(Color(0xFF0E1A3A), Color(0xFF232457), Color(0xFF3A2A5C))))
      )
      Box(
        Modifier.align(Alignment.TopEnd)
          .offset(70.dp, (-70).dp)
          .size(240.dp)
          .background(Brush.radialGradient(listOf(Color(0xFFFFE6A8).copy(alpha = 0.32f), Color.Transparent)))
      )
      val random = remember(seed) { Random(seed) }
      val stars = remember(seed) { List(28) { Triple(random.nextInt(0, 360), random.nextInt(0, 480), random.nextInt(2, 5)) } }
      for ((x, y, d) in stars) {
        Box(Modifier.offset(x.dp, y.dp).size(d.dp).background(Color.White.copy(alpha = 0.25f + d * 0.12f), CircleShape))
      }
    }
  }
}

@Composable
private fun Decoration(shape: Shape, color: Color, size: androidx.compose.ui.unit.Dp, rotation: Float, modifier: Modifier) {
  Box(modifier.size(size).graphicsLayer { rotationZ = rotation }.background(color, shape))
}

/**
 * Shows the poster scaled to the available width. It renders at the design size with a scaled
 * density, so the preview is laid out exactly like the exported image.
 */
@Composable
fun PosterPreview(modifier: Modifier = Modifier, poster: @Composable () -> Unit) {
  BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(PosterWidth / PosterHeight)) {
    val density = LocalDensity.current
    val scale = maxWidth / PosterWidth
    CompositionLocalProvider(LocalDensity provides Density(density.density * scale, fontScale = 1f)) { poster() }
  }
}

/**
 * Lays the poster out at [ExportDensity] without taking space or drawing on screen, and records
 * it into [layer] so it can be turned into a full-resolution bitmap on demand.
 */
@Composable
fun PosterExportHost(layer: GraphicsLayer, poster: @Composable () -> Unit) {
  Box(
    Modifier.clearAndSetSemantics {}.layout { measurable, _ ->
      val placeable = measurable.measure(Constraints())
      layout(0, 0) { placeable.place(0, 0) }
    }
  ) {
    CompositionLocalProvider(LocalDensity provides Density(ExportDensity, fontScale = 1f)) {
      Box(Modifier.drawWithContent { layer.record { this@drawWithContent.drawContent() } }) { poster() }
    }
  }
}
