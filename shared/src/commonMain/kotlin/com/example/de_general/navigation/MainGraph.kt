package com.example.de_general.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.de_general.di.AppContainer
import com.example.de_general.feature.chat.ui.ChatScreen
import com.example.de_general.feature.chat.ui.ChatViewModel
import com.example.de_general.feature.journal.ui.CreateJournalScreen
import com.example.de_general.feature.journal.ui.CreateJournalViewModel
import com.example.de_general.feature.journal.ui.JournalScreen
import com.example.de_general.feature.journal.ui.JournalViewModel
import com.example.de_general.feature.onboarding.domain.GemmaThreeOneB
import com.example.de_general.feature.settings.ui.SettingsScreen
import com.example.de_general.feature.settings.ui.SettingsViewModel

/**
 * Everything after setup.
 *
 * Three tabs switched between by `MainNavBar`, plus [CreateJournal] as a fourth flat sibling that
 * is not a tab — it is pushed onto the stack, and the bar hides itself because no tab matches it.
 * The bar itself is not built here: it sits beside the `NavHost` in [AppNavHost] so that a tab
 * switch does not drag it through the host's slide-and-fade transition.
 */
fun NavGraphBuilder.mainGraph(navController: NavController, container: AppContainer) {
    navigation<MainGraph>(startDestination = Journal) {
        composable<Chat> { backStackEntry ->
            val viewModel = chatViewModel(backStackEntry, navController, container)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // The weights are ~770 MB, so they are only resident while this tab is on screen.
            // Both calls go through the view model rather than a scope remembered here: this
            // composable's scope is cancelled the instant it leaves, which would abandon the
            // unload half-done. The view model is scoped to the whole main graph and is still
            // alive afterwards, so it is the thing that can finish the job.
            DisposableEffect(viewModel) {
                viewModel.loadEngine()
                onDispose { viewModel.unloadEngine() }
            }

            ChatScreen(
                state = state,
                onDraftChange = viewModel::onDraftChange,
                onSend = viewModel::send,
                onAttach = viewModel::onAttach,
                onReflectDeeper = viewModel::reflectDeeper,
                onSaveInsight = viewModel::saveInsight,
                onDismissNotice = viewModel::dismissNotice,
            )
        }

        composable<Journal> { backStackEntry ->
            val viewModel = journalViewModel(backStackEntry, navController, container)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            JournalScreen(
                state = state,
                onDelete = viewModel::deleteEntry,
                onDismissError = viewModel::dismissError,
            )
        }

        composable<CreateJournal> { backStackEntry ->
            val viewModel = createJournalViewModel(backStackEntry, navController, container)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Keyed on this entry's id, not an onDispose, and the difference is data loss:
            // `DisposableEffect`'s onDispose also fires on **rotation**. For Chat's engine that is
            // merely wasteful; for a half-written entry it would silently throw the entry away.
            // `remember(key)` runs this exactly once per entry, and `startComposing` is a no-op
            // when asked for the id it already holds.
            remember(backStackEntry.id) { viewModel.startComposing(backStackEntry.id) }

            // The screen has no NavController, so it closes itself through state. acknowledgeClose
            // clears only that flag — the draft stays put so the 320 ms exit animation renders the
            // entry rather than a composer visibly emptying itself.
            LaunchedEffect(state.finished) {
                if (state.finished) {
                    navController.popBackStack()
                    viewModel.acknowledgeClose()
                }
            }

            CreateJournalScreen(
                state = state,
                onTitleChange = viewModel::onTitleChange,
                onBodyChange = viewModel::onBodyChange,
                onClearBody = viewModel::clearBody,
                onMoodChange = viewModel::onMoodChange,
                onAnchorChange = viewModel::onAnchorChange,
                onOpenDatePicker = viewModel::openDatePicker,
                onDismissDatePicker = viewModel::dismissDatePicker,
                onDatePicked = viewModel::onDatePicked,
                onTagDraftChange = viewModel::onTagDraftChange,
                onCommitTag = viewModel::commitTagDraft,
                onRemoveTag = viewModel::removeTag,
                onRefine = viewModel::refine,
                onAcceptPolish = viewModel::acceptPolish,
                onDiscardPolish = viewModel::discardPolish,
                onUndoPolish = viewModel::undoPolish,
                onSave = viewModel::save,
                onClose = viewModel::close,
                onCancelDiscard = viewModel::cancelDiscard,
                onConfirmDiscard = viewModel::confirmDiscard,
                onDismissError = viewModel::dismissError,
            )
        }

        composable<Settings> { backStackEntry ->
            val viewModel = settingsViewModel(backStackEntry, navController, container)
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = viewModel::setThemeMode,
            )
        }
    }
}

