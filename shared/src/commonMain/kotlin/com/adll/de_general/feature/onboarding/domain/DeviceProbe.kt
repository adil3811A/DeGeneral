package com.adll.de_general.feature.onboarding.domain

/**
 * Reads a [DeviceSnapshot] from the platform.
 *
 * Follows the same shape as `DatabaseFactory`: an `expect class` whose constructor differs per
 * target (Android needs a `Context`, iOS needs nothing), built at the platform entry point and
 * handed down.
 */
expect class DeviceProbe {
    fun snapshot(): DeviceSnapshot
}
