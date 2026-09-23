package com.adll.de_general.feature.onboarding.ui

import com.adll.de_general.core.ui.components.MilestoneState
import com.adll.de_general.feature.onboarding.domain.CheckStatus
import com.adll.de_general.feature.onboarding.domain.DownloadProgress
import com.adll.de_general.feature.onboarding.domain.GemmaThreeOneB
import com.adll.de_general.feature.onboarding.domain.InstallFailure
import com.adll.de_general.feature.onboarding.domain.InstallState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The install screen's copy, tested without a composition.
 *
 * One test per `InstallState` arm on purpose: adding a state to the sealed interface should break
 * a test here rather than quietly render a blank string on the screen.
 */
class InstallCopyTest {

    private val halfway = DownloadProgress(
        bytesDownloaded = 500,
        totalBytes = 1000,
        bytesPerSecond = 100.0,
    )

    // --- headline and subhead ------------------------------------------------------------------

    @Test
    fun notInstalledInvitesTheDownload() {
        assertEquals("Install the local AI engine", installHeadline(InstallState.NotInstalled))
        assertTrue(
            installSubhead(InstallState.NotInstalled, GemmaThreeOneB)
                .contains("Nothing is sent anywhere"),
        )
    }

    @Test
    fun downloadingSaysSo() {
        val state = InstallState.Downloading(halfway)
        assertEquals("Downloading the model", installHeadline(state))
        assertEquals(
            "Streaming into this app's private storage.",
            installSubhead(state, GemmaThreeOneB),
        )
    }

    @Test
    fun pausedPromisesTheProgressIsKept() {
        val state = InstallState.Paused(halfway)
        assertEquals("Download paused", installHeadline(state))
        assertTrue(installSubhead(state, GemmaThreeOneB).contains("progress is kept"))
    }

    @Test
    fun verifyingNamesTheDigest() {
        assertEquals("Verifying the download", installHeadline(InstallState.Verifying))
        assertTrue(installSubhead(InstallState.Verifying, GemmaThreeOneB).contains("SHA-256"))
    }

    @Test
    fun installedIsReady() {
        assertEquals("Ready to write", installHeadline(InstallState.Installed))
        assertEquals(
            "The model is installed and verified.",
            installSubhead(InstallState.Installed, GemmaThreeOneB),
        )
    }

    @Test
    fun failedSaysNothingWasInstalled() {
        val state = InstallState.Failed(InstallFailure.Network, "No connection")
        assertEquals("Download stopped", installHeadline(state))
        assertTrue(installSubhead(state, GemmaThreeOneB).contains("Nothing was installed"))
    }

    // --- progressOrNull ------------------------------------------------------------------------

    @Test
    fun onlyDownloadingAndPausedCarryProgress() {
        assertEquals(halfway, InstallState.Downloading(halfway).progressOrNull())
        assertEquals(halfway, InstallState.Paused(halfway).progressOrNull())
        assertNull(InstallState.NotInstalled.progressOrNull())
        assertNull(InstallState.Verifying.progressOrNull())
        assertNull(InstallState.Installed.progressOrNull())
        assertNull(InstallState.Failed(InstallFailure.Unknown, "x").progressOrNull())
    }

    // --- progress card -------------------------------------------------------------------------

    @Test
    fun installedRingIsFull() {
        val copy = installProgressCopy(InstallState.Installed)
        assertEquals(1f, copy.ringFraction)
        assertEquals("100%", copy.ringPrimaryLabel)
        assertEquals("Installed", copy.ringSecondaryLabel)
        // Nothing left to transfer, so the screen shows no byte count.
        assertNull(copy.transferLine)
    }

    @Test
    fun verifyingShowsAnEllipsisRatherThanAPercentage() {
        val copy = installProgressCopy(InstallState.Verifying)
        assertEquals("…", copy.ringPrimaryLabel)
        assertEquals("Verifying", copy.ringSecondaryLabel)
    }

