package com.adll.de_general.feature.journal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adll.de_general.feature.journal.data.JournalRepository
import com.adll.de_general.feature.journal.domain.DateAnchor
import com.adll.de_general.feature.journal.domain.JournalEntry
import com.adll.de_general.feature.journal.domain.anchorDate
import com.adll.de_general.feature.journal.domain.anchorFor
import com.adll.de_general.feature.journal.domain.canAnchorTo
import com.adll.de_general.feature.journal.domain.entriesOn
import com.adll.de_general.feature.journal.domain.formatDay
import com.adll.de_general.feature.journal.domain.pickerMillisToDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * What the journal archive draws.
 *
 * Read-only since the composer moved to its own screen: there is no draft here any more, and no
 * save. Writing happens behind the pencil button, in
 * [com.adll.de_general.feature.journal.ui.CreateJournalUiState].
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

    /**
     * Which day the archive is narrowed to. Null is "All", the default.
     *
     * The composer's own [DateAnchor], because Today, Yesterday and a picked date mean exactly the
     * same thing in both places — see [entriesOn].
     */
    val filter: DateAnchor? = null,

    /** The day picker is open. Held here so it survives rotation, like the composer's. */
    val pickingDate: Boolean = false,
) {
    /**
     * Distinct from "no entries loaded yet".
     *
     * [loading] starts true and only flips once the database has actually answered, so the empty
     * state never flashes at someone who has fifty entries.
     */
    val isEmpty: Boolean get() = !loading && entries.isEmpty()

    /** What the list draws: [entries] narrowed to [filter]'s day. */
    val visibleEntries: List<JournalEntry> get() = entriesOn(entries, filter, nowMillis, zone)

    /**
     * There are entries, just none on the filtered day. Distinct from [isEmpty], which means
     * nothing has been written at all — the two get different words and only this one offers a
     * way back to the full list.
     */
    val isFilteredEmpty: Boolean
        get() = !loading && entries.isNotEmpty() && visibleEntries.isEmpty()

    /** What the list says when [isFilteredEmpty]. Null when showing all. */
    val filteredEmptyMessage: String?
        get() = when (val current = filter) {
            null -> null
            DateAnchor.Today -> "Nothing written today."
            DateAnchor.Yesterday -> "Nothing written yesterday."
            is DateAnchor.On -> "Nothing written on ${formatDay(current.date)}."
        }

    /** Where the picker opens: the filtered day, or today when showing all. */
    val pickerDate: LocalDate
        get() = anchorDate(filter ?: DateAnchor.Today, nowMillis, zone)
}

/**
 * Drives the journal archive.
 *
 * Shaped like [com.adll.de_general.feature.onboarding.ui.OnboardingViewModel]: one
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

    /**
     * Narrows the archive to one day, or back to all of it with null.
     *
     * Refreshes the clock and zone as it goes: "Today" has to mean today *now*, not today as of the
     * last database write, or a journal left open past midnight would filter to the wrong day.
     */
    fun onFilterChange(filter: DateAnchor?) {
        _uiState.update {
            it.copy(filter = filter, pickingDate = false, nowMillis = now(), zone = zone())
        }
    }

    fun openDatePicker() {
        _uiState.update { it.copy(pickingDate = true, nowMillis = now(), zone = zone()) }
    }

    fun dismissDatePicker() {
        _uiState.update { it.copy(pickingDate = false) }
    }

    /**
     * A day from the Material picker, whose value is **UTC midnight** of that day — converted in
     * `JournalDates.kt`, never read as a local instant here.
     *
     * Picking today or yesterday selects that pill rather than a third state meaning the same
     * thing ([anchorFor]). A future day is ignored: the picker already refuses one, and this is
     * the belt to its braces.
     */
    fun onDatePicked(pickerMillis: Long?) {
        val moment = now()
        val timeZone = zone()
        val date = pickerMillis?.let(::pickerMillisToDate)
        if (date == null || !canAnchorTo(date, moment, timeZone)) {
            dismissDatePicker()
            return
        }
        onFilterChange(anchorFor(date, moment, timeZone))
    }
}
