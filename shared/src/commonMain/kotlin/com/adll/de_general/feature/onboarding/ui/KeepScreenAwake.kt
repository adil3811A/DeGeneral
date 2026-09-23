package com.adll.de_general.feature.onboarding.ui

import androidx.compose.runtime.Composable

/**
 * Holds the screen on while [enabled] is true, and releases it when this leaves composition.
 *
 * The download only makes progress while the app is foregrounded, so letting the device sleep
 * halfway through a 769 MB transfer is a quietly miserable experience. Tied to composition rather
 * than a manual acquire/release so the flag cannot leak if the screen goes away unexpectedly.
 */
@Composable
expect fun KeepScreenAwake(enabled: Boolean)
