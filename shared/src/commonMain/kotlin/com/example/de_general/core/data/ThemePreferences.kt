package com.example.de_general.core.data

import com.example.de_general.core.ui.theme.ThemeMode
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import okio.Path

/**
 * The appearance choice, kept as one word in one file in app-private storage.
 *
 * **Read synchronously in the constructor**, on purpose. The theme is needed on the very first
 * frame, and the file is a few bytes — so the app opens in the right theme instead of flashing light
 * and then going dark. Same trick `AppNavHost` plays with the install state.
 *
 * Not in Room: the database is lazy so that someone who never finishes setup never opens it, and a
 * value needed on frame one would force it open on every launch — plus a migration, for one enum.
 * Not in DataStore: a new dependency to store one word okio already stores fine.
 *
 * A display preference, nothing personal, and it never leaves the device.
 */
class ThemePreferences(
    private val fileSystem: FileSystem,
    private val file: Path,
    /** Where the write happens. Defaults to the same context Room uses; `Dispatchers.IO` is JVM-only. */
    private val ioContext: CoroutineContext = databaseQueryContext,
) {
    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    /**
     * Updates [mode] first, so the UI answers the tap immediately, then persists.
     *
     * The write goes to a sibling `.tmp` and is moved over [file], so a crash mid-write leaves the
     * previous choice rather than half a word. A failed write is swallowed: the choice still holds
     * for this session, and the worst case is that it is forgotten on the next launch.
     */
    suspend fun setMode(mode: ThemeMode) {
        _mode.value = mode
        withContext(ioContext) {
            try {
                val directory = file.parent ?: return@withContext
                fileSystem.createDirectories(directory)
                val tmp = directory / "${file.name}.tmp"
                fileSystem.write(tmp) { writeUtf8(mode.storageKey) }
                fileSystem.atomicMove(tmp, file)
            } catch (_: IOException) {
                // See the KDoc: in-memory choice stands, persistence is best effort.
            }
        }
    }

    private fun read(): ThemeMode {
        val stored = try {
            fileSystem.read(file) { readUtf8() }
        } catch (_: IOException) {
            null // missing or unreadable — both mean "never chosen"
        }
        return ThemeMode.fromStorageKey(stored)
    }
}
