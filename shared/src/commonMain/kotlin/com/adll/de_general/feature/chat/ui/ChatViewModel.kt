package com.adll.de_general.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adll.de_general.core.ai.EngineState
import com.adll.de_general.core.ai.LlmEngine
import com.adll.de_general.feature.chat.data.ChatRepository
import com.adll.de_general.feature.chat.domain.ChatMessage
import com.adll.de_general.feature.chat.domain.buildPrompt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path
import kotlin.time.TimeSource

/**
 * What the Chat screen draws.
 *
 * [modelLabel] is derived from something the app actually knows — the pinned
 * [com.adll.de_general.feature.onboarding.domain.ModelSpec]. Nothing here is a placeholder
 * figure.
 */
data class ChatUiState(
    /** True until the first emission from the database arrives. */
    val loading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,

    /** e.g. "Gemma 3 1B · Q4_K_M". */
    val modelLabel: String = "",

    /** What the model is doing, straight from the engine. Never inferred here. */
    val engine: EngineState = EngineState.Idle,

    /**
     * The reply as it is being produced, token by token.
     *
     * Not a database row until it is finished: a half-written answer is a thing happening now, not
     * part of the transcript. If generation is cancelled, what arrived is kept and stored.
     */
    val streamingReply: String? = null,

    /**
     * A one-off message for the user — "no reply yet", "coming soon".
     *
     * Deliberately transient state rather than a database row: a notice is about this moment, not
     * about the conversation, and persisting them would fill the transcript with apologies.
     */
    val notice: String? = null,

    /**
     * The "Clear the whole conversation?" dialog is up. Held here rather than in the screen so it
     * survives rotation, like the composer's discard dialog.
     */
    val confirmingClear: Boolean = false,
) {
    val canSend: Boolean get() = draft.isNotBlank() && !sending

    /** The engine is loaded and idle, so a prompt would actually go somewhere. */
    val canGenerate: Boolean get() = engine is EngineState.Ready

    /**
     * Distinct from "no messages loaded yet".
     *
     * [loading] starts true and only flips once the database has answered, so the empty state
     * never flashes at someone with a long conversation.
     */
    val isEmpty: Boolean get() = !loading && messages.isEmpty() && streamingReply == null

    /**
     * What the status chip says about the engine, after the model name.
     *
     * Every branch reports something the engine knows. The load time is measured across the actual
     * call, which is what `docs/LOCAL_AI.md` asks for before a number goes on screen.
     */
    val engineLabel: String
        get() = when (val current = engine) {
            EngineState.Idle -> "not loaded"
            EngineState.Loading -> "loading…"
            is EngineState.Ready -> "ready in ${current.loadMillis} ms"
            is EngineState.Failed -> "unavailable"
        }

    /**
     * There is something to clear — a stored message, or a reply still being written. The clear
     * button is disabled otherwise, rather than offering to delete nothing.
     */
    val canClear: Boolean get() = messages.isNotEmpty() || streamingReply != null
}

/**
 * Drives the chat screen.
 *
 * Shaped like [com.adll.de_general.feature.journal.ui.JournalViewModel]: one [StateFlow] of one
 * immutable state, methods the screen reaches only as callbacks, no repository and no
 * `NavController` visible to the screen.
 *
 * The screen owns the engine's lifetime through [loadEngine]/[unloadEngine], called as the Chat
 * tab is entered and left, so ~770 MB of weights is only resident while there is a conversation
 * on screen.
 */
