package com.adll.de_general.feature.journal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adll.de_general.core.ai.EngineState
import com.adll.de_general.core.ai.LlmEngine
import com.adll.de_general.feature.journal.data.JournalRepository
import com.adll.de_general.feature.journal.domain.DateAnchor
import com.adll.de_general.feature.journal.domain.JournalMood
import com.adll.de_general.feature.journal.domain.addTags
import com.adll.de_general.feature.journal.domain.anchorDate
import com.adll.de_general.feature.journal.domain.anchorFor
import com.adll.de_general.feature.journal.domain.buildPolishPrompt
import com.adll.de_general.feature.journal.domain.buildTitlePrompt
import com.adll.de_general.feature.journal.domain.canAnchorTo
import com.adll.de_general.feature.journal.domain.formatDay
import com.adll.de_general.feature.journal.domain.normalizeSuggestedTitle
import com.adll.de_general.feature.journal.domain.pickerMillisToDate
import com.adll.de_general.feature.journal.domain.resolveAnchor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import okio.Path
import kotlin.time.TimeSource

/**
 * Where the local polish pass has got to.
 *
 * Every value is something that actually happened. [Ready] carries a **measured** duration, timed
 * across the real call — the same condition `docs/LOCAL_AI.md` set before the chat screen was
 * allowed a tokens/sec figure.
 */
sealed interface PolishState {

    /** Nothing asked for, or nothing running. */
    data object Idle : PolishState

    /**
     * Reading ~770 MB off disk.
     *
     * A state of its own rather than a spinner, because it is the slow half and the person who
     * tapped Refine deserves to know which half they are waiting on.
     */
    data object Loading : PolishState

    /** Tokens as they arrive. [partial] is the corrected entry so far. */
    data class Running(val partial: String) : PolishState

    /**
     * The entry is corrected; the model is being asked what to call it.
     *
     * A second pass rather than a second field in one answer — see `PolishPrompt.kt`. [text] is the
     * finished correction, carried through so the card keeps showing it instead of blanking while
     * the title is written.
     */
    data class Naming(val text: String) : PolishState

    /**
     * A finished suggestion, and how long both passes took together.
     *
     * [title] is null when the entry already had one — there is no point spending a pass on
     * something that would not be applied — and also when the model produced nothing usable.
     */
    data class Ready(val title: String?, val text: String, val millis: Long) : PolishState

    /** The model could not be loaded, or generation failed. [message] is shown as-is. */
    data class Failed(val message: String) : PolishState
}

/**
 * Enough to put back what "Keep this version" replaced.
 *
 * Holds both halves of the swap: what was there before, and what was written over it. The second
 * pair is what lets [CreateJournalUiState.canUndo] notice a later edit and stop offering an undo
 * that would throw that edit away.
 *
 * Composer-only, and deliberately not persisted. Once Save is pressed the entry is what is in the
 * fields; there is no column holding the pre-refine draft.
 */
data class PolishUndo(
    val previousTitle: String,
    val previousBody: String,
    val appliedTitle: String,
    val appliedBody: String,
)

/**
 * What the Create Journal screen draws.
 *
 * One immutable data class; everything the screen needs to *say* is a derived `val` here rather
 * than a `when` inside a composable, so `CreateJournalUiStateTest` pins the copy as pure logic.
 * That is not tidiness: several of these strings exist specifically to replace a claim the Stitch
 * design made that this app cannot make, and a test is the only thing that stops a well-meaning
 * edit putting the claim back.
 */
