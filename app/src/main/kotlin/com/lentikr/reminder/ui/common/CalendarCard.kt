package com.lentikr.reminder.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lentikr.reminder.R
import com.lentikr.reminder.domain.DayCount
import com.lentikr.reminder.domain.Reminder
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.ui.theme.LocalDynamicColor

/** Colors of one calendar page: a strong header band, a light page, a tinted footer. */
@Immutable
data class CalendarColors(
  val header: Color,
  val onHeader: Color,
  val page: Color,
  val number: Color,
  val unit: Color,
  val footer: Color,
  val onFooter: Color,
  val divider: Color,
)

/** Brand colors of the two types, kept from the original app: blue countdowns, orange count-ups. */
private val BrandBlue = Color(0xFF1E6FD9)
private val BrandOrange = Color(0xFFEF7D10)

/**
 * Header colors follow the brand unless dynamic color is on, in which case they come from the
 * wallpaper scheme. The page and footer use surface container roles, so they adapt to dark and
 * pure-black themes. Ended reminders drop to neutral colors.
 */
@Composable
@ReadOnlyComposable
fun calendarColors(type: ReminderType, inactive: Boolean): CalendarColors {
  val scheme = MaterialTheme.colorScheme
  val dark = scheme.surface.luminance() < 0.5f
  val (header, onHeader) =
    when {
      inactive -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
      LocalDynamicColor.current ->
        if (type == ReminderType.COUNTDOWN) scheme.primary to scheme.onPrimary else scheme.tertiary to scheme.onTertiary
      else -> (if (type == ReminderType.COUNTDOWN) BrandBlue else BrandOrange) to Color.White
    }
  return CalendarColors(
    header = header,
    onHeader = onHeader,
    page = if (dark) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest,
    number = if (inactive) scheme.onSurfaceVariant else scheme.onSurface,
    unit = scheme.onSurfaceVariant,
    footer = if (dark) scheme.surfaceContainerHighest else scheme.surfaceContainerLow,
    onFooter = scheme.onSurfaceVariant,
    divider = scheme.outlineVariant.copy(alpha = 0.6f),
  )
}

/** Proportions of the card on the home grid and on the poster. */
enum class CalendarSize(
  val header: Dp,
  val page: Dp,
  val footer: Dp,
  val titleSize: TextUnit,
  val numberMax: TextUnit,
  val numberMin: TextUnit,
  val unitSize: TextUnit,
  val footerSize: TextUnit,
) {
  Compact(header = 44.dp, page = 92.dp, footer = 34.dp, 15.sp, 52.sp, 22.sp, 15.sp, 12.sp),
  Poster(header = 72.dp, page = 184.dp, footer = 68.dp, 26.sp, 132.sp, 48.sp, 26.sp, 15.sp),
}

/**
 * A page from a desk calendar: "<title> 还有" in the header band, the day count large on the page,
 * and the date in the footer. The poster size adds two binder holes, a fuller date and a second
 * date line.
 */
