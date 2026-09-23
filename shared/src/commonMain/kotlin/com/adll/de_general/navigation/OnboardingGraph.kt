package com.adll.de_general.navigation

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
import com.adll.de_general.di.AppContainer
import com.adll.de_general.feature.onboarding.ui.InstallScreen
import com.adll.de_general.feature.onboarding.ui.OnboardingViewModel
import com.adll.de_general.feature.onboarding.ui.WelcomeScreen

/**
 * The setup flow: device check, then model install.
 *
 * Screens stay navigation-agnostic — they take callbacks and never see the [NavController]. That is
 * what keeps them previewable and testable, so resist handing one a nav controller later.
 */
fun NavGraphBuilder.onboardingGraph(
    navController: NavController,
    container: AppContainer,
    startDestination: Any,
) {
    navigation<OnboardingGraph>(startDestination = startDestination) {
        composable<Welcome> { backStackEntry ->
            val viewModel = onboardingViewModel(backStackEntry, navController, container)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            WelcomeScreen(
                state = state,
                onContinue = {
                    viewModel.prepareInstall()
                    navController.navigate(Install)
                },
                onSkip = { navController.finishOnboarding() },
            )
        }

        composable<Install> { backStackEntry ->
            val viewModel = onboardingViewModel(backStackEntry, navController, container)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            InstallScreen(
                state = state,
                onStartOrResume = viewModel::startOrResumeDownload,
                onPause = viewModel::pauseDownload,
                onRetry = viewModel::retry,
                onKeepScreenAwakeChange = viewModel::setKeepScreenAwake,
                onFinish = { navController.finishOnboarding() },
            )
        }
    }
}

/**
 * Leaves onboarding for good.
 *
 * `popUpTo` with `inclusive` drops the whole onboarding graph, so back from the journal exits the
 * app rather than walking the user back into setup they have already completed.
 */
private fun NavController.finishOnboarding() {
    navigate(MainGraph) {
        popUpTo<OnboardingGraph> { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * The [OnboardingViewModel], scoped to the onboarding graph rather than to either screen.
 *
 * Welcome and Install share one view model: the compatibility report gathered on the first is what
 * the second reports in its milestones, and the running download must survive moving between them.
 * Scoping to the graph entry is what makes back from Install keep the transfer alive — a
 * destination-scoped view model would be torn down and the download cancelled with it.
 */
@Composable
private fun onboardingViewModel(
    backStackEntry: NavBackStackEntry,
    navController: NavController,
    container: AppContainer,
): OnboardingViewModel {
    // Keyed on the destination's own entry, not navController.currentBackStackEntry: the latter is
    // a plain property read rather than observable state, so it would silently go stale.
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry<OnboardingGraph>()
    }
    return viewModel(parentEntry) {
        OnboardingViewModel(container.deviceProbe, container.modelInstaller)
    }
}
