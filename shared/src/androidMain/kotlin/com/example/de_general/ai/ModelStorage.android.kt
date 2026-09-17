package com.example.de_general.ai

import android.content.Context
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

/**
 * Weights live in the app's private files directory.
 *
 * That gets them out of the user's media, removed with the app, excluded from cloud backup by
 * size, and — on Android 10 and later — encrypted at rest by the OS as part of file-based
 * encryption. The app itself does no encryption, so the UI says "private sandbox" rather than
 * claiming a guarantee we do not provide.
 */
actual class ModelStorage(context: Context) {
    actual val modelsDirectory: Path = context.filesDir.resolve("models").toOkioPath()
    actual val fileSystem: FileSystem = FileSystem.SYSTEM
}
