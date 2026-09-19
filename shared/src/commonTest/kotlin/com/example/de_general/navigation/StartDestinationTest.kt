package com.example.de_general.navigation

import com.example.de_general.ai.DownloadProgress
import com.example.de_general.ai.InstallFailure
import com.example.de_general.ai.InstallState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where the app opens is derived from what is on disk, never stored. These pin that mapping down,
 * because getting it wrong means either re-running setup someone already finished or dropping them
 * into a journal with no model behind it.
 */
class StartDestinationTest {

    @Test
    fun aFreshInstallOpensOnboardingAtTheDeviceCheck() {
        assertEquals(OnboardingGraph, startGraph(InstallState.NotInstalled))
        assertEquals(Welcome, onboardingStart(InstallState.NotInstalled))
    }

    @Test
    fun anInstalledModelSkipsOnboardingEntirely() {
        assertEquals(MainGraph, startGraph(InstallState.Installed))
    }

    @Test
    fun aHalfFinishedDownloadReopensOnTheResumeButton() {
        val paused = InstallState.Paused(DownloadProgress(400_000_000L, 806_058_240L, 0.0))

        assertEquals(OnboardingGraph, startGraph(paused))
        assertEquals(Install, onboardingStart(paused))
    }

    @Test
    fun aPreviousFailureStartsOverRatherThanStrandingTheUser() {
        val failed = InstallState.Failed(InstallFailure.Network, "offline")

        assertEquals(OnboardingGraph, startGraph(failed))
        assertEquals(Welcome, onboardingStart(failed))
    }
}
