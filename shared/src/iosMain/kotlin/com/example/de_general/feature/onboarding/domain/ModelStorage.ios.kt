package com.example.de_general.feature.onboarding.domain

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Weights live in Application Support, which is the right home for files the app needs but the
 * user never browses, and which iOS does not purge.
 *
 * **Unverified** — see the note on [DeviceProbe].
 */
actual class ModelStorage {
    actual val modelsDirectory: Path = run {
        val base = NSSearchPathForDirectoriesInDomains(
            directory = NSApplicationSupportDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true,
        ).firstOrNull() as? String ?: error("No Application Support directory on this device")
        "$base/models".toPath()
    }

    actual val fileSystem: FileSystem = FileSystem.SYSTEM
}
