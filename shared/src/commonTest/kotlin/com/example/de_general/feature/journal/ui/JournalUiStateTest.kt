package com.example.de_general.feature.journal.ui

import com.example.de_general.feature.journal.domain.JournalEntry
import kotlin.test.Test
import kotlin.test.assertEquals
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

    /**
     * A counter, not a flag. Two presses of the pencil button must be two requests, or the
     * second one does nothing after the user has tapped away from the editor.
     */
    @Test
    fun everyComposeRequestIsDistinctFromTheLast() {
        val first = JournalUiState().requestingCompose()
        val second = first.requestingCompose()

        assertEquals(0, JournalUiState().composeRequest)
        assertEquals(1, first.composeRequest)
        assertEquals(2, second.composeRequest)
    }

    /** Nothing else on the state moves when focus is asked for. */
    @Test
    fun askingForFocusDoesNotDisturbTheDraft() {
        val state = JournalUiState(draft = "half a thought", saving = true)

        assertEquals(state.copy(composeRequest = 1), state.requestingCompose())
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
