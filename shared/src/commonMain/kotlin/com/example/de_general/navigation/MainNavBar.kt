package com.example.de_general.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.de_general.core.ui.components.FloatingNavBar
import com.example.de_general.core.ui.components.FloatingNavBarDefaults
import com.example.de_general.core.ui.components.NavBarItem
import com.example.de_general.core.ui.icons.AutoStories
import com.example.de_general.core.ui.icons.ChatBubble
import com.example.de_general.core.ui.icons.Edit
import com.example.de_general.core.ui.icons.MindfulIcons
import com.example.de_general.core.ui.icons.Settings
import com.example.de_general.core.ui.theme.MindfulTheme

/**
 * The bottom bar, and the only thing in the app that turns a tab into a `navigate` call.
 *
 * It lives beside the `NavHost` rather than inside any destination, so switching tabs does not
 * drag it through the host's slide-and-fade transition. That is also why it takes a
 * [NavController]: this is the navigation layer, where routes are allowed. Screens still never see
 * one — [com.example.de_general.core.ui.components.FloatingNavBar] is handed plain callbacks.
 *
 * Renders nothing outside the main graph, which is what keeps it off the onboarding screens — and
 * nothing on [CreateJournal] either, which is a sibling of the tabs rather than a child of one, so
 * [selectedTab] matches nothing and the bar fades away under the full-screen composer.
 */
@Composable
internal fun MainNavBar(navController: NavController, modifier: Modifier = Modifier) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val tab = selectedTab(backStackEntry?.destination)

    AnimatedVisibility(
        visible = tab != null,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        if (tab != null) {
            MainNavBarContent(navController, tab)
        }
    }
}

@Composable
private fun MainNavBarContent(navController: NavController, selected: MainTab) {
    val spacing = MindfulTheme.spacing

    val items = remember(selected) {
        MainTab.entries.map { tab ->
            NavBarItem(
                icon = tab.icon,
                label = tab.label,
                selected = tab == selected,
                onClick = { navController.switchTab(tab) },
            )
        }
    }

    FloatingNavBar(
        items = items,
        action = NavBarItem(
            icon = MindfulIcons.Edit,
            label = "New entry",
            onClick = {
                // A push, not a switchTab: the composer is a full screen of its own, not a tab,
                // and switchTab would pop back to the graph root with saveState.
                //
                // launchSingleTop is load-bearing. Two taps in quick succession must not stack two
                // composers, because both would resolve the same graph-scoped view model and the
                // second would reset the first one's draft.
                navController.navigate(CreateJournal) { launchSingleTop = true }
            },
        ),
        modifier = Modifier
            // safeDrawing *minus* the keyboard, and the exclusion is the whole point.
            // `safeDrawing.bottom` is `maxOf(displayCutout, ime, systemBars)`, so a plain
            // `safeDrawingPadding()` here lifted the whole bar up with the keyboard — the tabs
            // would ride above the composer every time someone started typing. The bar belongs to
            // the window, not to the text field: it stays put and lets the keyboard cover it.
            .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime))
            .padding(
                horizontal = spacing.margin,
                vertical = FloatingNavBarDefaults.ScreenOffset,
            ),
    )
}

/**
 * Switch tabs without growing the back stack.
 *
 * `saveState`/`restoreState` are what let a half-scrolled journal still be half-scrolled after a
 * trip to Settings; `launchSingleTop` stops a re-tap of the current tab from stacking a duplicate.
 */
private fun NavController.switchTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo<MainGraph> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private val MainTab.icon: ImageVector
    get() = when (this) {
        MainTab.Chat -> MindfulIcons.ChatBubble
        MainTab.Journal -> MindfulIcons.AutoStories
        MainTab.Settings -> MindfulIcons.Settings
    }
