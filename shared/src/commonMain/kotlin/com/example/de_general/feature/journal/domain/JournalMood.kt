package com.example.de_general.feature.journal.domain

import com.example.de_general.core.ui.theme.Mood

/**
 * The six moods the composer offers, in the order the Stitch "Create Journal" screen lays them out.
 *
 * Pure data — no Compose, no platform types — so `commonTest` covers it on every target, the same
 * reason `Compatibility.kt` and `ChatPrompt.kt` are shaped this way.
 *
 * **Six chips, four accents.** The theme's [Mood] palette has four accents and `docs/THEME.md` is
 * explicit that Stitch is upstream: adding a fifth and sixth accent here would fork the palette
 * away from its source, and `tools/check_theme_tokens.py` only reads `Color.kt`, so the drift
 * would not even be caught. So Grateful borrows Happy's accent and Pensive borrows Reflective's.
 * Two pairs therefore share a selected colour; the label disambiguates, and only one chip is ever
 * selected at a time. If six real accents are wanted, that is a Stitch change plus a re-port, after
 * which this maps one-to-one and nothing else moves.
 *
 * No emoji. Neither Newsreader nor Plus Jakarta Sans guarantees coverage, and the design system's
 * 6px chip dot is the affordance it actually specifies — that dot is [accent].
 */
enum class JournalMood(val label: String, val accent: Mood) {
    Calm("Calm", Mood.Calm),
    Joyful("Joyful", Mood.Happy),
    Reflective("Reflective", Mood.Reflective),
    Grateful("Grateful", Mood.Happy),
    Energized("Energized", Mood.Energetic),
    Pensive("Pensive", Mood.Reflective),
}

/**
 * The stored value for [mood], or null if there is none.
 *
 * The column holds the constant's `name`, and this is deliberately hand-rolled rather than a Room
 * `@TypeConverter` over the enum: Room's enum support **throws** on a value the build does not
 * recognise, which would mean an entry written by a newer version of the app could not be opened
 * by an older one at all. A journal should lose a chip, not refuse to open.
 */
fun journalMoodOrNull(mood: String?): JournalMood? =
    JournalMood.entries.firstOrNull { it.name == mood }
