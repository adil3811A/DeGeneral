package com.adll.de_general.core.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeModeTest {

    @Test
    fun systemFollowsThePlatform() {
        assertTrue(ThemeMode.System.isDark(systemDark = true))
        assertFalse(ThemeMode.System.isDark(systemDark = false))
    }

    @Test
    fun lightAndDarkIgnoreThePlatform() {
        assertFalse(ThemeMode.Light.isDark(systemDark = true))
        assertFalse(ThemeMode.Light.isDark(systemDark = false))
        assertTrue(ThemeMode.Dark.isDark(systemDark = true))
        assertTrue(ThemeMode.Dark.isDark(systemDark = false))
    }

    @Test
    fun everyModeRoundTripsThroughItsStorageKey() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorageKey(mode.storageKey))
        }
    }

    @Test
    fun surroundingWhitespaceIsTolerated() {
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorageKey(" dark\n"))
    }

    @Test
    fun anythingUnrecognisedFallsBackToSystem() {
        listOf(null, "", "   ", "purple", "Dark", "DARK").forEach { key ->
            assertEquals(ThemeMode.System, ThemeMode.fromStorageKey(key), "key = \"$key\"")
        }
    }

    @Test
    fun storageKeysAreStable() {
        // Written to disk. Changing one silently resets everyone who picked it.
        assertEquals(
            listOf("system", "light", "dark"),
            ThemeMode.entries.map { it.storageKey },
        )
    }
}
