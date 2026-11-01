package com.lentikr.reminder.ui.edit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lentikr.reminder.R
import com.lentikr.reminder.domain.ReminderType
import com.lentikr.reminder.domain.Repeat
import com.lentikr.reminder.ui.common.ShapedIcon
import com.lentikr.reminder.ui.common.TooltipIconButton
import com.lentikr.reminder.ui.common.formatLong
import com.lentikr.reminder.ui.common.label
import com.lentikr.reminder.ui.common.labelRes
import com.lentikr.reminder.ui.common.lunarFullName
import com.lentikr.reminder.ui.home.icon
import com.lentikr.reminder.ui.theme.colors

private enum class EditSheet { NONE, SOLAR_DATE, LUNAR_DATE, REPEAT }

@Composable
fun EditScreen(viewModel: EditViewModel, onClose: () -> Unit, onDeleted: () -> Unit, modifier: Modifier = Modifier) {
  val draft = viewModel.draft
  val suggestions by viewModel.categorySuggestions.collectAsStateWithLifecycle()
  var sheet by rememberSaveable { mutableStateOf(EditSheet.NONE) }
  var confirmDelete by rememberSaveable { mutableStateOf(false) }
  val currentOnClose by rememberUpdatedState(onClose)
  val currentOnDeleted by rememberUpdatedState(onDeleted)

  LaunchedEffect(viewModel) {
    viewModel.eventFlow.collect { event ->
      when (event) {
        EditEvent.Saved -> currentOnClose()
        EditEvent.Deleted -> currentOnDeleted()
      }
    }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(if (viewModel.isNew) R.string.edit_title_new else R.string.edit_title_edit)) },
        navigationIcon = { TooltipIconButton(R.drawable.ic_close, stringResource(R.string.action_close), onClose) },
        actions = {
          Button(
            onClick = viewModel::save,
            enabled = draft.loaded,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.padding(end = 8.dp),
          ) {
            Text(stringResource(R.string.action_save))
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      Modifier.fillMaxSize()
        .padding(innerPadding)
        .consumeWindowInsets(innerPadding)
        .fitInside(WindowInsetsRulers.Ime.current)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
        TypeSelector(draft.type, viewModel::setType)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
          state = viewModel.title,
          label = { Text(stringResource(R.string.edit_field_title)) },
          isError = viewModel.titleError,
          supportingText = if (viewModel.titleError) ({ Text(stringResource(R.string.edit_field_title_error)) }) else null,
          lineLimits = TextFieldLineLimits.SingleLine,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
          state = viewModel.category,
          label = { Text(stringResource(R.string.edit_field_category)) },
          leadingIcon = { Icon(painterResource(R.drawable.ic_sell), contentDescription = null) },
          lineLimits = TextFieldLineLimits.SingleLine,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          modifier = Modifier.fillMaxWidth(),
        )
        if (suggestions.isNotEmpty()) {
          val current = viewModel.category.text.toString().trim()
          Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            for (name in suggestions) {
              FilterChip(
                selected = name == current,
                onClick = { viewModel.pickCategory(if (name == current) "" else name) },
                label = { Text(name) },
              )
            }
          }
        }
        Spacer(Modifier.height(20.dp))

        val typeColors = draft.type.colors()
        val rows = buildList {
          add(EditRow.Date)
          add(EditRow.Lunar)
          if (draft.type == ReminderType.COUNTDOWN) add(EditRow.Repeat)
          add(EditRow.Pinned)
        }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          rows.forEachIndexed { index, row ->
            val shapes = ListItemDefaults.segmentedShapes(index = index, count = rows.size)
            val colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            val leading: @Composable () -> Unit = {
              ShapedIcon(row.icon, MaterialShapes.Circle, typeColors.container, typeColors.onContainer)
            }
            when (row) {
              EditRow.Date ->
                SegmentedListItem(
                  onClick = { sheet = if (draft.isLunar) EditSheet.LUNAR_DATE else EditSheet.SOLAR_DATE },
                  shapes = shapes,
                  colors = colors,
                  leadingContent = leading,
                  supportingContent = {
                    Text(if (draft.isLunar) "${draft.date.lunarFullName()} · ${draft.date.formatLong()}" else draft.date.formatLong())
                  },
                  trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), null) },
                ) {
                  Text(stringResource(R.string.edit_date))
                }
              EditRow.Lunar ->
                SegmentedListItem(
                  checked = draft.isLunar,
                  onCheckedChange = viewModel::setLunar,
                  shapes = shapes,
                  colors = colors,
                  leadingContent = leading,
                  supportingContent = {
                    Text(if (draft.isLunar) draft.date.lunarFullName() else stringResource(R.string.edit_lunar_off))
                  },
                  trailingContent = { Switch(checked = draft.isLunar, onCheckedChange = null) },
                ) {
                  Text(stringResource(R.string.edit_lunar))
                }
              EditRow.Repeat ->
                SegmentedListItem(
                  onClick = { sheet = EditSheet.REPEAT },
                  shapes = shapes,
                  colors = colors,
                  leadingContent = leading,
                  supportingContent = {
                    Text(draft.repeat?.let { Repeat(it.interval, it.unit) }.label(draft.isLunar))
                  },
                  trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), null) },
                ) {
                  Text(stringResource(R.string.edit_repeat))
                }
              EditRow.Pinned ->
                SegmentedListItem(
                  checked = draft.isPinned,
                  onCheckedChange = viewModel::setPinned,
                  shapes = shapes,
                  colors = colors,
                  leadingContent = leading,
                  supportingContent = { Text(stringResource(R.string.edit_pinned_body)) },
                  trailingContent = { Switch(checked = draft.isPinned, onCheckedChange = null) },
                ) {
                  Text(stringResource(R.string.edit_pinned))
                }
            }
          }
        }

        if (!viewModel.isNew) {
          Spacer(Modifier.height(32.dp))
          OutlinedButton(
            onClick = { confirmDelete = true },
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
          ) {
            Icon(painterResource(R.drawable.ic_delete), null, Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.edit_delete), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
          }
        }
        Spacer(Modifier.height(24.dp))
      }
    }
  }

  when (sheet) {
    EditSheet.NONE -> Unit
    EditSheet.SOLAR_DATE ->
      SolarDatePickerDialog(draft.date, onDismiss = { sheet = EditSheet.NONE }) {
        viewModel.setDate(it)
        sheet = EditSheet.NONE
      }
    EditSheet.LUNAR_DATE ->
      LunarDatePickerSheet(draft.date, onDismiss = { sheet = EditSheet.NONE }) {
        viewModel.setDate(it)
        sheet = EditSheet.NONE
      }
    EditSheet.REPEAT ->
      RepeatSheet(
        initial = draft.repeat?.let { Repeat(it.interval, it.unit) },
        lunar = draft.isLunar,
        onDismiss = { sheet = EditSheet.NONE },
      ) {
        viewModel.setRepeat(it)
        sheet = EditSheet.NONE
      }
  }

  if (confirmDelete) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
      title = { Text(stringResource(R.string.detail_delete_title, viewModel.title.text.toString())) },
      text = { Text(stringResource(R.string.detail_delete_body)) },
      confirmButton = {
        TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text(stringResource(R.string.action_delete)) }
      },
      dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
    )
  }
}

