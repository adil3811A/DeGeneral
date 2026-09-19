package com.example.de_general.feature.journal.ui

import com.example.de_general.feature.journal.domain.JournalEntry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JournalUiStateTest {

    @Test
    fun anEmptyDraftCannotBeSaved() {
        assertFalse(JournalUiState(draft = "").canSave)
    }

    @Test
    fun aWhitespaceOnlyDraftCannotBeSaved() {
        assertFalse(JournalUiState(draft = "   \n  ").canSave)
    }

    @Test
    fun aDraftAlreadyBeingSavedCannotBeSavedAgain() {
        assertFalse(JournalUiState(draft = "something", saving = true).canSave)
    }

    @Test
    fun aDraftWithTextCanBeSaved() {
        assertTrue(JournalUiState(draft = "something").canSave)
    }

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
            entries = listOf(JournalEntry(id = 1, rawText = "hello", timestamp = 0L)),
        )
        assertFalse(state.isEmpty)
    }
}
