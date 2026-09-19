package com.example.de_general.navigation

import com.example.de_general.ai.InstallState
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

/** Everything after onboarding. Bottom navigation will hang off this. */
@Serializable
data object MainGraph

@Serializable
data object Journal

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
