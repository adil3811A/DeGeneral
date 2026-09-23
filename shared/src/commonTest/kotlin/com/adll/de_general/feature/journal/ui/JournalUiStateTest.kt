package com.adll.de_general.feature.journal.ui

import com.adll.de_general.feature.journal.domain.DateAnchor
import com.adll.de_general.feature.journal.domain.JournalEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The archive's decisions: empty versus loading, and what the date filter shows.
 *
 * The draft, the save and the compose-request counter left with the composer; the four `canSave`
 * cases moved to `CreateJournalUiStateTest` rather than being deleted.
 */
class JournalUiStateTest {

    /**
     * The flash-of-empty-state guard: until the database has actually answered, "no entries" is
     * not something the app knows, so it must not say it.
     */
    @Test
    fun loadingIsNotTheSameAsEmpty() {
        assertFalse(JournalUiState(loading = true, entries = emptyList()).isEmpty)
        assertTrue(JournalUiState(loading = false, entries = emptyList()).isEmpty)
    }

    @Test
    fun aLoadedJournalWithEntriesIsNotEmpty() {
        val state = JournalUiState(
            loading = false,
            entries = listOf(
                JournalEntry(id = 1, rawText = "hello", timestamp = 1_792_716_300_000L),
            ),
        )
        assertFalse(state.isEmpty)
    }

    // --- the date filter ------------------------------------------------------------------------

    private val now = 1_792_716_300_000L // Thu 22 Oct 2026, 8:45 PM New York
    private val newYork = TimeZone.of("America/New_York")

    private val todayEntry = JournalEntry(id = 2, rawText = "today", timestamp = now)
    private val yesterdayEntry =
        JournalEntry(id = 1, rawText = "yesterday", timestamp = now - 86_400_000L)

    private fun loaded(
        filter: DateAnchor?,
        entries: List<JournalEntry> = listOf(todayEntry, yesterdayEntry),
    ) = JournalUiState(
        loading = false,
        entries = entries,
        nowMillis = now,
        zone = newYork,
        filter = filter,
    )

    @Test
    fun visibleEntriesFollowTheFilter() {
        assertEquals(listOf(todayEntry, yesterdayEntry), loaded(null).visibleEntries)
        assertEquals(listOf(todayEntry), loaded(DateAnchor.Today).visibleEntries)
        assertEquals(listOf(yesterdayEntry), loaded(DateAnchor.Yesterday).visibleEntries)
    }

    @Test
    fun aDayWithNothingOnItIsFilteredEmptyNotEmpty() {
        val state = loaded(DateAnchor.On(LocalDate(2026, 1, 3)))
        assertTrue(state.isFilteredEmpty)
        assertFalse(state.isEmpty)
    }

    /** An empty journal is "nothing written yet", not "nothing on this day". */
    @Test
    fun anEmptyJournalIsNeverFilteredEmpty() {
        val state = loaded(DateAnchor.Today, entries = emptyList())
        assertTrue(state.isEmpty)
        assertFalse(state.isFilteredEmpty)
    }

    @Test
    fun loadingIsNeverFilteredEmpty() {
        val state = loaded(DateAnchor.On(LocalDate(2026, 1, 3))).copy(loading = true)
        assertFalse(state.isFilteredEmpty)
    }

    @Test
    fun theEmptyMessageNamesTheDay() {
        assertNull(loaded(null).filteredEmptyMessage)
        assertEquals("Nothing written today.", loaded(DateAnchor.Today).filteredEmptyMessage)
        assertEquals("Nothing written yesterday.", loaded(DateAnchor.Yesterday).filteredEmptyMessage)
        assertEquals(
            "Nothing written on Saturday, Jan 3, 2026.",
            loaded(DateAnchor.On(LocalDate(2026, 1, 3))).filteredEmptyMessage,
        )
    }

    @Test
    fun thePickerOpensOnTheFilteredDayOrToday() {
        assertEquals(LocalDate(2026, 10, 22), loaded(null).pickerDate)
        assertEquals(LocalDate(2026, 10, 21), loaded(DateAnchor.Yesterday).pickerDate)
        assertEquals(LocalDate(2026, 1, 3), loaded(DateAnchor.On(LocalDate(2026, 1, 3))).pickerDate)
    }
}
