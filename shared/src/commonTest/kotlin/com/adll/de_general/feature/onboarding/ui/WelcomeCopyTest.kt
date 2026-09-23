package com.adll.de_general.feature.onboarding.ui

import com.adll.de_general.feature.onboarding.domain.CheckId
import com.adll.de_general.feature.onboarding.domain.CheckStatus
import com.adll.de_general.feature.onboarding.domain.CompatibilityReport
import com.adll.de_general.feature.onboarding.domain.DeviceCheck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WelcomeCopyTest {

    private fun check(id: CheckId, status: CheckStatus) = DeviceCheck(
        id = id,
        title = id.name,
        headline = "headline",
        detail = "detail",
        status = status,
    )

    private fun report(vararg statuses: CheckStatus) = CompatibilityReport(
        checks = statuses.mapIndexed { index, status ->
            check(CheckId.entries[index % CheckId.entries.size], status)
        },
    )

    private val allPass = report(
        CheckStatus.Pass,
        CheckStatus.Pass,
        CheckStatus.Pass,
        CheckStatus.Pass,
    )

    @Test
    fun beforeTheProbeReturnsNothingIsClaimed() {
        val copy = verdictCopy(report = null, probing = true)
        assertEquals(0f, copy.ringFraction)
        assertEquals("—", copy.ringPrimaryLabel)
        assertEquals("Checking", copy.ringSecondaryLabel)
        assertEquals("Reading your hardware…", copy.title)
    }

    @Test
    fun aFullyCapableDeviceIsReady() {
        val copy = verdictCopy(allPass, probing = false)
        assertEquals(1f, copy.ringFraction)
        assertEquals("4/4", copy.ringPrimaryLabel)
        assertEquals("Checks passed", copy.ringSecondaryLabel)
        assertEquals("Ready to run", copy.title)
        assertEquals(VerdictTone.Positive, copy.tone)
    }

    @Test
    fun oneWarningIsWorkableWithCaveats() {
        val copy = verdictCopy(
            report(CheckStatus.Pass, CheckStatus.Warn, CheckStatus.Pass, CheckStatus.Pass),
            probing = false,
        )
        assertEquals("3/4", copy.ringPrimaryLabel)
        assertEquals("Workable, with caveats", copy.title)
        assertEquals(VerdictTone.Caution, copy.tone)
    }

    @Test
    fun oneFailureBlocksRegardlessOfTheRest() {
        val copy = verdictCopy(
            report(CheckStatus.Pass, CheckStatus.Warn, CheckStatus.Fail, CheckStatus.Pass),
            probing = false,
        )
        assertEquals("Not supported", copy.title)
        assertEquals(VerdictTone.Blocked, copy.tone)
        assertTrue(copy.body.contains("cannot be met"))
    }

    /**
     * `checkCompatibility` always returns four checks, so this is unreachable from the app today.
     * The guard exists because a NaN would reach `ProgressRing`'s sweep angle and draw nothing at
     * all, which is a miserable thing to track down for the sake of one comparison.
     */
    @Test
    fun anEmptyReportGivesZeroRatherThanNaN() {
        val copy = verdictCopy(CompatibilityReport(emptyList()), probing = false)
        assertEquals(0f, copy.ringFraction)
        assertFalse(copy.ringFraction.isNaN())
        assertEquals("0/0", copy.ringPrimaryLabel)
    }
}
