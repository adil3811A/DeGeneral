package com.example.de_general.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The compatibility rules are the app's first promise to the user, and the easiest thing to get
 * quietly wrong — so they are a pure function, and this is the table that pins them down.
 */
class CompatibilityTest {

    private fun snapshot(
        abi: String = "arm64-v8a",
        is64Bit: Boolean = true,
        cpuCores: Int = 8,
        totalRam: Long = 8_000_000_000L,
        availableRam: Long? = 4_000_000_000L,
        freeDisk: Long = 30_000_000_000L,
        totalDisk: Long = 128_000_000_000L,
        thermal: ThermalState? = ThermalState.Nominal,
        battery: Int? = 80,
        charging: Boolean? = false,
    ) = DeviceSnapshot(
        abi = abi,
        is64Bit = is64Bit,
        cpuCores = cpuCores,
        totalRamBytes = totalRam,
        availableRamBytes = availableRam,
        freeDiskBytes = freeDisk,
        totalDiskBytes = totalDisk,
        osVersion = "Android 14 (API 34)",
        thermal = thermal,
        batteryPercent = battery,
        charging = charging,
    )

    private fun CompatibilityReport.status(id: CheckId) = checks.single { it.id == id }.status

    @Test
    fun healthyModernPhonePassesEverything() {
        val report = checkCompatibility(snapshot())

        assertEquals(CheckStatus.Pass, report.verdict)
        assertEquals(4, report.total)
        assertEquals(4, report.passed)
        assertTrue(report.canInstall)
    }

    @Test
    fun thirtyTwoBitDeviceIsBlocked() {
        val report = checkCompatibility(snapshot(abi = "armeabi-v7a", is64Bit = false))

        assertEquals(CheckStatus.Fail, report.status(CheckId.Processor))
        assertEquals(CheckStatus.Fail, report.verdict)
        assertFalse(report.canInstall)
    }

    @Test
    fun diskSmallerThanTheModelIsBlocked() {
        val report = checkCompatibility(snapshot(freeDisk = 500_000_000L))

        assertEquals(CheckStatus.Fail, report.status(CheckId.Storage))
        assertFalse(report.canInstall)
    }

    @Test
    fun diskBetweenTheFileSizeAndTheHeadroomWarnsButStillInstalls() {
        // Enough for the 806 MB file, short of the 903 MB the install wants.
        val report = checkCompatibility(snapshot(freeDisk = 850_000_000L))

        assertEquals(CheckStatus.Warn, report.status(CheckId.Storage))
        assertEquals(CheckStatus.Warn, report.verdict)
        assertTrue(report.canInstall)
    }

    @Test
    fun ramBelowTheRequirementIsBlocked() {
        val report = checkCompatibility(snapshot(totalRam = 2_000_000_000L, availableRam = 600_000_000L))

        assertEquals(CheckStatus.Fail, report.status(CheckId.Memory))
        assertFalse(report.canInstall)
    }

    @Test
    fun ramJustAboveTheRequirementIsTightRatherThanComfortable() {
        // 1.3 GB usable against a 1.2 GB requirement: inside the 20% tight margin.
        val report = checkCompatibility(snapshot(availableRam = 1_300_000_000L))

        assertEquals(CheckStatus.Warn, report.status(CheckId.Memory))
        assertTrue(report.canInstall)
    }

    @Test
    fun unknownAvailableRamFallsBackToHalfOfTotal() {
        // This is the iOS shape: physicalMemory is readable, free memory is not.
        val plenty = checkCompatibility(snapshot(totalRam = 6_000_000_000L, availableRam = null))
        assertEquals(CheckStatus.Pass, plenty.status(CheckId.Memory))

        // 2 GB total means 1 GB assumed usable, under the 1.2 GB requirement.
        val tooSmall = checkCompatibility(snapshot(totalRam = 2_000_000_000L, availableRam = null))
        assertEquals(CheckStatus.Fail, tooSmall.status(CheckId.Memory))
    }

    @Test
    fun unknownAvailableRamReportsTotalRatherThanInventingAFreeFigure() {
        val check = checkCompatibility(snapshot(availableRam = null))
            .checks.single { it.id == CheckId.Memory }

        assertTrue(check.headline.contains("total"), "was: ${check.headline}")
        assertFalse(check.headline.contains("free"), "was: ${check.headline}")
    }

    @Test
    fun heatWarnsButNeverBlocks() {
        val report = checkCompatibility(snapshot(thermal = ThermalState.Serious))

        assertEquals(CheckStatus.Warn, report.status(CheckId.ThermalAndBattery))
        assertTrue(report.canInstall, "heat is a 'not right now', not a 'not ever'")
    }

    @Test
    fun lowBatteryWarnsOnlyWhenUnplugged() {
        assertEquals(
            CheckStatus.Warn,
            checkCompatibility(snapshot(battery = 8, charging = false))
                .status(CheckId.ThermalAndBattery),
        )
        assertEquals(
            CheckStatus.Pass,
            checkCompatibility(snapshot(battery = 8, charging = true))
                .status(CheckId.ThermalAndBattery),
        )
    }

    @Test
    fun unknownThermalAndBatteryDoNotManufactureAWarning() {
        val check = checkCompatibility(snapshot(thermal = null, battery = null, charging = null))
            .checks.single { it.id == CheckId.ThermalAndBattery }

        assertEquals(CheckStatus.Pass, check.status)
        assertTrue(check.headline.contains("unknown"), "was: ${check.headline}")
    }

    @Test
    fun theWorstCheckDecidesTheVerdict() {
        val report = checkCompatibility(
            snapshot(freeDisk = 850_000_000L, is64Bit = false, abi = "armeabi-v7a"),
        )

        assertEquals(CheckStatus.Fail, report.verdict, "a Fail must outrank a Warn")
    }
}
