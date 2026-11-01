package com.lentikr.reminder.ui.navigation

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lentikr.reminder.ui.detail.DetailPlaceholder
import com.lentikr.reminder.ui.detail.DetailScreen
import com.lentikr.reminder.ui.detail.DetailViewModel
import com.lentikr.reminder.ui.edit.EditScreen
import com.lentikr.reminder.ui.edit.EditViewModel
import com.lentikr.reminder.ui.home.HomeScreen
import com.lentikr.reminder.ui.settings.SettingsScreen

/**
 * Single back stack. On wide windows the list-detail scene strategy shows Home and Detail side by
 * side; on phones each entry is full screen.
 */
@Composable
fun AppNavigation() {
  val backStack = rememberNavBackStack(Home)

  // No gap between panes; each pane draws its own surface. See b/418201867.
  val windowInfo = currentWindowAdaptiveInfoV2()
  val directive =
    remember(windowInfo) {
      calculatePaneScaffoldDirective(windowInfo).copy(horizontalPartitionSpacerSize = 0.dp)
    }
  // PopLatest: with both panes visible, Back closes the open reminder instead of leaving the app.
  val listDetail =
    rememberListDetailSceneStrategy<NavKey>(
      backNavigationBehavior = BackNavigationBehavior.PopLatest,
      directive = directive,
    )
  val twoPane = directive.maxHorizontalPartitions > 1

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators =
      listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
    sceneStrategies = listOf(listDetail),
    entryProvider =
      entryProvider {
        entry<Home>(metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { DetailPlaceholder() })) {
          HomeScreen(
            onOpenReminder = { id -> backStack.showDetail(id) },
            onCreate = { type -> if (backStack.lastOrNull() !is Edit) backStack.add(Edit(type = type)) },
            onOpenSettings = dropUnlessResumed { backStack.add(Settings) },
          )
        }
        entry<Detail>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
          val viewModel =
            hiltViewModel<DetailViewModel, DetailViewModel.Factory>(creationCallback = { it.create(key.id) })
          DetailScreen(
            viewModel = viewModel,
            // In a two-pane layout the list is visible, so the detail pane has no back arrow.
            showBack = !twoPane,
            onBack = { backStack.remove(key) },
            onEdit = dropUnlessResumed { backStack.add(Edit(id = key.id)) },
          )
        }
        entry<Edit> { key ->
          val viewModel = hiltViewModel<EditViewModel, EditViewModel.Factory>(creationCallback = { it.create(key) })
          EditScreen(
            viewModel = viewModel,
            onClose = { backStack.remove(key) },
            // The reminder is gone, so its detail entry must go too.
            onDeleted = { backStack.removeAll { it == key || (it is Detail && it.id == key.id) } },
          )
        }
        entry<Settings> { SettingsScreen(onBack = { backStack.remove(Settings) }) }
      },
  )
}

/** Opens [id] in the detail pane, replacing any detail already shown instead of stacking them. */
private fun NavBackStack<NavKey>.showDetail(id: Long) {
  if (lastOrNull() == Detail(id)) return
  removeAll { it is Detail }
  add(Detail(id))
}