data class CreateJournalUiState(
    /**
     * The back-stack entry this draft belongs to. See [CreateJournalViewModel.startComposing] —
     * it is what tells a fresh composer apart from a rotation.
     */
    val sessionKey: String? = null,

    val title: String = "",
    val body: String = "",
    val mood: JournalMood? = null,
    val anchor: DateAnchor = DateAnchor.Today,
    val tags: List<String> = emptyList(),

    /** The tag being typed, before it becomes a chip. */
    val tagDraft: String = "",

    /** The clock and zone the date labels are computed against. Both set by `startComposing`. */
    val nowMillis: Long = 0L,
    val zone: TimeZone = TimeZone.UTC,

    val saving: Boolean = false,

    /**
     * The screen has done its job and should be popped.
     *
     * A plain Boolean, not a counter like `JournalUiState.composeRequest`: that was a counter
     * because sender and receiver sat in different compositions and the request could genuinely
     * repeat. Here the view model and the destination are the same composition, and "close" happens
     * once.
     */
    val finished: Boolean = false,

    val confirmingDiscard: Boolean = false,
    val pickingDate: Boolean = false,
    val errorMessage: String? = null,

    val polish: PolishState = PolishState.Idle,

    /** What the last accepted suggestion replaced, while putting it back is still safe. */
    val undo: PolishUndo? = null,

    /** e.g. "Gemma 3 1B · Q4_K_M", derived from the pinned spec by the graph. */
    val modelLabel: String = "",
) {

    /** An entry is its body. A title alone is a label for nothing. */
    val canSave: Boolean get() = body.isNotBlank() && !saving

    /** Whether closing would throw anything away. */
    val hasDraft: Boolean
        get() = title.isNotBlank() || body.isNotBlank() || tags.isNotEmpty() ||
            tagDraft.isNotBlank() || mood != null || anchor != DateAnchor.Today

    val polishing: Boolean
        get() = polish is PolishState.Loading ||
            polish is PolishState.Running ||
            polish is PolishState.Naming

    val canRefine: Boolean get() = body.isNotBlank() && !polishing && !saving

    /**
     * Whether the last "Keep this version" can still be put back.
     *
     * Requires that **neither** field has been touched since. An undo that also silently discarded
     * a sentence written after accepting would be a worse trap than no undo at all, so the offer
     * simply goes away the moment it stops being a pure reversal. Pure, so `commonTest` pins it.
     */
    val canUndo: Boolean
        get() = undo != null && title == undo.appliedTitle && body == undo.appliedBody

    /** What the undo row says. Names what was replaced, because the swap is not reversible later. */
    val undoLabel: String get() = "Your entry was replaced with the refined version."

    /**
     * Words, counted.
     *
     * The design also promised "1 min read". That is arithmetic about a reader nobody timed, so it
     * is not here. This number the app counted itself.
     */
    val wordCount: Int get() = body.split(' ', '\n', '\t', '\r').count { it.isNotBlank() }

    val wordCountLabel: String get() = if (wordCount == 1) "1 word" else "$wordCount words"

    /** The date the entry will be filed under, spelled out. Never a weekday copied from a mock. */
    val dateLabel: String get() = formatDay(anchorDate(anchor, nowMillis, zone))

    /**
     * Why the card does not print a time.
     *
     * The design shows one, but the only honest time to show is the one Save will stamp, and that
     * has not happened yet. A time captured when the composer opened would be minutes stale by the
     * time anyone read it back — a number the app did not measure, wearing the clothes of one.
     */
    val dateCaption: String get() = "Saved with the time of day you press Save."

    val isBackdated: Boolean get() = anchor != DateAnchor.Today

    /** What the Refine button says. Two honest labels, rather than one spinner. */
    val refineLabel: String
        get() = when (polish) {
            PolishState.Loading -> "Loading the model…"
            is PolishState.Running -> "Refining…"
            is PolishState.Naming -> "Naming it…"
            is PolishState.Failed -> "Try refining again"
            else -> "Refine grammar & spelling"
        }

    /** "Refined in 1240 ms" — measured across the real call, or null when nothing has run. */
    val refinedInLabel: String?
        get() = (polish as? PolishState.Ready)?.let { "Refined in ${it.millis} ms" }

    /**
     * The truncation warning, shown with every suggestion.
     *
     * `LlamatikEngine` caps generation at 512 tokens and `LlmEngine.generate` has no per-call
     * limit, so a long entry's corrected version **will** stop at the cap with nothing to mark it.
     * A length heuristic here would be a guess dressed as a check; saying plainly what the limit is
     * and asking the reader to look is the honest version. The real fix is a per-call cap on the
     * `core` seam, which is its own piece of work.
     */
    val refineCaption: String get() = "The model stops after a fixed length — check the end before you keep it."

    /** The privacy badge. Not "encrypted": this app encrypts nothing of its own. */
    val privacyLabel: String get() = "Stays on this device"
}

