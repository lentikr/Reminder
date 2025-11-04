package com.lentikr.reminder.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    checkHeight: Boolean = false
) {
    var resizedTextStyle by remember { mutableStateOf(style) }
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier) {
        val readyToDraw = remember { mutableStateOf(false) }
        val rememberedStyle = remember(resizedTextStyle) { resizedTextStyle }

        LaunchedEffect(text, rememberedStyle, constraints) {
            var currentFontSize = resizedTextStyle.fontSize
            val maxFontSize = currentFontSize
            val minFontSize = 1.sp

            while (currentFontSize > minFontSize) {
                val result = textMeasurer.measure(
                    text,
                    rememberedStyle.copy(fontSize = currentFontSize)
                )
                val overflow = if (checkHeight) {
                    result.size.width > constraints.maxWidth || result.size.height > constraints.maxHeight
                } else {
                    result.size.width > constraints.maxWidth
                }
                if (!overflow) {
                    break
                }
                currentFontSize *= 0.95f
            }

            resizedTextStyle = rememberedStyle.copy(fontSize = currentFontSize)
            readyToDraw.value = true
        }

        if (readyToDraw.value) {
            Text(
                text = text,
                color = color,
                textAlign = TextAlign.Center,
                style = resizedTextStyle,
                softWrap = false
            )
        }
    }
}