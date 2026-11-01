package com.lentikr.reminder.ui.detail

import android.content.ClipData
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lentikr.reminder.R
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.lentikr.reminder.data.settings.PosterStyle
import com.lentikr.reminder.ui.common.calendarColors
import com.lentikr.reminder.domain.DayCounter
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.ui.common.EmptyState
import com.lentikr.reminder.ui.common.ShapedIcon
import com.lentikr.reminder.ui.common.TooltipIconButton
import com.lentikr.reminder.ui.common.formatLong
import com.lentikr.reminder.ui.common.label
import com.lentikr.reminder.ui.common.lunarFullName
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
  viewModel: DetailViewModel,
  showBack: Boolean,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val resources = LocalResources.current
  val currentOnBack by rememberUpdatedState(onBack)
  var confirmDelete by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(viewModel) {
    viewModel.eventFlow.collect { event ->
      when (event) {
        is DetailEvent.Share -> {
          val send =
            Intent(Intent.ACTION_SEND).apply {
              type = "image/png"
              putExtra(Intent.EXTRA_STREAM, event.uri)
              clipData = ClipData.newRawUri(null, event.uri)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
          context.startActivity(Intent.createChooser(send, resources.getString(R.string.detail_share_chooser)))
        }
        DetailEvent.Saved -> snackbarHostState.showSnackbar(resources.getString(R.string.detail_saved))
        DetailEvent.ExportFailed -> snackbarHostState.showSnackbar(resources.getString(R.string.detail_export_failed))
        DetailEvent.Deleted -> currentOnBack()
      }
    }
  }

  // The reminder was deleted elsewhere (for example from the list in a two-pane layout).
  if (state is DetailUiState.Missing) LaunchedEffect(Unit) { currentOnBack() }

  val posterStyle by viewModel.posterStyle.collectAsStateWithLifecycle()
  val loaded = state as? DetailUiState.Loaded
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {},
        navigationIcon = {
          if (showBack) TooltipIconButton(R.drawable.ic_arrow_back, stringResource(R.string.action_back), onBack)
        },
        actions = {
          if (loaded != null) {
            TooltipIconButton(R.drawable.ic_edit, stringResource(R.string.action_edit), onEdit)
            TooltipIconButton(R.drawable.ic_delete, stringResource(R.string.action_delete), { confirmDelete = true })
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    when (val s = state) {
      DetailUiState.Loading ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { LoadingIndicator() }
      DetailUiState.Missing ->
        EmptyState(
          icon = R.drawable.ic_event,
          title = stringResource(R.string.detail_not_found),
          body = "",
          modifier = Modifier.padding(innerPadding),
        )
      is DetailUiState.Loaded ->
        DetailBody(
          state = s,
          posterStyle = posterStyle,
          onStyleChange = viewModel::setPosterStyle,
          onShare = viewModel::share,
          onSave = { viewModel.saveImage(it, s.reminder.title) },
          modifier = Modifier.padding(innerPadding),
        )
    }
  }

  if (confirmDelete && loaded != null) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
      title = { Text(stringResource(R.string.detail_delete_title, loaded.reminder.title)) },
      text = { Text(stringResource(R.string.detail_delete_body)) },
      confirmButton = {
        TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text(stringResource(R.string.action_delete)) }
      },
      dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
    )
  }
}

@Composable
private fun DetailBody(
  state: DetailUiState.Loaded,
  posterStyle: PosterStyle,
  onStyleChange: (PosterStyle) -> Unit,
  onShare: (android.graphics.Bitmap) -> Unit,
  onSave: (android.graphics.Bitmap) -> Unit,
  modifier: Modifier = Modifier,
) {
  val reminder = state.reminder
  val scope = rememberCoroutineScope()
  val exportLayer = rememberGraphicsLayer()
  val capture: ((android.graphics.Bitmap) -> Unit) -> Unit = { action ->
    scope.launch { action(exportLayer.toImageBitmap().asAndroidBitmap()) }
  }
  val poster: @Composable () -> Unit = { SharePoster(reminder, state.count, posterStyle) }

  // Renders the poster off screen at export resolution; never visible.
  PosterExportHost(exportLayer, poster)

  Column(
    modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
      PosterPreview(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(32.dp)),
        poster,
      )

      Spacer(Modifier.height(12.dp))
      PosterStyleSelector(posterStyle, onStyleChange, Modifier.padding(horizontal = 16.dp))
      Spacer(Modifier.height(12.dp))
      ExportButtons(onShare = { capture(onShare) }, onSave = { capture(onSave) }, modifier = Modifier.padding(horizontal = 16.dp))
      Spacer(Modifier.height(24.dp))

      val rows = buildList {
        add(InfoRow(R.drawable.ic_calendar_today, R.string.detail_start.takeIf { reminder.type == ReminderType.COUNT_UP } ?: R.string.detail_date, reminder.date.formatLong()))
        if (reminder.isLunar) add(InfoRow(R.drawable.ic_calendar_month, R.string.detail_lunar_date, reminder.date.lunarFullName()))
        if (reminder.type == ReminderType.COUNTDOWN && reminder.repeat != null) {
          DayCounter.nextOccurrence(reminder, state.today)?.let { next ->
            val text = if (reminder.isLunar) "${next.formatLong()} · ${next.lunarFullName()}" else next.formatLong()
            add(InfoRow(R.drawable.ic_event_repeat, R.string.detail_next, text))
          }
        }
        if (reminder.type == ReminderType.COUNTDOWN) add(InfoRow(R.drawable.ic_repeat, R.string.detail_repeat, null))
        if (reminder.category.isNotEmpty()) add(InfoRow(R.drawable.ic_sell, R.string.detail_category, reminder.category))
      }
      val typeColors = calendarColors(reminder.type, inactive = false)
      Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        rows.forEachIndexed { index, row ->
          SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(index = index, count = rows.size),
            colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            leadingContent = {
              ShapedIcon(row.icon, MaterialShapes.Circle, typeColors.header, typeColors.onHeader)
            },
            supportingContent = { Text(row.value ?: reminder.repeat.label(reminder.isLunar)) },
          ) {
            Text(stringResource(row.label), style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }
  }
}