/**
 * Drives the Create Journal screen.
 *
 * Shaped like [JournalViewModel] and [com.adll.de_general.feature.chat.ui.ChatViewModel]: one
 * [StateFlow] of one immutable state, methods the screen reaches only as callbacks, no repository
 * and no `NavController` visible to the screen.
 *
 * **Engine lifetime: Chat owns the model, this screen borrows it.** Navigating here disposes the
 * Chat destination, so Chat's `DisposableEffect` has already fired `unloadEngine()`. This view
 * model therefore loads for itself and **never unloads** — a composer that unloaded on the way out
 * would be the thing that rips the weights out from under Chat. Two costs, stated rather than
 * hidden: Chat → pencil → Refine pays the load twice, and polishing once leaves ~770 MB resident
 * until the process dies.
 *
 * **Loading is lazy, on the first Refine.** Chat loads eagerly because the model *is* that screen.
 * A composer is for writing: most entries will never be polished, and seconds of disk I/O plus
 * 770 MB on every tap of the pencil would compete with exactly the responsiveness a text editor
 * needs.
 *
 * One residual race worth naming: a Refine that loses the ordering to a transition's unload fails
 * with the engine's own "The model is not loaded.", the button reads "Try refining again", and
 * nothing is lost. `LlamaBridge` is a global `expect object`; the mutex now held across
 * [LlmEngine.generate] closes the window this app can close, but a Llamatik-internal one may
 * remain and is not visible from here.
 */
