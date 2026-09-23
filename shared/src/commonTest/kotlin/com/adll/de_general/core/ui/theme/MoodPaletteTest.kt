package com.adll.de_general.core.ui.theme

import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * The light and dark palettes must agree on *which* mood a stored `feelingColor` means — only the
 * colours may differ. Otherwise an entry's mood would change when the theme does.
 */
class MoodPaletteTest {

    private val hexes = listOf(
        // Each palette's own accents — the dark ones are the likeliest to expose a palette-relative
        // snap.
        "#4A675F", "#A36814", "#635B7A", "#A9543E",
        "#A8D3C5", "#F0CF9E", "#D2C9E3", "#E8B4BE",
        // In-betweens and outliers.
        "#E8A33D", "#7F7F7F", "#000000", "#FFFFFF", "#FF0000", "#0000FF", "#88AA99",
        "FF4A675F", // 8-digit, alpha dropped
    )

    @Test
    fun bothPalettesPickTheSameMoodForEveryHex() {
        hexes.forEach { hex ->
            assertEquals(
                MindfulMoodPalette.nearestTo(hex).mood,
                MindfulMoodPaletteDark.nearestTo(hex).mood,
                "hex = $hex",
            )
        }
    }

    @Test
    fun eachPaletteReturnsItsOwnAccent() {
        hexes.forEach { hex ->
            val mood = MindfulMoodPalette.nearestTo(hex).mood
            assertSame(MindfulMoodPalette[mood], MindfulMoodPalette.nearestTo(hex))
            assertSame(MindfulMoodPaletteDark[mood], MindfulMoodPaletteDark.nearestTo(hex))
        }
    }

    @Test
    fun theLightAccentsAreTheReference() {
        // A light accent's own hex means its own mood — in the dark palette too.
        // Note the converse does NOT hold: the dark accents are pale, and pale sage (#A8D3C5)
        // snaps to Reflective against the light reference. That is fine — nothing stores dark
        // hexes as feelingColor — and it is why the reference has to be one fixed palette.
        MindfulMoodPalette.all.forEach { accent ->
            val hex = "#" + (accent.accent.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0')
            assertEquals(accent.mood, MindfulMoodPalette.nearestTo(hex).mood, "hex = $hex")
            assertEquals(accent.mood, MindfulMoodPaletteDark.nearestTo(hex).mood, "hex = $hex")
        }
    }

    @Test
    fun unparseableInputIsCalmInBothPalettes() {
        listOf(null, "", "   ", "sage", "#12345").forEach { input ->
            assertSame(MindfulMoodPalette.calm, MindfulMoodPalette.nearestTo(input))
            assertSame(MindfulMoodPaletteDark.calm, MindfulMoodPaletteDark.nearestTo(input))
        }
    }
}
