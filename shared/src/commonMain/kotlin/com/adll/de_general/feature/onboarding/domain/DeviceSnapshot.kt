package com.adll.de_general.feature.onboarding.domain

/**
 * What the device looks like right now, as far as the platform will honestly tell us.
 *
 * Several fields are nullable because the answer genuinely is not available everywhere, and a
 * guess would be worse than an absence:
 *  - [availableRamBytes] — iOS exposes total physical memory but no "free" figure.
 *  - [thermal] — Android only reports thermal status from API 29.
 *  - [batteryPercent] / [charging] — unavailable if the battery broadcast is missing.
 *
 * Anything null renders as "Unknown" and makes the corresponding check fall back to a weaker but
 * truthful rule. See [checkCompatibility].
 */
data class DeviceSnapshot(
    /** Primary ABI, e.g. `arm64-v8a` on Android or `arm64` on iOS. */
    val abi: String,
    val is64Bit: Boolean,
    val cpuCores: Int,
    val totalRamBytes: Long,
    val availableRamBytes: Long?,
    val freeDiskBytes: Long,
    val totalDiskBytes: Long,
    /** Human-readable OS version, e.g. "Android 14 (API 34)". */
    val osVersion: String,
    val thermal: ThermalState?,
    /** 0..100, or null when unknown. */
    val batteryPercent: Int?,
    val charging: Boolean?,
) {
    /**
     * RAM we can plan against: the free figure where the platform reports one, otherwise a
     * conservative slice of total. Phones rarely have more than about half their RAM free.
     */
    val usableRamBytes: Long
        get() = availableRamBytes ?: (totalRamBytes / 2)
}

enum class ThermalState {
    Nominal,
    Fair,
    Serious,
    Critical,
}
