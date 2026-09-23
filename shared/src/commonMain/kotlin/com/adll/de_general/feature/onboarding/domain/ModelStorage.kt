package com.adll.de_general.feature.onboarding.domain

import okio.FileSystem
import okio.Path

/**
 * Where the model files live on this device.
 *
 * Only the *location* is platform-specific — Android needs a `Context` to find its private files
 * directory, iOS asks `NSFileManager`. Everything else is shared, so this is a thin `expect class`
 * around a directory and the filesystem to use, and [ModelFiles] does the rest in common code.
 */
expect class ModelStorage {
    /** The app-private directory the weights are written to. */
    val modelsDirectory: Path

    /** Real filesystem on device; swapped for a fake in tests. */
    val fileSystem: FileSystem
}

/**
 * The two paths a model occupies, and the questions worth asking about them.
 *
 * There is no marker file and no database row: **the final file existing at the right size is the
 * install state**. Nothing can drift out of sync with itself.
 */
class ModelFiles(
    private val fileSystem: FileSystem,
    private val directory: Path,
    private val spec: ModelSpec,
) {
    /** The verified model. Only ever created by renaming [partial] after its digest matched. */
    val target: Path = directory / spec.fileName

    /** The in-flight download. Survives pause, app restart and a killed process. */
    val partial: Path = directory / "${spec.fileName}.part"

    fun isInstalled(): Boolean =
        fileSystem.metadataOrNull(target)?.size == spec.sizeBytes

    /** Bytes already on disk for a resumable download; 0 when there is nothing to resume. */
    fun partialBytes(): Long = fileSystem.metadataOrNull(partial)?.size ?: 0L

    fun ensureDirectory() {
        fileSystem.createDirectories(directory)
    }

    fun deletePartial() {
        fileSystem.delete(partial, mustExist = false)
    }

    fun deleteAll() {
        fileSystem.delete(target, mustExist = false)
        deletePartial()
    }

    /** Promote a verified partial to the real thing. */
    fun promote() {
        fileSystem.atomicMove(partial, target)
    }
}
