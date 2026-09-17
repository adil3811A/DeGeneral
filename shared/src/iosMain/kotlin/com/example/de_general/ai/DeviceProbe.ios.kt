package com.example.de_general.ai

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoThermalState
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.thermalState
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState

/**
 * iOS implementation.
 *
 * **Written but unverified** — it has never been run, because this was built on Linux with no
 * Mac or simulator available. It compiles for `iosArm64` and `iosSimulatorArm64`; treat the
 * runtime behaviour as unproven until someone runs it in Xcode.
 */
actual class DeviceProbe {

    actual fun snapshot(): DeviceSnapshot {
        val processInfo = NSProcessInfo.processInfo
        val device = UIDevice.currentDevice

        // Battery reporting is off by default and must be asked for explicitly.
        device.batteryMonitoringEnabled = true

        val (free, total) = volumeBytes()

        return DeviceSnapshot(
            abi = "arm64",
            // Every iOS device Compose Multiplatform can target is 64-bit.
            is64Bit = true,
            cpuCores = processInfo.processorCount.toInt(),
            totalRamBytes = processInfo.physicalMemory.toLong(),
            // iOS deliberately exposes no "free RAM" figure. Reporting null makes the
            // compatibility rule fall back to a total-RAM estimate rather than invent one.
            availableRamBytes = null,
            freeDiskBytes = free,
            totalDiskBytes = total,
            osVersion = "${device.systemName} ${device.systemVersion}",
            thermal = readThermal(processInfo),
            batteryPercent = readBatteryPercent(device),
            charging = readCharging(device),
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun volumeBytes(): Pair<Long, Long> {
        val path = NSSearchPathForDirectoriesInDomains(
            directory = NSApplicationSupportDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true,
        ).firstOrNull() as? String ?: return 0L to 0L

        val attributes = NSFileManager.defaultManager
            .attributesOfFileSystemForPath(path, null)
            ?: return 0L to 0L

        val free = (attributes[NSFileSystemFreeSize] as? Number)?.toLong() ?: 0L
        val total = (attributes[NSFileSystemSize] as? Number)?.toLong() ?: 0L
        return free to total
    }

    private fun readThermal(processInfo: NSProcessInfo): ThermalState? =
        when (processInfo.thermalState) {
            NSProcessInfoThermalState.NSProcessInfoThermalStateNominal -> ThermalState.Nominal
            NSProcessInfoThermalState.NSProcessInfoThermalStateFair -> ThermalState.Fair
            NSProcessInfoThermalState.NSProcessInfoThermalStateSerious -> ThermalState.Serious
            NSProcessInfoThermalState.NSProcessInfoThermalStateCritical -> ThermalState.Critical
            else -> null
        }

    /** `batteryLevel` is 0f..1f, or -1f when the level is unavailable. */
    private fun readBatteryPercent(device: UIDevice): Int? {
        val level = device.batteryLevel
        if (level < 0f) return null
        return (level * 100).toInt().coerceIn(0, 100)
    }

    private fun readCharging(device: UIDevice): Boolean? =
        when (device.batteryState) {
            UIDeviceBatteryState.UIDeviceBatteryStateCharging,
            UIDeviceBatteryState.UIDeviceBatteryStateFull,
            -> true

            UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> false
            else -> null
        }
}
