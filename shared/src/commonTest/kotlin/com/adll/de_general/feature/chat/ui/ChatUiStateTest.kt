package com.adll.de_general.feature.chat.ui

import com.adll.de_general.core.ai.EngineState
import com.adll.de_general.feature.chat.domain.ChatMessage
import com.adll.de_general.feature.chat.domain.ChatRole
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

    /** The clear button has something to clear, or it is disabled. */
    @Test
    fun anEmptyConversationCannotBeCleared() {
        assertFalse(ChatUiState(loading = false).canClear)
    }

    @Test
    fun storedMessagesCanBeCleared() {
        val state = ChatUiState(
            messages = listOf(
                ChatMessage(id = 1, role = ChatRole.User.column, text = "hi", timestamp = 0L),
            ),
        )
        assertTrue(state.canClear)
    }

    /** A reply still being written counts — clearing is also how someone stops one. */
    @Test
    fun aReplyInProgressCanBeCleared() {
        assertTrue(ChatUiState(streamingReply = "It takes").canClear)
    }

    @Test
    fun theClearDialogStartsClosed() {
        assertFalse(ChatUiState().confirmingClear)
    }

    /** Nothing generates until the engine says it is ready — not while it is still loading. */
    @Test
    fun onlyAReadyEngineCanGenerate() {
        assertFalse(ChatUiState(engine = EngineState.Idle).canGenerate)
        assertFalse(ChatUiState(engine = EngineState.Loading).canGenerate)
        assertFalse(ChatUiState(engine = EngineState.Failed("boom")).canGenerate)
        assertTrue(ChatUiState(engine = EngineState.Ready(loadMillis = 1_200)).canGenerate)
    }

    /**
     * The chip reports the engine's own state, and the load time is the measured one.
     *
     * `docs/LOCAL_AI.md` struck an invented speed from the design; the rule is that a number on
     * screen came from a measurement. This pins that the label carries the engine's figure rather
     * than one composed here.
     */
    @Test
    fun theEngineLabelReportsWhatTheEngineSaid() {
        assertEquals("not loaded", ChatUiState(engine = EngineState.Idle).engineLabel)
        assertEquals("loading…", ChatUiState(engine = EngineState.Loading).engineLabel)
        assertEquals(
            "ready in 1234 ms",
            ChatUiState(engine = EngineState.Ready(loadMillis = 1234)).engineLabel,
        )
        assertEquals("unavailable", ChatUiState(engine = EngineState.Failed("x")).engineLabel)
    }

    /**
     * A reply being streamed is not an empty conversation.
     *
     * Without this the empty state would flash over the first answer as it arrives, because the
     * first token lands before any row is written.
     */
    @Test
    fun aStreamingReplyMeansTheScreenIsNotEmpty() {
        val state = ChatUiState(loading = false, messages = emptyList(), streamingReply = "It ta")

        assertFalse(state.isEmpty)
    }
}
