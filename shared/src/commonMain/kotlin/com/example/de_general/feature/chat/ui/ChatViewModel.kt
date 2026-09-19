package com.example.de_general.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.de_general.core.ai.LlmEngine
import com.example.de_general.feature.chat.data.ChatRepository
import com.example.de_general.feature.chat.domain.ChatMessage
import com.example.de_general.feature.chat.domain.buildPrompt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the Chat screen draws.
 *
 * [modelLabel] and [contextLabel] are strings the view model derived from things the app actually
 * knows — the pinned [com.example.de_general.feature.onboarding.domain.ModelSpec] and a real count
 * of entries. Nothing here is a placeholder figure.
 */
data class ChatUiState(
    /** True until the first emission from the database arrives. */
    val loading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,

    /** e.g. "Gemma 3 1B · Q4_K_M". */
    val modelLabel: String = "",

    /** How many journal entries were actually found for context. */
    val contextEntryCount: Int = 0,

    /**
     * Whether anything can run the model. False on every build today.
     *
     * @see com.example.de_general.di.AppContainer.llmEngine
     */
    val engineAvailable: Boolean = false,

    /**
     * A one-off message for the user — "no reply yet", "coming soon".
     *
     * Deliberately transient state rather than a database row: a notice is about this moment, not
     * about the conversation, and persisting them would fill the transcript with apologies.
     */
    val notice: String? = null,
) {
    val canSend: Boolean get() = draft.isNotBlank() && !sending

    /**
     * Distinct from "no messages loaded yet".
     *
     * [loading] starts true and only flips once the database has answered, so the empty state
     * never flashes at someone with a long conversation.
     */
    val isEmpty: Boolean get() = !loading && messages.isEmpty()

    /** What the context chip says. Reports what was found, never what was asked for. */
    val contextLabel: String
        get() = when (contextEntryCount) {
            0 -> "No entries yet"
            1 -> "Context: 1 recent entry"
            else -> "Context: $contextEntryCount recent entries"
        }
}

/**
 * Drives the chat screen.
 *
 * Shaped like [com.example.de_general.feature.journal.ui.JournalViewModel]: one [StateFlow] of one
 * immutable state, methods the screen reaches only as callbacks, no repository and no
 * `NavController` visible to the screen.
 *
 * [engine] is nullable because there is no inference implementation in this app. When it is null —
 * which is always, today — sending records what the person wrote and then says plainly that no
 * reply is coming. It does not fabricate one, and it does not show a typing indicator for a model
 * that is not running.
 */
class ChatViewModel(
    private val repository: ChatRepository,
    private val engine: LlmEngine?,
    modelLabel: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(modelLabel = modelLabel, engineAvailable = engine != null),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages, loading = false) }
            }
        }
        refreshContextCount()
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
        viewModelScope.launch {
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

    /** "Save insight" — writes [message] into the journal through the lambda `di` supplied. */
    fun saveInsight(message: ChatMessage) {
        viewModelScope.launch {
            try {
                repository.saveAsJournalEntry(message.text)
                _uiState.update { it.copy(notice = "Saved to your journal.") }
                refreshContextCount()
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
     * Asks the model, if there is one.
     *
     * The prompt is built either way. That is not wasted work: it is the part of this pipeline
     * that `commonTest` can prove correct before an engine exists, and building it here means the
     * day one arrives, this function's only change is that the `if` stops being taken.
     */
    private suspend fun generateReply() {
        val context = repository.journalContext()
        val prompt = buildPrompt(context, repository.messages())

        if (engine == null) {
            _uiState.update { it.copy(sending = false, notice = NO_ENGINE_NOTICE) }
            return
        }

        val reply = StringBuilder()
        engine.generate(prompt).collect { token -> reply.append(token) }
        repository.recordModelMessage(reply.toString())
        _uiState.update { it.copy(sending = false) }
    }

    private fun refreshContextCount() {
        viewModelScope.launch {
            val count = runCatching { repository.journalContext().size }.getOrDefault(0)
            _uiState.update { it.copy(contextEntryCount = count) }
        }
    }
}

/**
 * What the screen says when there is no engine.
 *
 * Names the actual reason rather than a vague failure. The model really is downloaded and really
 * is verified — the missing piece is the runtime, and a user who paid for 806 MB of download
 * deserves to know which half is done.
 */
internal const val NO_ENGINE_NOTICE: String =
    "No reply yet — on-device generation isn't wired up. The model is downloaded and verified, " +
        "but nothing can run it yet."

/** The follow-up "Reflect deeper" sends. A real message, not a hidden instruction. */
internal const val REFLECT_DEEPER_PROMPT: String =
    "Can you take that a little deeper?"