private enum class EditRow(@DrawableRes val icon: Int) {
  Date(R.drawable.ic_calendar_today),
  Lunar(R.drawable.ic_calendar_month),
  Repeat(R.drawable.ic_repeat),
  Pinned(R.drawable.ic_keep),
}

/** Countdown / count-up as a two-button connected group with large targets. */
@Composable
private fun TypeSelector(selected: ReminderType, onSelect: (ReminderType) -> Unit) {
  val height = ButtonDefaults.MediumContainerHeight
  val types = ReminderType.entries
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
    types.forEachIndexed { index, type ->
      val colors = type.colors()
      ToggleButton(
        checked = type == selected,
        onCheckedChange = { onSelect(type) },
        shapes =
          if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
          else ButtonGroupDefaults.connectedTrailingButtonShapes(),
        colors =
          ToggleButtonDefaults.colors(
            checkedContainerColor = colors.accent,
            checkedContentColor = colors.onAccent,
          ),
        modifier = Modifier.weight(1f).height(height).semantics { role = Role.RadioButton },
      ) {
        Icon(painterResource(type.icon), null, Modifier.size(ButtonDefaults.iconSizeFor(height)))
        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
        Text(stringResource(type.labelRes), style = ButtonDefaults.textStyleFor(height))
      }
    }
  }
}
