package com.example.de_general.core.data

import okio.FileSystem
import okio.Path

/**
 * Where small preference files live on this device.
 *
 * Shaped like `ModelStorage`, for the same reason: only the *location* is platform-specific —
 * Android needs a `Context`, iOS asks `NSFileManager`. It sits in `core` rather than borrowing
 * `ModelStorage`'s directory because that belongs to `feature/onboarding`.
 */
expect class PreferencesStorage {
    /** App-private directory. Created on first write, not here. */
    val directory: Path

    /** Real filesystem on device; tests build [ThemePreferences] on a fake one directly. */
    val fileSystem: FileSystem
}
