package com.example.de_general.ai

/**
 * Whether this device can run [GemmaThreeOneB], decided from a [DeviceSnapshot].
 *
 * Deliberately a pure function over plain data: no Compose, no platform types, no I/O. The whole
 * decision surface is therefore testable in `commonTest`, which matters because these rules are
 * the app's first promise to the user and the easiest thing to get quietly wrong.
 */

enum class CheckStatus {
    /** Meets the requirement with room to spare. */
    Pass,

    /** Will work, but it is tight or the moment is wrong. Never blocks. */
    Warn,

    /** Cannot run the model. Blocks the install. */
    Fail,
}

enum class CheckId {
    Processor,
    Memory,
    Storage,
    ThermalAndBattery,
}

/**
 * One diagnostic row.
 *
 * @param headline the measured value, e.g. "5.4 GB free of 8.0 GB"
 * @param detail why that verdict, e.g. "Exceeds the 1.2 GB the model needs"
 */
data class DeviceCheck(
    val id: CheckId,
    val title: String,
    val headline: String,
    val detail: String,
    val status: CheckStatus,
)

data class CompatibilityReport(
    val checks: List<DeviceCheck>,
) {
    val passed: Int get() = checks.count { it.status == CheckStatus.Pass }
    val total: Int get() = checks.size

    /** The worst status wins. */
    val verdict: CheckStatus
        get() = when {
            checks.any { it.status == CheckStatus.Fail } -> CheckStatus.Fail
            checks.any { it.status == CheckStatus.Warn } -> CheckStatus.Warn
            else -> CheckStatus.Pass
        }

    val canInstall: Boolean get() = verdict != CheckStatus.Fail
}

/** Below this the device is running hot enough that a long download is unkind. */
private const val LOW_BATTERY_PERCENT = 15

/** Within this fraction of the requirement counts as "tight" rather than comfortable. */
private const val TIGHT_MARGIN = 0.20

fun checkCompatibility(
    snapshot: DeviceSnapshot,
    spec: ModelSpec = GemmaThreeOneB,
): CompatibilityReport = CompatibilityReport(
    checks = listOf(
        processorCheck(snapshot),
        memoryCheck(snapshot, spec),
        storageCheck(snapshot, spec),
        thermalAndBatteryCheck(snapshot, spec),
    ),
)

private fun processorCheck(snapshot: DeviceSnapshot): DeviceCheck {
    val headline = "${snapshot.cpuCores} cores · ${snapshot.abi}"
    return if (snapshot.is64Bit) {
        DeviceCheck(
            id = CheckId.Processor,
            title = "Processor",
            headline = headline,
            detail = "64-bit CPU. Inference runs here, on ${snapshot.osVersion}.",
            status = CheckStatus.Pass,
        )
    } else {
        DeviceCheck(
            id = CheckId.Processor,
            title = "Processor",
            headline = headline,
            detail = "A 64-bit CPU is required. This device cannot address the model's weights.",
            status = CheckStatus.Fail,
        )
    }
}

private fun memoryCheck(snapshot: DeviceSnapshot, spec: ModelSpec): DeviceCheck {
    val usable = snapshot.usableRamBytes
    val required = spec.requiredRamBytes
    val headline = if (snapshot.availableRamBytes != null) {
        "${formatBytes(snapshot.availableRamBytes)} free of ${formatBytes(snapshot.totalRamBytes)}"
    } else {
        // iOS reports no free figure, so say what we do know rather than inventing one.
        "${formatBytes(snapshot.totalRamBytes)} total"
    }

    val status = when {
        usable < required -> CheckStatus.Fail
        usable < required * (1 + TIGHT_MARGIN) -> CheckStatus.Warn
        else -> CheckStatus.Pass
    }

    val detail = when (status) {
        CheckStatus.Pass -> "Needs ~${formatBytes(required)} while writing. Comfortable."
        CheckStatus.Warn ->
            "Needs ~${formatBytes(required)}. This is tight — close other apps before writing."
        CheckStatus.Fail ->
            "Needs ~${formatBytes(required)}, more than this device can spare."
    }

    return DeviceCheck(CheckId.Memory, "Memory", headline, detail, status)
}

private fun storageCheck(snapshot: DeviceSnapshot, spec: ModelSpec): DeviceCheck {
    val headline =
        "${formatBytes(snapshot.freeDiskBytes)} free of ${formatBytes(snapshot.totalDiskBytes)}"

    val status = when {
        snapshot.freeDiskBytes < spec.sizeBytes -> CheckStatus.Fail
        snapshot.freeDiskBytes < spec.requiredDiskBytes -> CheckStatus.Warn
        else -> CheckStatus.Pass
    }

    val detail = when (status) {
        CheckStatus.Pass -> "The model weights take ${formatBytes(spec.sizeBytes)}."
        CheckStatus.Warn ->
            "The weights take ${formatBytes(spec.sizeBytes)} and the download needs headroom " +
                "on top. Free up a little space first."
        CheckStatus.Fail ->
            "Not enough room for ${formatBytes(spec.sizeBytes)} of weights."
    }

    return DeviceCheck(CheckId.Storage, "Storage", headline, detail, status)
}

private fun thermalAndBatteryCheck(snapshot: DeviceSnapshot, spec: ModelSpec): DeviceCheck {
    val thermalText = when (snapshot.thermal) {
        ThermalState.Nominal -> "Normal temperature"
        ThermalState.Fair -> "Slightly warm"
        ThermalState.Serious -> "Running hot"
        ThermalState.Critical -> "Overheating"
        null -> "Temperature unknown"
    }
    val batteryText = when {
        snapshot.batteryPercent == null -> "battery unknown"
        snapshot.charging == true -> "battery at ${snapshot.batteryPercent}%, charging"
        else -> "battery at ${snapshot.batteryPercent}%"
    }

    val hot = snapshot.thermal == ThermalState.Serious || snapshot.thermal == ThermalState.Critical
    val lowBattery =
        snapshot.batteryPercent != null &&
            snapshot.batteryPercent < LOW_BATTERY_PERCENT &&
            snapshot.charging != true

    // Never a Fail: heat and charge are a "not right now", not a "not ever".
    val status = if (hot || lowBattery) CheckStatus.Warn else CheckStatus.Pass

    val detail = when {
        hot && lowBattery -> "Let the device cool down and plug it in before downloading."
        hot -> "Let the device cool down before downloading — a long transfer will add heat."
        lowBattery -> "Plug in before downloading. The transfer is ${formatBytes(spec.sizeBytes)}."
        else -> "Good conditions for a large download."
    }

    return DeviceCheck(
        id = CheckId.ThermalAndBattery,
        title = "Thermal & battery",
        headline = "$thermalText · $batteryText",
        detail = detail,
        status = status,
    )
}
