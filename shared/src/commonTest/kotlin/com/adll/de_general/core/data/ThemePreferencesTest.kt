package com.adll.de_general.core.data

import com.adll.de_general.core.ui.theme.ThemeMode
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

/**
 * The preference file, against an in-memory filesystem.
 *
 * No `MockEngine` and no virtual-time interleaving here — the writes run inline on
 * [EmptyCoroutineContext] — so the hang described in `CLAUDE.md` does not apply.
 */
class ThemePreferencesTest {

    private val fileSystem = FakeFileSystem()
    private val directory = "/prefs".toPath()
    private val file = directory / "theme_mode"

    private fun preferences() = ThemePreferences(fileSystem, file, EmptyCoroutineContext)

    @AfterTest
    fun tearDown() {
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun neverChosenIsSystem() {
        assertEquals(ThemeMode.System, preferences().mode.value)
    }

    @Test
    fun aChoiceSurvivesARestart() = runTest {
        preferences().setMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, preferences().mode.value)
    }

    @Test
    fun theFlowHoldsTheNewChoiceAsSoonAsSetModeReturns() = runTest {
        val preferences = preferences()
        preferences.setMode(ThemeMode.Light)
        assertEquals(ThemeMode.Light, preferences.mode.value)
    }

    @Test
    fun theLastChoiceWins() = runTest {
        val preferences = preferences()
        preferences.setMode(ThemeMode.Dark)
        preferences.setMode(ThemeMode.Light)
        preferences.setMode(ThemeMode.System)
        assertEquals(ThemeMode.System, preferences().mode.value)
    }

    @Test
    fun aCorruptFileIsSystem() {
        fileSystem.createDirectories(directory)
        fileSystem.write(file) { writeUtf8("not a theme") }
        assertEquals(ThemeMode.System, preferences().mode.value)
    }

    @Test
    fun theDirectoryIsCreatedOnFirstWrite() = runTest {
        assertFalse(fileSystem.exists(directory))
        preferences().setMode(ThemeMode.Dark)
        assertEquals("dark", fileSystem.read(file) { readUtf8() })
    }

    @Test
    fun noTempFileIsLeftBehind() = runTest {
        preferences().setMode(ThemeMode.Dark)
        assertEquals(listOf(file), fileSystem.list(directory))
    }
}
