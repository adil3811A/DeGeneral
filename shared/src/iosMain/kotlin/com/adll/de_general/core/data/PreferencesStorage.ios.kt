package com.adll.de_general.core.data

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Application Support, like the model weights in `ModelStorage.ios.kt`.
 *
 * **Unverified** — iOS compiles but has never been run.
 */
actual class PreferencesStorage {
    actual val directory: Path = run {
        val base = NSSearchPathForDirectoriesInDomains(
            directory = NSApplicationSupportDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true,
        ).firstOrNull() as? String ?: error("No Application Support directory on this device")
        "$base/preferences".toPath()
    }

    actual val fileSystem: FileSystem = FileSystem.SYSTEM
}
