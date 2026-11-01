package com.lentikr.reminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lentikr.reminder.data.settings.AppSettings
import com.lentikr.reminder.data.settings.ThemeMode
import com.lentikr.reminder.domain.ReminderType

private val LightColors =
  lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
  )

private val DarkColors =
  darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
  )

/** Dark scheme with black surfaces for OLED screens; containers stay slightly lifted. */
private fun ColorScheme.toPureBlack(): ColorScheme =
  copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0D),
    surfaceContainer = Color(0xFF111114),
    surfaceContainerHigh = Color(0xFF19191C),
    surfaceContainerHighest = Color(0xFF222226),
  )

val AppSettings.supportsDynamicColor: Boolean
  get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun AppSettings.isDarkTheme(): Boolean =
  when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

@Composable
fun ReminderTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
  val dark = settings.isDarkTheme()
  val base =
    if (settings.dynamicColor && settings.supportsDynamicColor) {
      val context = LocalContext.current
      if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
      if (dark) DarkColors else LightColors
    }
  val colorScheme = if (dark && settings.pureBlack) base.toPureBlack() else base

  val dynamic = settings.dynamicColor && settings.supportsDynamicColor
  val context = LocalContext.current
  // The share poster always prints in light colors, whatever the app theme is.
  val lightScheme = if (dynamic) dynamicLightColorScheme(context) else LightColors
  CompositionLocalProvider(LocalDynamicColor provides dynamic, LocalLightColorScheme provides lightScheme) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = MotionScheme.expressive(),
      content = content,
    )
  }
}

/** True when colors come from the wallpaper, so brand colors give way to the scheme. */
val LocalDynamicColor = staticCompositionLocalOf { false }

/** The light variant of the active scheme, for content that must look printed. */
val LocalLightColorScheme = staticCompositionLocalOf { LightColors }

/** Color roles for a reminder type: countdowns use primary, count-ups use tertiary. */
@Immutable
data class TypeColors(
  val accent: Color,
  val onAccent: Color,
  val container: Color,
  val onContainer: Color,
)

@Composable
@ReadOnlyComposable
fun ReminderType.colors(): TypeColors {
  val scheme = MaterialTheme.colorScheme
  return when (this) {
    ReminderType.COUNTDOWN ->
      TypeColors(scheme.primary, scheme.onPrimary, scheme.primaryContainer, scheme.onPrimaryContainer)
    ReminderType.COUNT_UP ->
      TypeColors(
        scheme.tertiary,
        scheme.onTertiary,
        scheme.tertiaryContainer,
        scheme.onTertiaryContainer,
      )
  }
}
