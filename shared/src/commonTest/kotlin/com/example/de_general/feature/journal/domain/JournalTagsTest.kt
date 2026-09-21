package com.example.de_general.feature.journal.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JournalTagsTest {

    @Test
    fun whitespaceIsStrippedRatherThanTrimmed() {
        assertEquals("twowords", normalizeTag("  two words  "))
        assertEquals("a", normalizeTag("\t a \n"))
    }

    @Test
    fun oneLeadingHashIsDroppedAndASecondIsKept() {
        assertEquals("rain", normalizeTag("#rain"))
        assertEquals("#rain", normalizeTag("##rain"))
        assertEquals("rain#", normalizeTag("rain#"))
    }

    @Test
    fun aTagThatIsOnlyWhitespaceOrAHashIsNothing() {
        assertNull(normalizeTag(""))
        assertNull(normalizeTag("   "))
        assertNull(normalizeTag("#"))
        assertNull(normalizeTag(" # "))
    }

    @Test
    fun anOverLongTagIsCappedRatherThanRejected() {
        val tag = normalizeTag("x".repeat(100))!!
        assertEquals(32, tag.length)
    }

    /**
     * The separator is safe **by construction**: [normalizeTag] removes every whitespace character,
     * so a newline cannot survive into a tag and therefore cannot split one in two on the way back.
     */
    @Test
    fun aTagContainingTheSeparatorCannotSplitIntoTwo() {
        val stored = encodeTags(listOf("two\nparts"))

        assertEquals("twoparts", stored)
        assertEquals(listOf("twoparts"), decodeTags(stored))
    }

    @Test
    fun duplicatesGoByCaseButTheFirstSpellingWins() {
        assertEquals(listOf("Rain", "sun"), normalizeTags(listOf("Rain", "rain", "sun", "SUN")))
    }

    @Test
    fun orderIsTheOrderTheyWereAddedIn() {
        assertEquals(listOf("c", "a", "b"), normalizeTags(listOf("c", "a", "b")))
    }

    @Test
    fun theListIsCappedAtTwelve() {
        val tags = normalizeTags((1..40).map { "tag$it" })

        assertEquals(12, tags.size)
        assertEquals("tag1", tags.first())
        assertEquals("tag12", tags.last())
    }

    /** "No tags" has exactly one representation in the column, so no query has to test for two. */
    @Test
    fun nothingEncodesToNullAndNeverToAnEmptyString() {
        assertNull(encodeTags(emptyList()))
        assertNull(encodeTags(listOf("", "   ", "#")))
    }

    @Test
    fun decodingWhatWasEncodedGivesTheNormalisedList() {
        val typed = listOf("#Rain", " two words ", "rain", "x".repeat(100))
        val normalised = normalizeTags(typed)

        assertEquals(normalised, decodeTags(encodeTags(typed)))
        assertEquals(listOf("Rain", "twowords", "x".repeat(32)), normalised)
    }

    /** A row that cannot be parsed must still open. Decoding is total. */
    @Test
    fun decodingIsTotal() {
        assertEquals(emptyList(), decodeTags(null))
        assertEquals(emptyList(), decodeTags(""))
        assertEquals(emptyList(), decodeTags("\n\n\n"))
        // Not empty, but not a crash either: a row holding junk still opens, and what it holds is
        // re-normalised on the way out rather than trusted.
        assertEquals(listOf(",,,"), decodeTags("  ,,,  "))
    }

    @Test
    fun oneTypedLineCanCarrySeveralTags() {
        assertEquals(listOf("rain", "walking", "quiet"), splitTagInput("rain, walking, quiet"))
        assertEquals(listOf("rain", "walking"), splitTagInput("#rain\n#walking"))
    }

    @Test
    fun addingRespectsTheCapAndTheDuplicateRuleAcrossBothLists() {
        assertEquals(listOf("Rain", "sun"), addTags(listOf("Rain"), "rain, sun"))

        val full = (1..12).map { "tag$it" }
        assertEquals(full, addTags(full, "thirteenth"))
    }

    @Test
    fun addingNothingChangesNothing() {
        assertEquals(listOf("rain"), addTags(listOf("rain"), "   "))
    }
}
