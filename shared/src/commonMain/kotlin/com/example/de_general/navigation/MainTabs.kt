package com.example.de_general.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

/**
 * The three places the bottom bar can take you, in the order they appear in it.
 *
 * Declaration order *is* the bar's left-to-right order, which is why [Journal] sits in the middle:
 * it is the graph's start destination and the design puts it under the thumb.
 */
internal enum class MainTab(val label: String, val route: Any) {
    // Qualified on purpose: inside the enum body `Chat` is the entry being declared, not the
    // route object of the same name in this package.
    Chat("Chat", com.example.de_general.navigation.Chat),
    Journal("Journal", com.example.de_general.navigation.Journal),
    Settings("Settings", com.example.de_general.navigation.Settings),
}

/**
 * Which tab [destination] belongs to, or `null` if it is not a tab at all — onboarding, for
 * instance, where the bar must not appear.
 *
 * Walks the destination's hierarchy rather than comparing routes, so a tab that later grows nested
 * destinations of its own keeps its tab highlighted instead of silently deselecting.
 */
internal fun selectedTab(destination: NavDestination?): MainTab? {
    if (destination == null) return null
    return MainTab.entries.firstOrNull { tab ->
        destination.hierarchy.any { it.hasRoute(tab.route::class) }
    }
}
