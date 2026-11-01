package com.lentikr.reminder.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

/** Background of the share poster. */
enum class PosterStyle {
  WOOD,
  BLOOM,
  NIGHT,
}

enum class ListLayout {
  CARDS,
  LIST,
}

data class AppSettings(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  /** Pure black surfaces in dark theme, for OLED screens. */
  val pureBlack: Boolean = false,
  /** Wallpaper-based colors on Android 12+. */
  val dynamicColor: Boolean = false,
  val listLayout: ListLayout = ListLayout.CARDS,
  val posterStyle: PosterStyle = PosterStyle.WOOD,
)

@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

  val settings: Flow<AppSettings> =
    dataStore.data
      .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
      .map { prefs ->
        AppSettings(
          themeMode = prefs[Keys.THEME_MODE].toEnum(ThemeMode.SYSTEM),
          pureBlack = prefs[Keys.PURE_BLACK] ?: false,
          dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
          listLayout = prefs[Keys.LIST_LAYOUT].toEnum(ListLayout.CARDS),
          posterStyle = prefs[Keys.POSTER_STYLE].toEnum(PosterStyle.WOOD),
        )
      }

  suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[Keys.THEME_MODE] = mode.name }

  suspend fun setPureBlack(enabled: Boolean) = dataStore.edit { it[Keys.PURE_BLACK] = enabled }

  suspend fun setDynamicColor(enabled: Boolean) =
    dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }

  suspend fun setListLayout(layout: ListLayout) =
    dataStore.edit { it[Keys.LIST_LAYOUT] = layout.name }

  suspend fun setPosterStyle(style: PosterStyle) =
    dataStore.edit { it[Keys.POSTER_STYLE] = style.name }

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val PURE_BLACK = booleanPreferencesKey("pure_black")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val LIST_LAYOUT = stringPreferencesKey("list_layout")
    val POSTER_STYLE = stringPreferencesKey("poster_style")
  }

  companion object {
    const val FILE_NAME = "settings"
  }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
  this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
