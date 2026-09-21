package com.example.de_general.feature.journal.ui

import com.example.de_general.feature.journal.domain.JournalEntry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Two cases, because the archive screen now only has two things to decide.
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
}
