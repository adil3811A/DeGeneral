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
import kotlinx.datetime.TimeZone

/**
 * What the journal archive draws.
 *
 * Read-only since the composer moved to its own screen: there is no draft here any more, and no
 * save. Writing happens behind the pencil button, in
 * [com.example.de_general.feature.journal.ui.CreateJournalUiState].
 */
data class JournalUiState(
    /** True until the first emission from the database arrives. */
    val loading: Boolean = true,
    val entries: List<JournalEntry> = emptyList(),

    /**
     * The clock and zone the row labels are read against.
     *
     * Carried on the state rather than read inside the screen, for the reason the rest of this app
     * injects its clock: a composable that calls a wall clock cannot be previewed at a known date
     * and cannot be tested at all. Refreshed on every emission from the database, which is often
     * enough that a journal left open across midnight relabels itself on the next write.
     */
    val nowMillis: Long = 0L,
    val zone: TimeZone = TimeZone.UTC,

    val errorMessage: String? = null,
) {
    /**
     * Distinct from "no entries loaded yet".
     *
     * [loading] starts true and only flips once the database has actually answered, so the empty
     * state never flashes at someone who has fifty entries.
     */
    val isEmpty: Boolean get() = !loading && entries.isEmpty()
}

/**
 * Drives the journal archive.
 *
 * Shaped like [com.example.de_general.feature.onboarding.ui.OnboardingViewModel]: one
 * [StateFlow] of one immutable state, methods that the screen reaches only as callbacks. The
 * screen never sees the repository and never sees a `NavController`.
 *
 * Scoped to the main nav *graph* entry rather than the journal destination, so a delete in flight
 * finishes even if the user taps away to a sibling tab on the way.
 */
class JournalViewModel(
    private val repository: JournalRepository,
    private val now: () -> Long,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeEntries().collect { entries ->
                _uiState.update {
                    it.copy(
                        entries = entries,
                        loading = false,
                        nowMillis = now(),
                        zone = zone(),
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
