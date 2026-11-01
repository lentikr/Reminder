package com.lentikr.reminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lentikr.reminder.data.settings.AppSettings
import com.lentikr.reminder.data.settings.SettingsRepository
import com.lentikr.reminder.ui.navigation.AppNavigation
import com.lentikr.reminder.ui.theme.ReminderTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    // Keep the splash screen until settings are read, so the first frame uses the right theme.
    installSplashScreen().setKeepOnScreenCondition { viewModel.settings.value == null }
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val settings by viewModel.settings.collectAsStateWithLifecycle()
      ReminderTheme(settings ?: AppSettings()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) { AppNavigation() }
      }
    }
  }
}

@HiltViewModel
class MainViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {
  /** Null until the settings file has been read once. */
  val settings: StateFlow<AppSettings?> =
    settingsRepository.settings
      .map<AppSettings, AppSettings?> { it }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
