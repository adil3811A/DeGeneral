package com.example.de_general.ai

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs

actual class DeviceProbe(private val context: Context) {

    actual fun snapshot(): DeviceSnapshot {
        val memory = ActivityManager.MemoryInfo().also { info ->
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.getMemoryInfo(info)
        }

        // Measure the volume the model will actually live on, not some notional external card.
        val stats = StatFs(context.filesDir.absolutePath)

        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

        return DeviceSnapshot(
            abi = abi,
            is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamBytes = memory.totalMem,
            availableRamBytes = memory.availMem,
            freeDiskBytes = stats.availableBytes,
            totalDiskBytes = stats.totalBytes,
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            thermal = readThermal(),
            batteryPercent = readBatteryPercent(),
            charging = readCharging(),
        )
    }

    /** Thermal status only exists from API 29. Below that the answer is genuinely unknown. */
    private fun readThermal(): ThermalState? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
        return when (power.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE,
            PowerManager.THERMAL_STATUS_LIGHT,
            -> ThermalState.Nominal

            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.Fair
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.Serious

            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN,
            -> ThermalState.Critical

            else -> null
        }
    }

    private fun readBatteryPercent(): Int? {
        val manager =
            context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        // The property returns Integer.MIN_VALUE when the device cannot answer.
        return level.takeIf { it in 0..100 }
    }

    private fun readCharging(): Boolean? {
        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            ?: return null
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> true
            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING,
            -> false

            else -> null
        }
    }
}
