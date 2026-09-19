package com.example.de_general.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.de_general.ui.journal.JournalScreen

/**
 * Everything after setup.
 *
 * One destination for now. Insights and Settings — and the bottom bar that switches between them —
 * are added here as siblings of [Journal] without disturbing anything above.
 */
fun NavGraphBuilder.mainGraph() {
    navigation<MainGraph>(startDestination = Journal) {
        composable<Journal> { JournalScreen() }
    }
}
