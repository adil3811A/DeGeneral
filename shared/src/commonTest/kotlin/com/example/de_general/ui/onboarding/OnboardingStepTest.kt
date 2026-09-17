package com.example.de_general.ui.onboarding

import com.example.de_general.ai.DownloadProgress
import com.example.de_general.ai.InstallFailure
import com.example.de_general.ai.InstallState
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingStepTest {

    @Test
    fun aFreshInstallStartsAtTheDeviceCheck() {
        assertEquals(OnboardingStep.Welcome, initialStep(InstallState.NotInstalled))
    }

    @Test
    fun anInstalledModelSkipsOnboardingEntirely() {
        assertEquals(OnboardingStep.Done, initialStep(InstallState.Installed))
    }

    @Test
    fun ahalfFinishedDownloadReopensOnTheResumeButton() {
        val paused = InstallState.Paused(DownloadProgress(400_000_000L, 806_058_240L, 0.0))
        assertEquals(OnboardingStep.Install, initialStep(paused))
    }

    @Test
    fun aPreviousFailureStartsOverRatherThanStrandingTheUser() {
        val failed = InstallState.Failed(InstallFailure.Network, "offline")
        assertEquals(OnboardingStep.Welcome, initialStep(failed))
    }
}
