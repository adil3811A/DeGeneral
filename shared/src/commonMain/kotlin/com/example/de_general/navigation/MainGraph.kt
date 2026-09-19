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
import com.example.de_general.feature.journal.ui.JournalScreen
import com.example.de_general.feature.journal.ui.JournalViewModel

/**
 * Everything after setup.
 *
 * One destination for now. Insights and Settings — and the bottom bar that switches between them —
 * are added here as siblings of [Journal] without disturbing anything above.
 */
fun NavGraphBuilder.mainGraph(navController: NavController, container: AppContainer) {
    navigation<MainGraph>(startDestination = Journal) {
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
    }
}

/**
 * One journal view model, scoped to the whole main graph.
 *
 * Graph-scoped rather than destination-scoped for the same reason onboarding is: the siblings that
 * land here next will share this state, and a destination-scoped view model would throw away an
 * unsaved draft every time the user looked at another tab.
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
        JournalViewModel(container.journalRepository)
    }
}
