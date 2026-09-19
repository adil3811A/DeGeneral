package com.example.de_general.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

/**
 * Semantic mood accents from the "Mindful Scribe" design system.
 *
 * These live only in the design system's written guidance, not in its machine-readable token map,
 * so they are transcribed here by hand from the "Dynamic Emotion & Mood Accent Tokens" section.
 * They modulate entry tags, contextual cards, highlight tints and analytics badges.
 */
enum class Mood { Calm, Happy, Reflective, Energetic }

@Immutable
data class MoodAccent(
    val mood: Mood,
    /** The core accent — the 6px chip dot, waveform tint, selected radio bead. */
    val accent: Color,
    /** Selected-chip and tag background. */
    val container: Color,
    /** Text and icons drawn on [container]. */
    val onContainer: Color,
) {
    /**
     * The design system's "Mood Glow Bleed": a diffuse ambient wash at 4% opacity around an
     * active card, warm enough to read as mood without touching text contrast.
     */
    val glow: Color get() = accent.copy(alpha = 0.04f)
}

@Immutable
data class MoodPalette(
    val calm: MoodAccent,
    val happy: MoodAccent,
    val reflective: MoodAccent,
    val energetic: MoodAccent,
    /**
     * Local shield / encrypted safe. Not a mood — it marks on-device intelligence with no cloud
     * egress, and carries the "On-Device Encrypted" badge.
     */
    val privacyShield: Color,
) {
    val all: List<MoodAccent> get() = listOf(calm, happy, reflective, energetic)

    operator fun get(mood: Mood): MoodAccent = when (mood) {
        Mood.Calm -> calm
        Mood.Happy -> happy
        Mood.Reflective -> reflective
        Mood.Energetic -> energetic
    }

    /**
     * Snaps an arbitrary hex to the closest accent in this palette.
     *
     * `JournalEntry.feelingColor` holds whatever hex the model picked for an entry's mood. Letting
     * that colour onto the canvas directly would break the palette, so it is quantised to one of
     * the four accents instead — which is the "chromatic tones shift quietly" behaviour the design
     * system describes, rather than arbitrary colour.
     *
     * Returns [calm] for null, blank or unparseable input.
     */
    fun nearestTo(feelingColor: String?): MoodAccent {
        val target = parseHexColor(feelingColor) ?: return calm
        return all.minBy { colorDistance(it.accent, target) }
    }
}

/**
 * Parses `#RRGGBB`, `#AARRGGBB` or the same without the leading `#`. Returns null on anything
 * else, including named colours and `rgb()` notation — the model is expected to emit hex.
 */
internal fun parseHexColor(value: String?): Color? {
    val hex = value?.trim()?.removePrefix("#") ?: return null
    val rgb = when (hex.length) {
        6 -> hex
        8 -> hex.substring(2) // drop alpha; mood is a hue decision, not an opacity one
        else -> return null
    }
    val parsed = rgb.toLongOrNull(radix = 16) ?: return null
    return Color(0xFF000000 or parsed)
}

/**
 * Perceptual-ish distance using the "redmean" weighting — closer to how the eye reads colour
 * difference than plain RGB euclidean, without dragging in a full Lab conversion.
 */
private fun colorDistance(a: Color, b: Color): Float {
    val r1 = a.red * 255f
    val g1 = a.green * 255f
    val b1 = a.blue * 255f
    val r2 = b.red * 255f
    val g2 = b.green * 255f
    val b2 = b.blue * 255f

    val rMean = (r1 + r2) / 2f
    val dr = r1 - r2
    val dg = g1 - g2
    val db = b1 - b2

    return sqrt(
        (2f + rMean / 256f) * dr * dr +
            4f * dg * dg +
            (2f + (255f - rMean) / 256f) * db * db
    )
}

val MindfulMoodPalette = MoodPalette(
    calm = MoodAccent(
        mood = Mood.Calm,
        accent = Color(0xFF4A675F),      // restorative sage — balance, meditation, groundedness
        container = Color(0xFFD7E6DF),
        onContainer = Color(0xFF072019),
    ),
    happy = MoodAccent(
        mood = Mood.Happy,
        accent = Color(0xFFA36814),      // warm amber saffron — gratitude, joy, warmth
        container = Color(0xFFFFE0B2),
        onContainer = Color(0xFF2D1600),
    ),
    reflective = MoodAccent(
        mood = Mood.Reflective,
        accent = Color(0xFF635B7A),      // twilight lavender — processing, memory, vulnerability
        container = Color(0xFFE6DFF2),
        onContainer = Color(0xFF1F1833),
    ),
    energetic = MoodAccent(
        mood = Mood.Energetic,
        accent = Color(0xFFA9543E),      // soft coral peach — inspiration, passion, movement
        container = Color(0xFFFFDAD2),
        onContainer = Color(0xFF3D0B03),
    ),
    privacyShield = Color(0xFF3E6857),
)

val LocalMoodPalette = staticCompositionLocalOf { MindfulMoodPalette }