    @Test
    fun nothingDownloadedMeasuresNothing() {
        val copy = installProgressCopy(InstallState.NotInstalled)
        assertEquals(0f, copy.ringFraction)
        assertEquals("0%", copy.ringPrimaryLabel)
        assertNull(copy.transferLine)
        assertNull(copy.rateLine)
        assertNull(copy.errorMessage)
    }

    @Test
    fun downloadingReportsTransferAndRate() {
        val copy = installProgressCopy(InstallState.Downloading(halfway))
        assertEquals(0.5f, copy.ringFraction)
        assertEquals("50%", copy.ringPrimaryLabel)
        assertEquals("Downloaded", copy.ringSecondaryLabel)
        assertEquals("500 B of 1 kB", copy.transferLine)
        assertEquals("100 B/s · a few seconds remaining", copy.rateLine)
    }

    @Test
    fun anUnknownRateDropsTheRemainingClause() {
        val stalled = halfway.copy(bytesPerSecond = 0.0)
        val copy = installProgressCopy(InstallState.Downloading(stalled))
        assertEquals("—", copy.rateLine)
    }

    @Test
    fun pausedKeepsTheTransferLineButNotTheRate() {
        val copy = installProgressCopy(InstallState.Paused(halfway))
        assertEquals("Paused", copy.ringSecondaryLabel)
        assertEquals("500 B of 1 kB", copy.transferLine)
        assertNull(copy.rateLine)
    }

    @Test
    fun failureCarriesItsMessageThrough() {
        val copy = installProgressCopy(
            InstallState.Failed(InstallFailure.ChecksumMismatch, "The digest did not match"),
        )
        assertTrue(copy.isError)
        assertEquals("Stopped", copy.ringSecondaryLabel)
        assertEquals("The digest did not match", copy.errorMessage)
    }

    // --- milestones ----------------------------------------------------------------------------

    @Test
    fun thereAreAlwaysFourMilestonesInOrder() {
        val milestones = setupMilestones(InstallState.NotInstalled, CheckStatus.Pass)
        assertEquals(
            listOf(
                MilestoneKind.Hardware,
                MilestoneKind.Weights,
                MilestoneKind.Audit,
                MilestoneKind.Engine,
            ),
            milestones.map { it.kind },
        )
    }

    @Test
    fun anUncheckedDeviceLeavesHardwarePending() {
        val hardware = setupMilestones(InstallState.NotInstalled, null).first()
        assertEquals(MilestoneState.Pending, hardware.state)
        assertEquals("Checked on the previous screen", hardware.detail)
    }

    @Test
    fun anIncapableDeviceFailsTheHardwareMilestone() {
        val hardware = setupMilestones(InstallState.NotInstalled, CheckStatus.Fail).first()
        assertEquals(MilestoneState.Failed, hardware.state)
        assertEquals("This device cannot run the model", hardware.detail)
    }

    @Test
    fun aFailedDownloadPutsItsMessageOnTheWeightsRow() {
        val milestones = setupMilestones(
            InstallState.Failed(InstallFailure.Server, "The server returned 404"),
            CheckStatus.Pass,
        )
        val weights = milestones.single { it.kind == MilestoneKind.Weights }
        val audit = milestones.single { it.kind == MilestoneKind.Audit }
        assertEquals("The server returned 404", weights.detail)
        assertEquals(MilestoneState.Failed, weights.state)
        assertEquals(MilestoneState.Failed, audit.state)
    }

    @Test
    fun anInstalledModelCompletesEveryMilestone() {
        val milestones = setupMilestones(InstallState.Installed, CheckStatus.Pass)
        assertTrue(milestones.all { it.state == MilestoneState.Done })
    }

    @Test
    fun verifyingMakesTheAuditTheActiveRow() {
        val milestones = setupMilestones(InstallState.Verifying, CheckStatus.Pass)
        assertEquals(
            MilestoneState.Active,
            milestones.single { it.kind == MilestoneKind.Audit }.state,
        )
        assertEquals(
            MilestoneState.Pending,
            milestones.single { it.kind == MilestoneKind.Engine }.state,
        )
    }
}
