package com.adll.de_general.feature.journal.domain

import com.adll.de_general.core.ui.theme.Mood
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JournalMoodTest {

    @Test
    fun theSixMoodsAreInTheDesignsOrder() {
        assertEquals(
            listOf("Calm", "Joyful", "Reflective", "Grateful", "Energized", "Pensive"),
            JournalMood.entries.map { it.label },
        )
    }

    /**
     * Six chips on four accents, and which two pairs double up is a decision, not an accident.
     * Changing this means changing the palette in Stitch first — see [JournalMood].
     */
    @Test
    fun theTwoSharedAccentsAreTheOnesTheDesignImplies() {
        assertEquals(Mood.Happy, JournalMood.Grateful.accent)
        assertEquals(Mood.Happy, JournalMood.Joyful.accent)
        assertEquals(Mood.Reflective, JournalMood.Pensive.accent)
        assertEquals(Mood.Reflective, JournalMood.Reflective.accent)
        assertEquals(Mood.Calm, JournalMood.Calm.accent)
        assertEquals(Mood.Energetic, JournalMood.Energized.accent)
    }

    /** Six chips, four accents — the quantisation is deliberate and this pins that it is 4. */
    @Test
    fun theSixMoodsUseExactlyTheFourAccentsTheThemeHas() {
        assertEquals(Mood.entries.toSet(), JournalMood.entries.map { it.accent }.toSet())
    }

    @Test
    fun aStoredNameComesBackAsItsMood() {
        JournalMood.entries.forEach { mood ->
            assertEquals(mood, journalMoodOrNull(mood.name))
        }
    }

    /**
     * The whole reason this is not a Room enum: an unknown value must not be an exception. An
     * entry written by a newer build has to still open in an older one, minus the chip.
     */
    @Test
    fun anUnknownMoodIsNullRatherThanAThrow() {
        assertNull(journalMoodOrNull(null))
        assertNull(journalMoodOrNull(""))
        assertNull(journalMoodOrNull("Melancholy"))
        assertNull(journalMoodOrNull("calm"))
    }

    /** No emoji: neither shipped font guarantees coverage, and the chip dot is the affordance. */
    @Test
    fun noLabelCarriesAnEmoji() {
        JournalMood.entries.forEach { mood ->
            assertEquals(mood.label, mood.label.filter { it.code in 32..126 })
        }
    }
}
