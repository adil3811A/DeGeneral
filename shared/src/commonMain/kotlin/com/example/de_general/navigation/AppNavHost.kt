package com.example.de_general.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.de_general.di.AppContainer

/** How far a screen travels while sliding in or out. A full-width slide is too brisk for this app. */
private const val SLIDE_FRACTION = 6

private const val DURATION_MS = 320

/**
 * The app's navigation host.
 *
 * The back stack is the single source of truth for what is on screen — no view model holds a
 * "current step". Where the app opens is still derived from disk, via [startGraph] and
 * [onboardingStart], both read once before composition because a graph's start destination is
 * fixed when its builder runs.
 */
@Composable
fun AppNavHost(container: AppContainer, modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Read once. ModelInstaller.refresh() runs in its init, so this value is ready synchronously
    // and the app opens on the right screen instead of flashing through Welcome first.
    val installState = remember { container.modelInstaller.state.value }
    val rootStart = remember(installState) { startGraph(installState) }
    val onboardingStart = remember(installState) { onboardingStart(installState) }

    NavHost(
        navController = navController,
        startDestination = rootStart,
        modifier = modifier,
        enterTransition = { slideIn(forward = true) },
        exitTransition = { slideOut(forward = true) },
        popEnterTransition = { slideIn(forward = false) },
        popExitTransition = { slideOut(forward = false) },
    ) {
        onboardingGraph(navController, container, onboardingStart)
        mainGraph(navController, container)
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideIn(
    forward: Boolean,
): EnterTransition {
    val direction = if (forward) 1 else -1
    return slideInHorizontally(tween(DURATION_MS)) { it * direction / SLIDE_FRACTION } +
        fadeIn(tween(DURATION_MS))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOut(
    forward: Boolean,
): ExitTransition {
    val direction = if (forward) 1 else -1
    return slideOutHorizontally(tween(DURATION_MS)) { -it * direction / SLIDE_FRACTION } +
        fadeOut(tween(DURATION_MS))
}
