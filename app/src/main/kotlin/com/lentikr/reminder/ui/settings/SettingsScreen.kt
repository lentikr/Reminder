package com.lentikr.reminder.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lentikr.reminder.BuildConfig
import com.lentikr.reminder.R
import com.lentikr.reminder.data.settings.AppSettings
import com.lentikr.reminder.data.settings.ThemeMode
import com.lentikr.reminder.ui.common.SectionHeader
import com.lentikr.reminder.ui.common.ShapedIcon
import com.lentikr.reminder.ui.common.TooltipIconButton
import com.lentikr.reminder.ui.theme.isDarkTheme
import com.lentikr.reminder.ui.theme.supportsDynamicColor

@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val busy by viewModel.busy.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val resources = LocalResources.current
  val context = LocalContext.current
  var pendingRestore by rememberSaveable { mutableStateOf<String?>(null) }

  // The Storage Access Framework needs no storage permission for either direction.
  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) viewModel.export(uri)
    }
  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) pendingRestore = uri.toString()
    }

  LaunchedEffect(viewModel) {
    viewModel.resultFlow.collect { result ->
      val message =
        when (result) {
          is BackupResult.Exported -> resources.getString(R.string.settings_backup_done, result.count)
          BackupResult.NothingToExport -> resources.getString(R.string.settings_backup_empty)
          is BackupResult.Restored -> resources.getString(R.string.settings_restore_done, result.count)
          is BackupResult.ExportFailed -> resources.getString(R.string.settings_backup_failed, result.reason)
          is BackupResult.RestoreFailed -> resources.getString(R.string.settings_restore_failed, result.reason)
        }
      snackbarHostState.showSnackbar(message)
    }
  }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      MediumFlexibleTopAppBar(
        title = { Text(stringResource(R.string.action_settings)) },
        navigationIcon = { TooltipIconButton(R.drawable.ic_arrow_back, stringResource(R.string.action_back), onBack) },
        scrollBehavior = scrollBehavior,
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    LazyColumn(
      contentPadding =
        PaddingValues(
          top = innerPadding.calculateTopPadding(),
          bottom = innerPadding.calculateBottomPadding() + 24.dp,
          start = 16.dp,
          end = 16.dp,
        ),
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      item(contentType = "header") { Section(stringResource(R.string.settings_section_appearance)) }
      item(contentType = "appearance") { AppearanceGroup(settings, viewModel) }

      item(contentType = "header") { Section(stringResource(R.string.settings_section_data)) }
      item(contentType = "data") {
        Column(Modifier.widthIn(max = 600.dp)) {
          if (busy) LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 8.dp))
          SettingsGroup(
            listOf(
              SettingRow.Action(
                R.drawable.ic_upload_file,
                stringResource(R.string.settings_backup),
                stringResource(R.string.settings_backup_body),
                enabled = !busy,
              ) {
                exportLauncher.launch(viewModel.suggestedBackupName())
              },
              SettingRow.Action(
                R.drawable.ic_restore_page,
                stringResource(R.string.settings_restore),
                stringResource(R.string.settings_restore_body),
                enabled = !busy,
              ) {
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
              },
            )
          )
        }
      }

      item(contentType = "header") { Section(stringResource(R.string.settings_section_about)) }
      item(contentType = "about") {
        val url = stringResource(R.string.settings_source_url)
        SettingsGroup(
          listOf(
            SettingRow.Info(R.drawable.ic_info, stringResource(R.string.settings_version), BuildConfig.VERSION_NAME),
            SettingRow.Action(R.drawable.ic_code, stringResource(R.string.settings_source), url.removePrefix("https://")) {
              try {
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
              } catch (_: ActivityNotFoundException) {
                // No browser installed; nothing useful to do.
              }
            },
          )
        )
      }
    }
  }

  pendingRestore?.let { uri ->
    AlertDialog(
      onDismissRequest = { pendingRestore = null },
      icon = { Icon(painterResource(R.drawable.ic_restore_page), contentDescription = null) },
      title = { Text(stringResource(R.string.settings_restore_title)) },
      text = { Text(stringResource(R.string.settings_restore_message)) },
      confirmButton = {
        TextButton(onClick = { pendingRestore = null; viewModel.import(uri.toUri()) }) {
          Text(stringResource(R.string.settings_restore_action))
        }
      },
      dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.action_cancel)) } },
    )
  }
}

