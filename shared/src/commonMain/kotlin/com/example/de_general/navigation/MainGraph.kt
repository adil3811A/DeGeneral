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
import com.example.de_general.feature.journal.ui.JournalScreen
import com.example.de_general.feature.journal.ui.JournalViewModel
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
        composable<Chat> { ChatScreen() }

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