class ChatViewModel(
    private val repository: ChatRepository,
    private val engine: LlmEngine,
    private val modelPath: Path,
    modelLabel: String,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(modelLabel = modelLabel))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages, loading = false) }
            }
        }
        viewModelScope.launch {
            engine.state.collect { state ->
                _uiState.update { it.copy(engine = state) }
            }
        }
    }

    /**
     * The send in flight — the person's row being written, then the reply being generated.
     * Held so [confirmClear] can stop a reply mid-stream instead of letting it land in a
     * conversation that was just cleared.
     */
    private var replyJob: Job? = null

    /** Called as the Chat tab appears. Safe to call repeatedly; loading twice is a no-op. */
    fun loadEngine() {
        viewModelScope.launch { engine.load(modelPath) }
    }

    /**
     * Called as the Chat tab goes away.
     *
     * Runs on [viewModelScope] precisely because this view model is scoped to the whole main
     * graph: it is still alive after the screen is gone, so the unload actually completes. A scope
     * remembered in the composable would be cancelled on the way out and leave the weights
     * resident.
     */
    fun unloadEngine() {
        viewModelScope.launch { engine.unload() }
    }

    fun onDraftChange(text: String) {
        _uiState.update { it.copy(draft = text) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    /** The + button. Attachments are not built; saying so is the whole behaviour. */
    fun onAttach() {
        _uiState.update { it.copy(notice = "Attachments are coming soon.") }
    }

    /** Sends the draft and clears the composer. */
    fun send() {
        val state = _uiState.value
        if (!state.canSend) return
        _uiState.update { it.copy(draft = "") }
        dispatch(state.draft)
    }

    /**
     * "Reflect deeper" — a real follow-up message, worded and sent as the person.
     *
     * Deliberately does not go through the draft: someone can press this with half a thought
     * already typed, and throwing their words away to make room would be rude.
     */
    fun reflectDeeper() {
        if (_uiState.value.sending) return
        dispatch(REFLECT_DEEPER_PROMPT)
    }

    /**
     * Records [text] as the person's turn, then tries to answer it.
     *
     * The write happens first and unconditionally — what was said is worth keeping whether or not
     * anything can reply to it.
     */
    private fun dispatch(text: String) {
        if (text.isBlank()) return
        _uiState.update { it.copy(sending = true, notice = null) }
        replyJob = viewModelScope.launch {
            try {
                repository.sendUserMessage(text)
                generateReply()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(
                        sending = false,
                        notice = failure.message ?: "That message could not be saved.",
                    )
                }
            }
        }
    }

    /** The trash button. Asks first — see [confirmClear]. */
    fun requestClear() {
        if (!_uiState.value.canClear) return
        _uiState.update { it.copy(confirmingClear = true) }
    }

    fun cancelClear() {
        _uiState.update { it.copy(confirmingClear = false) }
    }

    /**
     * Deletes every chat message. Journal entries are untouched — including ones saved from this
     * chat with "Save insight", which are journal rows, not chat rows.
     *
     * A reply still streaming is stopped **and joined** before the delete. Its `finally` in
     * [generateReply] may record the partial answer on the way out; joining first means the delete
     * runs after that write and takes it too, so no half-answer survives the clear.
     *
     * The draft is kept. Clearing the history is not clearing a thought someone is halfway
     * through typing.
     */
    fun confirmClear() {
        _uiState.update { it.copy(confirmingClear = false) }
        viewModelScope.launch {
            try {
                replyJob?.cancelAndJoin()
                replyJob = null
                repository.clear()
                _uiState.update { it.copy(sending = false, streamingReply = null, notice = null) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(notice = failure.message ?: "The conversation could not be cleared.")
                }
            }
        }
    }

    /** "Save insight" — writes [message] into the journal through the lambda `di` supplied. */
    fun saveInsight(message: ChatMessage) {
        viewModelScope.launch {
            try {
                repository.saveAsJournalEntry(message.text)
                _uiState.update { it.copy(notice = "Saved to your journal.") }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(notice = failure.message ?: "That could not be saved.")
                }
            }
        }
    }

    /**
     * Asks the model, and streams what comes back.
     *
     * Tokens land in [ChatUiState.streamingReply] as they arrive and become a database row only
     * once generation ends — including when it ends early. A reply that was cut off is still
     * something the model said.
     *
     * The speed figure is measured here, across the real generation, by counting emissions. It is
     * approximate in one honest way: the engine emits *deltas*, which are usually one token but
     * are not guaranteed to be, so this is "chunks per second" wearing a tokens/sec label. Close
     * enough to be useful, and far closer than the estimate `docs/LOCAL_AI.md` refused to ship.
     */
    private suspend fun generateReply() {
        val context = repository.journalContext()
        val prompt = buildPrompt(context, repository.messages())

        if (!_uiState.value.canGenerate) {
            _uiState.update { it.copy(sending = false, notice = engineNotice()) }
            return
        }

        val reply = StringBuilder()
        var tokens = 0
        val started = timeSource.markNow()

        try {
            engine.generate(prompt).collect { delta ->
                tokens++
                reply.append(delta)
                _uiState.update { it.copy(streamingReply = reply.toString()) }
            }
        } finally {
            val elapsed = started.elapsedNow()
            _uiState.update { it.copy(sending = false, streamingReply = null) }
            if (reply.isNotBlank()) {
                val seconds = elapsed.inWholeMilliseconds / 1000.0
                repository.recordModelMessage(
                    text = reply.toString(),
                    // No timing for a reply that arrived in under a millisecond — dividing by
                    // zero-ish gives a number that says nothing true.
                    tokensPerSecond = if (seconds > 0.0) tokens / seconds else null,
                    generationMillis = elapsed.inWholeMilliseconds,
                )
            }
        }
    }

    /** Why no reply is coming, in the engine's own words where it has any. */
    private fun engineNotice(): String = when (val current = _uiState.value.engine) {
        is EngineState.Failed -> current.message
        EngineState.Loading -> "Still loading the model — try again in a moment."
        else -> NO_ENGINE_NOTICE
    }

}

/**
 * What the screen says when the model is not ready and has given no reason of its own.
 *
 * Names the state rather than apologising vaguely: someone who waited for 806 MB of download
 * deserves to know which half of the pipeline is not working.
 */
internal const val NO_ENGINE_NOTICE: String =
    "The model isn't loaded, so there's no reply yet."

/** The follow-up "Reflect deeper" sends. A real message, not a hidden instruction. */
internal const val REFLECT_DEEPER_PROMPT: String =
    "Can you take that a little deeper?"