@Composable
private fun Section(text: String) {
  Row(Modifier.widthIn(max = 600.dp).fillMaxWidth()) { SectionHeader(text) }
}

@Composable
private fun AppearanceGroup(settings: AppSettings, viewModel: SettingsViewModel) {
  Column(Modifier.widthIn(max = 600.dp)) {
    ThemeModeSelector(settings.themeMode, viewModel::setThemeMode)
    Spacer(Modifier.height(12.dp))
    val rows = buildList {
      if (settings.supportsDynamicColor) {
        add(
          SettingRow.Toggle(
            R.drawable.ic_palette,
            stringResource(R.string.settings_dynamic_color),
            stringResource(R.string.settings_dynamic_color_body),
            settings.dynamicColor,
            viewModel::setDynamicColor,
          )
        )
      }
      add(
        SettingRow.Toggle(
          R.drawable.ic_contrast,
          stringResource(R.string.settings_pure_black),
          stringResource(R.string.settings_pure_black_body),
          settings.pureBlack,
          viewModel::setPureBlack,
          enabled = settings.isDarkTheme(),
        )
      )
    }
    SettingsGroup(rows)
  }
}

/**
 * System / light / dark as a connected button group. Each button stacks its icon above the label so
 * the label gets the full button width; side by side, "跟随系统" clipped on 360dp phones.
 */
@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
  val labelStyle = MaterialTheme.typography.labelLarge
  val modes = ThemeMode.entries
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
    modes.forEachIndexed { index, mode ->
      val (icon, label) =
        when (mode) {
          ThemeMode.SYSTEM -> R.drawable.ic_brightness_auto to R.string.settings_theme_system
          ThemeMode.LIGHT -> R.drawable.ic_light_mode to R.string.settings_theme_light
          ThemeMode.DARK -> R.drawable.ic_dark_mode to R.string.settings_theme_dark
        }
      ToggleButton(
        checked = mode == selected,
        onCheckedChange = { onSelect(mode) },
        shapes =
          when (index) {
            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
            modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
          },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(painterResource(icon), null, Modifier.size(24.dp))
          Spacer(Modifier.height(4.dp))
          // The sp line height stays fixed while the font steps down, so the buttons keep equal heights.
          Text(
            stringResource(label),
            style = labelStyle,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = labelStyle.fontSize),
          )
        }
      }
    }
  }
}

private sealed interface SettingRow {
  val icon: Int
  val title: String

  data class Toggle(
    @DrawableRes override val icon: Int,
    override val title: String,
    val body: String,
    val checked: Boolean,
    val onChange: (Boolean) -> Unit,
    val enabled: Boolean = true,
  ) : SettingRow

  data class Action(
    @DrawableRes override val icon: Int,
    override val title: String,
    val body: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
  ) : SettingRow

  data class Info(@DrawableRes override val icon: Int, override val title: String, val body: String) : SettingRow
}

/** A grouped list where each row is one segment, as in the M3 Expressive list samples. */
@Composable
private fun SettingsGroup(rows: List<SettingRow>) {
  val scheme = MaterialTheme.colorScheme
  val colors = ListItemDefaults.segmentedColors(containerColor = scheme.surfaceContainer)
  Column(Modifier.widthIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
    rows.forEachIndexed { index, row ->
      val shapes = ListItemDefaults.segmentedShapes(index = index, count = rows.size)
      val leading: @Composable () -> Unit = {
        ShapedIcon(row.icon, MaterialShapes.Circle, scheme.secondaryContainer, scheme.onSecondaryContainer)
      }
      when (row) {
        is SettingRow.Toggle ->
          SegmentedListItem(
            checked = row.checked,
            onCheckedChange = row.onChange,
            enabled = row.enabled,
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            supportingContent = { Text(row.body) },
            trailingContent = { Switch(checked = row.checked, onCheckedChange = null, enabled = row.enabled) },
          ) {
            Text(row.title)
          }
        is SettingRow.Action ->
          SegmentedListItem(
            onClick = row.onClick,
            enabled = row.enabled,
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            supportingContent = { Text(row.body) },
            trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), null) },
          ) {
            Text(row.title)
          }
        is SettingRow.Info ->
          SegmentedListItem(
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            supportingContent = { Text(row.body) },
          ) {
            Text(row.title)
          }
      }
    }
  }
}

