package com.adll.de_general.core.data

import android.content.Context
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

/** The app's private files directory — removed with the app, never visible to other apps. */
actual class PreferencesStorage(context: Context) {
    actual val directory: Path = context.filesDir.resolve("preferences").toOkioPath()
    actual val fileSystem: FileSystem = FileSystem.SYSTEM
}
