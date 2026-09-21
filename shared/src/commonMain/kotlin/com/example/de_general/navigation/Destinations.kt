package com.example.de_general.navigation

import com.example.de_general.feature.onboarding.domain.InstallState
import kotlinx.serialization.Serializable

/**
 * Every place the app can be.
 *
 * Type-safe routes: each destination is a `@Serializable` object, so `navigate(Install)` is checked
 * by the compiler and a typo cannot become a runtime crash the way a string route can. When a
 * destination needs arguments it becomes a `data class` with fields, and nothing else changes.
 *
 * Graph markers ([OnboardingGraph], [MainGraph]) are destinations too, which is what lets
 * `popUpTo<OnboardingGraph>` clear a whole flow in one move.
 */

/** The setup flow. Unreachable once onboarding is finished. */
@Serializable
data object OnboardingGraph

@Serializable
data object Welcome

@Serializable
data object Install

/** Everything after onboarding. The bottom bar switches between this graph's children. */
@Serializable
data object MainGraph

/** The on-device companion. Not built yet — see `feature/chat/ui/ChatScreen.kt`. */
@Serializable
data object Chat

@Serializable
data object Journal

/**
 * The full-screen composer behind the pencil button.
 *
 * A **flat sibling** of the three tabs inside [MainGraph], not a child of [Journal], and that is
 * load-bearing: [selectedTab] walks a destination's hierarchy, so nested under [Journal] it would
 * find [Journal] and leave the bottom bar up over a full-screen editor. Flat, the walk finds
 * `[CreateJournal, MainGraph]`, matches no tab, returns null, and the bar fades itself out.
 */
@Serializable
data object CreateJournal

/** Not built yet — see `feature/settings/ui/SettingsScreen.kt`. */
@Serializable
data object Settings

/**
 * Which graph the app opens on.
 *
 * Onboarding state is never stored — it is derived from what is on disk, so deleting the model
 * sends the user back through setup and there is no flag that can disagree with reality.
 */
internal fun startGraph(install: InstallState): Any =
    if (install == InstallState.Installed) MainGraph else OnboardingGraph

/**
 * Where inside onboarding to open.
 *
 * A half-finished download should land on the screen with the resume button rather than making the
 * user walk through the device check again.
 */
internal fun onboardingStart(install: InstallState): Any =
    if (install is InstallState.Paused) Install else Welcome
