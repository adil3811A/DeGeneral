package com.example.de_general.feature.chat.ui

import com.example.de_general.feature.chat.domain.ChatMessage
import com.example.de_general.feature.chat.domain.ChatRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatUiStateTest {

    @Test
    fun anEmptyDraftCannotBeSent() {
        assertFalse(ChatUiState(draft = "").canSend)
        assertFalse(ChatUiState(draft = "   \n ").canSend)
    }

    @Test
    fun aMessageAlreadyGoingOutCannotBeSentAgain() {
        assertFalse(ChatUiState(draft = "hello", sending = true).canSend)
    }

    @Test
    fun aDraftWithTextCanBeSent() {
        assertTrue(ChatUiState(draft = "hello").canSend)
    }

    /**
     * The flash-of-empty-state guard, same as the journal's: until the database has answered,
     * "nothing said yet" is not something the app knows, so it must not say it.
     */
    @Test
    fun loadingIsNotTheSameAsEmpty() {
        assertFalse(ChatUiState(loading = true, messages = emptyList()).isEmpty)
        assertTrue(ChatUiState(loading = false, messages = emptyList()).isEmpty)
    }

    @Test
    fun aLoadedConversationWithMessagesIsNotEmpty() {
        val state = ChatUiState(
            loading = false,
            messages = listOf(
                ChatMessage(id = 1, role = ChatRole.User.column, text = "hi", timestamp = 0L),
            ),
        )
        assertFalse(state.isEmpty)
    }

    /**
     * The context chip reports what was found, never what was asked for.
     *
     * A chip that said "Context: 3 recent entries" to someone with one entry would be a number the
     * app did not measure, which is the thing `docs/LOCAL_AI.md` exists to prevent.
     */
    @Test
    fun theContextChipCountsWhatIsActuallyThere() {
        assertEquals("No entries yet", ChatUiState(contextEntryCount = 0).contextLabel)
        assertEquals("Context: 1 recent entry", ChatUiState(contextEntryCount = 1).contextLabel)
        assertEquals("Context: 3 recent entries", ChatUiState(contextEntryCount = 3).contextLabel)
    }

    /**
     * The engine really is absent, and the default says so.
     *
     * If this ever starts defaulting to `true`, the screen would stop telling the user why no
     * reply arrives.
     */
    @Test
    fun thereIsNoEngineUnlessOneIsSupplied() {
        assertFalse(ChatUiState().engineAvailable)
    }
}