class CreateJournalViewModel(
    private val repository: JournalRepository,
    private val engine: LlmEngine,
    private val modelPath: Path,
    modelLabel: String,
    private val now: () -> Long,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateJournalUiState(modelLabel = modelLabel))
    val uiState: StateFlow<CreateJournalUiState> = _uiState.asStateFlow()

    private var polishJob: Job? = null

    /**
     * Begin (or resume) a draft for the back-stack entry [sessionKey].
     *
     * Keyed on the entry's id rather than reset in a `DisposableEffect`'s `onDispose`, and that is
     * the difference between a working screen and silent data loss: `onDispose` also fires on
     * **rotation**. For Chat's engine that is merely wasteful; for a half-written journal entry it
     * would throw the entry away. Calling this twice with the same id is a no-op, which is exactly
     * what a rotation does.
     */
    fun startComposing(sessionKey: String) {
        if (_uiState.value.sessionKey == sessionKey) return
        polishJob?.cancel()
        polishJob = null
        _uiState.value = CreateJournalUiState(
            sessionKey = sessionKey,
            modelLabel = _uiState.value.modelLabel,
            nowMillis = now(),
            zone = zone(),
        )
    }

    fun onTitleChange(text: String) {
        _uiState.update { it.copy(title = text) }
    }

    /**
     * The body changed.
     *
     * A polish in flight is cancelled: it was a correction of text that no longer exists, and
     * letting it run is thirty seconds of CPU for an answer nobody can use.
     *
     * An accepted one is left alone here. [CreateJournalUiState.canUndo] already notices the edit
     * and withdraws the offer, and discarding the record outright would mean typing one character
     * after accepting made the swap unreversible with no warning.
     */
    fun onBodyChange(text: String) {
        // Unconditionally, not "if something is running": a job that has been launched but has not
        // been dispatched yet still reports [PolishState.Idle], and would go on to correct text
        // that no longer exists.
        cancelPolish()
        _uiState.update { it.copy(body = text) }
    }

    /** Clears the body and nothing else. The title, mood, date and tags are separate decisions. */
    fun clearBody() {
        cancelPolish()
        _uiState.update { it.copy(body = "") }
    }

    /** Tapping the selected mood clears it — there is no "no mood" chip to have to find. */
    fun onMoodChange(mood: JournalMood) {
        _uiState.update { it.copy(mood = if (it.mood == mood) null else mood) }
    }

    /** Today / Yesterday. [nowMillis] is refreshed so a draft open across midnight still agrees. */
    fun onAnchorChange(anchor: DateAnchor) {
        _uiState.update { it.copy(anchor = anchor, nowMillis = now(), pickingDate = false) }
    }

    fun openDatePicker() {
        _uiState.update { it.copy(pickingDate = true, nowMillis = now()) }
    }

    fun dismissDatePicker() {
        _uiState.update { it.copy(pickingDate = false) }
    }

    /**
     * A date chosen in the Material picker, whose `selectedDateMillis` is **UTC midnight** of that
     * day — converted in `JournalDates.kt`, never treated as a local instant here.
     *
     * A future date is ignored rather than reported: the picker's own `selectableDates` already
     * refuses one, so reaching this branch means something else went wrong and an error message
     * about it would be noise.
     */
    fun onDatePicked(pickerMillis: Long?) {
        val moment = now()
        val timeZone = zone()
        val date = pickerMillis?.let(::pickerMillisToDate)
        if (date == null || !canAnchorTo(date, moment, timeZone)) {
            dismissDatePicker()
            return
        }
        _uiState.update {
            it.copy(
                anchor = anchorFor(date, moment, timeZone),
                nowMillis = moment,
                pickingDate = false,
            )
        }
    }

    fun onTagDraftChange(text: String) {
        _uiState.update { it.copy(tagDraft = text) }
    }

    /** Turns whatever is in the tag field into chips. Normalising and capping happens in `addTags`. */
    fun commitTagDraft() {
        _uiState.update { it.copy(tags = addTags(it.tags, it.tagDraft), tagDraft = "") }
    }

    fun removeTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
    }

    /**
     * Ask the local model to correct spelling, grammar and punctuation, then to name the result.
     *
     * Two passes, not two fields in one answer — `PolishPrompt.kt` says why. The naming pass runs
     * only when the title field is empty: spending a second trip through 770 MB of weights on a
     * title that [acceptPolish] would not apply is pure waste, and it would also imply the app
     * intends to overwrite something the person typed.
     *
     * The title pass is allowed to fail on its own. A title is a nicety; the correction is the
     * thing that was actually asked for, and losing the second must not cost the first.
     *
     * Loads the engine first if it is not already loaded — see the class KDoc for why that is lazy
     * and why nothing here ever unloads.
     */
    fun refine() {
        val state = _uiState.value
        if (!state.canRefine) return

        val prompt = buildPolishPrompt(state.body)
        if (prompt.isEmpty()) return
        val wantsTitle = state.title.isBlank()

        polishJob?.cancel()
        polishJob = viewModelScope.launch {
            try {
                if (engine.state.value !is EngineState.Ready) {
                    _uiState.update { it.copy(polish = PolishState.Loading) }
                    engine.load(modelPath)
                }
                if (engine.state.value !is EngineState.Ready) {
                    _uiState.update { it.copy(polish = PolishState.Failed(engineMessage())) }
                    return@launch
                }

                // Timed across both passes, because both are what the person waited for.
                val started = timeSource.markNow()

                val suggestion = StringBuilder()
                _uiState.update { it.copy(polish = PolishState.Running("")) }
                engine.generate(prompt).collect { delta ->
                    suggestion.append(delta)
                    _uiState.update { it.copy(polish = PolishState.Running(suggestion.toString())) }
                }

                val text = suggestion.toString().trim()
                if (text.isEmpty()) {
                    _uiState.update {
                        it.copy(polish = PolishState.Failed("The model returned nothing to keep."))
                    }
                    return@launch
                }

                val title = if (wantsTitle) nameEntry(text) else null

                _uiState.update {
                    it.copy(
                        polish = PolishState.Ready(
                            title = title,
                            text = text,
                            millis = started.elapsedNow().inWholeMilliseconds,
                        ),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(
                        polish = PolishState.Failed(
                            failure.message ?: "That could not be refined.",
                        ),
                    )
                }
            }
        }
    }

    /**
     * The naming pass, over the **corrected** text rather than the original.
     *
     * Titling clean prose is an easier job than titling prose with the typos still in it, and the
     * corrected version is what the person is about to accept anyway.
     *
     * Returns null rather than throwing on failure — see [refine].
     */
    private suspend fun nameEntry(text: String): String? {
        _uiState.update { it.copy(polish = PolishState.Naming(text)) }
        return try {
            val named = StringBuilder()
            engine.generate(buildTitlePrompt(text)).collect { named.append(it) }
            normalizeSuggestedTitle(named.toString())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            null
        }
    }

    /**
     * "Keep this version" — writes the suggestion into the fields.
     *
     * The corrected text replaces the body, and a suggested title fills the title field. What was
     * there is kept in [CreateJournalUiState.undo] so the swap can be put straight back, and only
     * there: this is a composer-lifetime undo, not a saved history. Once the entry is written,
     * `raw_text` is what these fields hold.
     */
    fun acceptPolish() {
        val ready = _uiState.value.polish as? PolishState.Ready ?: return
        _uiState.update { current ->
            val title = ready.title ?: current.title
            current.copy(
                title = title,
                body = ready.text,
                undo = PolishUndo(
                    previousTitle = current.title,
                    previousBody = current.body,
                    appliedTitle = title,
                    appliedBody = ready.text,
                ),
                polish = PolishState.Idle,
            )
        }
    }

    /**
     * Puts both fields back as they were before the last accept.
     *
     * Gated on [CreateJournalUiState.canUndo], not merely on there being an undo recorded. The
     * record outlives the offer on purpose — it is what lets the offer come back if the accepted
     * text is typed back in — so without this check a stale tap would revert a sentence written
     * after accepting, which is the one thing an undo must never do.
     */
    fun undoPolish() {
        val state = _uiState.value
        val undo = state.undo
        if (undo == null || !state.canUndo) return
        _uiState.update {
            it.copy(title = undo.previousTitle, body = undo.previousBody, undo = null)
        }
    }

    /** "Discard" on the suggestion card. The entry is untouched either way. */
    fun discardPolish() {
        _uiState.update { it.copy(polish = PolishState.Idle) }
    }

    /**
     * Writes the entry, then asks the screen to close.
     *
     * Failure is surfaced and the screen stays open — a draft must never be thrown away because of
     * a write that did not land.
     */
    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        cancelPolish()
        _uiState.update { it.copy(saving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.write(
                    // Whatever is in the field, which after an accepted refine is the corrected
                    // text. There is no second version to store: the swap happened in the editor
                    // and `fixed_text` stays null for entries written here.
                    rawText = state.body,
                    title = state.title,
                    mood = state.mood,
                    // A tag left sitting in the field was still typed on purpose. Committing it
                    // here is the difference between "I added three tags" and "I added two".
                    tags = addTags(state.tags, state.tagDraft),
                    timestamp = resolveAnchor(state.anchor, now(), zone()),
                )
                _uiState.update { it.copy(saving = false, finished = true) }
            } catch (cancellation: CancellationException) {
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

    /** Cancel, or system back. Asks first only when there is something to lose. */
    fun close() {
        if (_uiState.value.hasDraft) {
            _uiState.update { it.copy(confirmingDiscard = true) }
        } else {
            cancelPolish()
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun cancelDiscard() {
        _uiState.update { it.copy(confirmingDiscard = false) }
    }

    fun confirmDiscard() {
        cancelPolish()
        _uiState.update { it.copy(confirmingDiscard = false, finished = true) }
    }

    /**
     * The graph has popped this screen.
     *
     * Clears **only** [CreateJournalUiState.finished], so the 320 ms exit animation still renders
     * the entry the person wrote rather than a composer visibly emptying itself on the way out.
     */
    fun acknowledgeClose() {
        _uiState.update { it.copy(finished = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun cancelPolish() {
        polishJob?.cancel()
        polishJob = null
        if (_uiState.value.polish != PolishState.Idle) {
            _uiState.update { it.copy(polish = PolishState.Idle) }
        }
    }

    /** Why no suggestion is coming, in the engine's own words where it has any. */
    private fun engineMessage(): String = when (val current = engine.state.value) {
        is EngineState.Failed -> current.message
        EngineState.Loading -> "Still loading the model — try again in a moment."
        else -> "The model isn't loaded, so there's nothing to refine with."
    }
}
