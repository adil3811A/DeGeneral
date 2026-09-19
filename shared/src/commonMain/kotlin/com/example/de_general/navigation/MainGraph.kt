package com.example.de_general.navigation

import androidx.compose.runtime.Composable
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
import com.example.de_general.feature.journal.ui.JournalScreen
import com.example.de_general.feature.journal.ui.JournalViewModel
import com.example.de_general.feature.onboarding.domain.GemmaThreeOneB
import com.example.de_general.feature.settings.ui.SettingsScreen

/**
 * Everything after setup.
 *
 * Three flat siblings, switched between by `MainNavBar`. The bar itself is not built here: it
 * sits beside the `NavHost` in [AppNavHost] so that a tab switch does not drag it through the
 * host's slide-and-fade transition.
 */
fun NavGraphBuilder.mainGraph(navController: NavController, container: AppContainer) {
    navigation<MainGraph>(startDestination = Journal) {
        composable<Chat> { backStackEntry ->
            val viewModel = chatViewModel(backStackEntry, navController, container)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
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
                onDraftChange = viewModel::onDraftChange,
                onSave = viewModel::saveDraft,
                onDelete = viewModel::deleteEntry,
                onDismissError = viewModel::dismissError,
            )
        }

        composable<Settings> { SettingsScreen() }
    }
}

/**
 * One journal view model, scoped to the whole main graph.
 *
 * Graph-scoped rather than destination-scoped for the same reason onboarding is: a
 * destination-scoped view model would throw away an unsaved draft every time the user looked at
 * another tab. `MainNavBar` resolves the same instance through the same graph entry, which is how
 * the pencil button reaches the editor it is asking to focus.
 */
@Composable
internal fun journalViewModel(
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
        JournalViewModel(container.journalRepository)
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
            modelLabel = "${GemmaThreeOneB.displayName} · ${GemmaThreeOneB.quantization}",
        )
    }
}
