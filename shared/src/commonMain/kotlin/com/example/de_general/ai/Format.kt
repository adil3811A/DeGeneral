package com.example.de_general.ai

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Human-readable sizes, speeds and durations.
 *
 * Decimal units (1 GB = 1,000,000,000 bytes), matching what Hugging Face, Android's storage
 * settings and the Stitch design all show. Binary units would make the same file look smaller
 * than every other number the user sees for it.
 */

fun formatBytes(bytes: Long): String {
    val negative = bytes < 0
    val value = abs(bytes)
    val text = when {
        value >= 1_000_000_000L -> "${oneDecimal(value / 1_000_000_000.0)} GB"
        value >= 1_000_000L -> "${(value / 1_000_000.0).roundToLong()} MB"
        value >= 1_000L -> "${(value / 1_000.0).roundToLong()} kB"
        else -> "$value B"
    }
    return if (negative) "-$text" else text
}

/** Bytes per second, as the download screen shows it. */
fun formatSpeed(bytesPerSecond: Double): String = when {
    bytesPerSecond <= 0.0 -> "—"
    bytesPerSecond >= 1_000_000.0 -> "${oneDecimal(bytesPerSecond / 1_000_000.0)} MB/s"
    bytesPerSecond >= 1_000.0 -> "${(bytesPerSecond / 1_000.0).roundToLong()} kB/s"
    else -> "${bytesPerSecond.roundToLong()} B/s"
}

/**
 * Coarse "time remaining". Deliberately rounded — a download ETA that ticks down by the second
 * reads as precision the estimate does not have.
 */
fun formatRemaining(seconds: Long): String = when {
    seconds < 0 -> "—"
    seconds < 10 -> "a few seconds"
    seconds < 60 -> "~${(seconds / 5) * 5}s"
    seconds < 3600 -> {
        val minutes = (seconds / 60.0).roundToInt()
        if (minutes <= 1) "~1 min" else "~$minutes min"
    }
    else -> {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        if (minutes == 0L) "~${hours}h" else "~${hours}h ${minutes}m"
    }
}

private fun oneDecimal(value: Double): String {
    val scaled = (value * 10).roundToLong()
    return "${scaled / 10}.${scaled % 10}"
}