/**
 * One journal view model, scoped to the whole main graph.
 *
 * Graph-scoped rather than destination-scoped for the same reason onboarding is: it outlives a trip
 * to a sibling tab, so a delete in flight finishes rather than being cancelled on the way out.
 */
@Composable
private fun journalViewModel(
    backStackEntry: NavBackStackEntry,
    navController: NavController,
    container: AppContainer,
): JournalViewModel {
    // Keyed on the destination's own entry, not navController.currentBackStackEntry: the latter is
    // a plain property read rather than observable state, so it would silently go stale.
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry<MainGraph>()
    }
    return viewModel(parentEntry) {
        JournalViewModel(container.journalRepository, container.now)
    }
}

/**
 * One chat view model, scoped to the whole main graph, for the same reason the journal's is: a
 * half-typed message should survive a look at another tab.
 *
 * The model label is derived here from the pinned spec rather than hard-coded in the screen, so
 * the chip cannot drift from what `ModelSpec.kt` actually pins.
 */
@Composable
private fun chatViewModel(
    backStackEntry: NavBackStackEntry,
    navController: NavController,
    container: AppContainer,
): ChatViewModel {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry<MainGraph>()
    }
    return viewModel(parentEntry) {
        ChatViewModel(
            repository = container.chatRepository,
            engine = container.llmEngine,
            modelPath = container.modelInstaller.modelPath,
            modelLabel = "${GemmaThreeOneB.displayName} · ${GemmaThreeOneB.quantization}",
        )
    }
}

/**
 * One composer view model, scoped to the whole main graph.
 *
 * Graph-scoped rather than destination-scoped for the same reason `ChatViewModel` is: it has to
 * outlive its own destination. `save()` and the polish job both run on `viewModelScope`, and a
 * destination-scoped scope is cancelled the instant the screen pops — which is exactly when the
 * save is still in flight.
 *
 * The draft is *not* kept alive by that scoping. `startComposing(backStackEntry.id)` resets it for
 * each new push, so closing the composer and opening it again gives a blank page.
 *
 * The model label is derived here from the pinned spec, exactly as the chat chip's is, so it cannot
 * drift from what `ModelSpec.kt` actually pins. `now` comes from the container so a backdated entry
 * is stamped by the same clock the repository would have used.
 */
@Composable
private fun createJournalViewModel(
    backStackEntry: NavBackStackEntry,
    navController: NavController,
    container: AppContainer,
): CreateJournalViewModel {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry<MainGraph>()
    }
    return viewModel(parentEntry) {
        CreateJournalViewModel(
            repository = container.journalRepository,
            engine = container.llmEngine,
            modelPath = container.modelInstaller.modelPath,
            modelLabel = "${GemmaThreeOneB.displayName} · ${GemmaThreeOneB.quantization}",
            now = container.now,
        )
    }
}

/**
 * The settings view model, scoped to the whole main graph like its siblings, so a preference write
 * that is still on disk when the user taps away is not cancelled on the way out.
 */
@Composable
private fun settingsViewModel(
    backStackEntry: NavBackStackEntry,
    navController: NavController,
    container: AppContainer,
): SettingsViewModel {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry<MainGraph>()
    }
    return viewModel(parentEntry) {
        SettingsViewModel(container.themePreferences)
    }
}