private data class InfoRow(@DrawableRes val icon: Int, val label: Int, val value: String?)

/** Share and save as a button group, so the pressed button widens per M3 Expressive. */
@Composable
private fun ExportButtons(onShare: () -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
  val height = ButtonDefaults.MediumContainerHeight
  val saveSource = remember { MutableInteractionSource() }
  val shareSource = remember { MutableInteractionSource() }
  val saveLabel = stringResource(R.string.detail_save_image)
  val shareLabel = stringResource(R.string.detail_share)
  ButtonGroup(
    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(menuState = it) },
    modifier = modifier.fillMaxWidth(),
  ) {
    customItem(
      buttonGroupContent = {
        FilledTonalButton(
          onClick = onSave,
          shapes = ButtonDefaults.shapesFor(height),
          contentPadding = ButtonDefaults.contentPaddingFor(height),
          interactionSource = saveSource,
          modifier = Modifier.weight(1f).heightIn(min = height).animateWidth(saveSource),
        ) {
          Icon(painterResource(R.drawable.ic_download), null, Modifier.size(ButtonDefaults.iconSizeFor(height)))
          Spacer(Modifier.size(8.dp))
          Text(saveLabel, style = ButtonDefaults.textStyleFor(height), maxLines = 1)
        }
      },
      menuContent = { DropdownMenuItem(text = { Text(saveLabel) }, onClick = onSave) },
    )
    customItem(
      buttonGroupContent = {
        Button(
          onClick = onShare,
          shapes = ButtonDefaults.shapesFor(height),
          contentPadding = ButtonDefaults.contentPaddingFor(height),
          interactionSource = shareSource,
          modifier = Modifier.weight(1f).heightIn(min = height).animateWidth(shareSource),
        ) {
          Icon(painterResource(R.drawable.ic_share), null, Modifier.size(ButtonDefaults.iconSizeFor(height)))
          Spacer(Modifier.size(8.dp))
          Text(shareLabel, style = ButtonDefaults.textStyleFor(height), maxLines = 1)
        }
      },
      menuContent = { DropdownMenuItem(text = { Text(shareLabel) }, onClick = onShare) },
    )
  }
}

/** Wood, garden or night background for the poster, as a connected button group. */
@Composable
private fun PosterStyleSelector(selected: PosterStyle, onSelect: (PosterStyle) -> Unit, modifier: Modifier = Modifier) {
  val styles = PosterStyle.entries
  Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
    styles.forEachIndexed { index, style ->
      ToggleButton(
        checked = style == selected,
        onCheckedChange = { onSelect(style) },
        shapes =
          when (index) {
            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
            styles.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
          },
        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
      ) {
        Text(
          stringResource(
            when (style) {
              PosterStyle.WOOD -> R.string.poster_style_wood
              PosterStyle.BLOOM -> R.string.poster_style_bloom
              PosterStyle.NIGHT -> R.string.poster_style_night
            }
          )
        )
      }
    }
  }
}

/** Shown in the detail pane of a two-pane layout until a reminder is picked. */
@Composable
fun DetailPlaceholder(modifier: Modifier = Modifier) {
  Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    EmptyState(
      icon = R.drawable.ic_celebration,
      title = stringResource(R.string.home_pick_one),
      body = "",
      shape = MaterialShapes.Flower,
    )
  }
}
