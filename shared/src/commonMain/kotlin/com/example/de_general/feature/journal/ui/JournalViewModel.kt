package com.example.de_general.feature.journal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.de_general.feature.journal.data.JournalRepository
import com.example.de_general.feature.journal.domain.JournalEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JournalUiState(
    /** True until the first emission from the database arrives. */
    val loading: Boolean = true,
    val entries: List<JournalEntry> = emptyList(),
    val draft: String = "",
    val saving: Boolean = false,
    val errorMessage: String? = null,
    /**
     * How many times something has asked the editor to take focus.
     *
     * A counter rather than a flag: the bottom bar's pencil button can be pressed twice in a
     * row, and a `Boolean` that is already `true` would swallow the second press. The screen
     * watches the number change; the value itself means nothing.
     */
    val composeRequest: Int = 0,
) {
    val canSave: Boolean get() = draft.isNotBlank() && !saving

    /**
     * Distinct from "no entries loaded yet".
     *
     * [loading] starts true and only flips once the database has actually answered, so the empty
     * state never flashes at someone who has fifty entries.
     */
    val isEmpty: Boolean get() = !loading && entries.isEmpty()

    /** See [composeRequest]. A pure step, so `commonTest` can pin it without a view model. */
    fun requestingCompose(): JournalUiState = copy(composeRequest = composeRequest + 1)
}

/**
 * Drives the journal screen.
 *
 * Shaped like [com.example.de_general.feature.onboarding.ui.OnboardingViewModel]: one
 * [StateFlow] of one immutable state, methods that the screen reaches only as callbacks. The
 * screen never sees the repository and never sees a `NavController`.
 *
 * Scoped to the main nav *graph* entry rather than the journal destination, so an unsaved draft
 * survives navigating to a sibling screen and back.
 */
class JournalViewModel(
    private val repository: JournalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeEntries().collect { entries ->
                _uiState.update { it.copy(entries = entries, loading = false) }
            }
        }
    }

    fun onDraftChange(text: String) {
        _uiState.update { it.copy(draft = text) }
    }

    /**
     * Ask the editor for focus — what the bottom bar's pencil button does.
     *
     * The request goes through state rather than an event channel because the button and the
     * editor are not in the same composition: the bar lives beside the `NavHost`, the editor
     * inside it. Both reach this view model through the main graph's back stack entry.
     */
    fun requestCompose() {
        _uiState.update { it.requestingCompose() }
    }

    /**
     * Writes the draft.
     *
     * The failure is surfaced, never swallowed: a write that did not land must not look like one
     * that did, and the draft is only cleared once the row is actually in.
     */
    fun saveDraft() {
        val state = _uiState.value
        if (!state.canSave) return

        _uiState.update { it.copy(saving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.write(state.draft)
                _uiState.update { it.copy(draft = "", saving = false) }
            } catch (cancellation: CancellationException) {
                // The scope is going away; there is no one left to show a message to.
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(
                        saving = false,
                        errorMessage = failure.message ?: "That entry could not be saved.",
                    )
                }
            }
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            try {
                repository.delete(entry)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(errorMessage = failure.message ?: "That entry could not be deleted.")
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