@Composable
fun CalendarPage(
  reminder: Reminder,
  count: DayCount,
  size: CalendarSize,
  colors: CalendarColors,
  modifier: Modifier = Modifier,
  headerTrailing: @Composable (() -> Unit)? = null,
) {
  val poster = size == CalendarSize.Poster
  Column(modifier.fillMaxWidth()) {
    // Header band.
    Box(Modifier.fillMaxWidth().height(size.header).background(colors.header)) {
      if (poster) {
        Row(Modifier.align(Alignment.TopCenter).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(160.dp)) {
          repeat(2) { Box(Modifier.size(10.dp).background(Color.Black.copy(alpha = 0.18f), CircleShape)) }
        }
      }
      Row(
        Modifier.align(Alignment.Center).padding(horizontal = if (poster) 24.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          reminder.title,
          style = MaterialTheme.typography.titleMedium,
          fontSize = size.titleSize,
          fontWeight = FontWeight.Bold,
          color = colors.onHeader,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(if (poster) 8.dp else 4.dp))
        Text(
          count.headerWord(),
          style = MaterialTheme.typography.titleMedium,
          fontSize = size.titleSize,
          fontWeight = FontWeight.Bold,
          color = colors.onHeader,
          maxLines = 1,
        )
        if (headerTrailing != null) {
          Spacer(Modifier.width(4.dp))
          headerTrailing()
        }
      }
    }

    // Page with the number. Baseline alignment ignores the row's vertical alignment, so a Box
    // centers the row in the page.
    Box(
      Modifier.fillMaxWidth().height(size.page).background(colors.page).padding(horizontal = 16.dp),
      contentAlignment = Alignment.Center,
    ) {
    Row(horizontalArrangement = Arrangement.Center) {
      val number = if (count is DayCount.Today) stringResource(R.string.count_today_big) else "${count.dayNumber}"
      val numberStyle =
        TextStyle(
          fontSize = size.numberMax,
          fontWeight = FontWeight.Black,
          letterSpacing = (-1).sp,
          lineHeight = size.numberMax,
          lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        )
      Text(
        number,
        style = numberStyle,
        color = colors.number,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = size.numberMin, maxFontSize = size.numberMax),
        modifier = Modifier.alignByBaseline().weight(1f, fill = false),
      )
      if (count !is DayCount.Today) {
        Text(
          stringResource(R.string.count_unit_day),
          fontSize = size.unitSize,
          color = colors.unit,
          modifier = Modifier.alignByBaseline().padding(start = if (poster) 8.dp else 4.dp),
        )
      }
    }
    }

    HorizontalDivider(color = colors.divider)

    // Footer with the date.
    Column(
      Modifier.fillMaxWidth().height(size.footer).background(colors.footer).padding(horizontal = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        if (poster) count.footerLine(reminder) else count.caption(reminder),
        fontSize = size.footerSize,
        color = colors.onFooter,
        textAlign = TextAlign.Center,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = size.footerSize),
      )
      if (poster) {
        val second = buildList {
          add(if (reminder.isLunar) count.date.formatLong() else "农历" + count.date.lunarMonthDay())
          reminder.repeat?.let { add(it.label(reminder.isLunar)) }
        }
        Text(
          second.joinToString(" · "),
          fontSize = 13.sp,
          color = colors.onFooter.copy(alpha = 0.75f),
          maxLines = 1,
          modifier = Modifier.padding(top = 2.dp),
        )
      }
    }
  }
}

@Composable
@ReadOnlyComposable
private fun DayCount.headerWord(): String =
  stringResource(
    when (this) {
      is DayCount.Upcoming -> R.string.header_remaining
      is DayCount.Today -> R.string.header_today
      is DayCount.Ended -> R.string.header_ended
      is DayCount.Elapsed -> R.string.header_elapsed
      is DayCount.NotStarted -> R.string.header_not_started
    }
  )

@Composable
@ReadOnlyComposable
private fun DayCount.footerLine(reminder: Reminder): String {
  val shown = reminder.displayDate(date)
  return when (this) {
    is DayCount.Upcoming,
    is DayCount.Today -> shown
    is DayCount.Ended -> shown + " · " + stringResource(R.string.count_ended_suffix)
    is DayCount.Elapsed -> stringResource(R.string.count_since, shown)
    is DayCount.NotStarted -> stringResource(R.string.count_starts, shown)
  }
}

@Composable
fun PinBadge(color: Color, modifier: Modifier = Modifier) {
  Icon(
    painterResource(R.drawable.ic_keep_filled),
    contentDescription = stringResource(R.string.home_pin),
    tint = color,
    modifier = modifier.size(14.dp),
  )
}

/** The number shown for a count; today has none. */
val DayCount.dayNumber: Long
  get() =
    when (this) {
      is DayCount.Upcoming -> days
      is DayCount.Today -> 0
      is DayCount.Ended -> days
      is DayCount.Elapsed -> days
      is DayCount.NotStarted -> days
    }
