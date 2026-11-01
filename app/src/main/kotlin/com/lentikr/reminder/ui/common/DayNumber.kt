package com.lentikr.reminder.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lentikr.reminder.domain.DayCount

/**
 * "还有 128 天" with the number in an emphasized display style. The number shrinks to fit, so very
 * large counts never wrap or clip.
 */
@Composable
fun DayNumber(
  count: DayCount,
  modifier: Modifier = Modifier,
  numberStyle: TextStyle = MaterialTheme.typography.displayMediumEmphasized,
  labelStyle: TextStyle = MaterialTheme.typography.titleSmall,
  minNumberSize: TextUnit = 20.sp,
  numberColor: Color = LocalContentColor.current,
  labelColor: Color = LocalContentColor.current.copy(alpha = 0.78f),
) {
  val text = count.text()
  Row(modifier = modifier) {
    if (text.prefix.isNotEmpty()) {
      Text(text.prefix, style = labelStyle, color = labelColor, modifier = Modifier.alignByBaseline())
      Spacer(Modifier.width(4.dp))
    }
    Text(
      text = text.number,
      style = numberStyle,
      color = numberColor,
      maxLines = 1,
      softWrap = false,
      autoSize = TextAutoSize.StepBased(minFontSize = minNumberSize, maxFontSize = numberStyle.fontSize),
      modifier = Modifier.alignByBaseline().weight(1f, fill = false),
    )
    if (text.suffix.isNotEmpty()) {
      Text(
        text.suffix,
        style = labelStyle,
        color = labelColor,
        modifier = Modifier.alignByBaseline().padding(start = 4.dp),
      )
    }
  }
}
