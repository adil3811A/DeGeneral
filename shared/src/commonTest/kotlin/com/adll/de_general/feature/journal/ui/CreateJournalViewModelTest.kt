package com.adll.de_general.feature.journal.ui

import com.adll.de_general.core.ai.EngineState
import com.adll.de_general.core.ai.LlmEngine
import com.adll.de_general.feature.journal.data.FakeJournalDao
import com.adll.de_general.feature.journal.data.JournalRepository
import com.adll.de_general.feature.journal.domain.DateAnchor
import com.adll.de_general.feature.journal.domain.JournalMood
import com.adll.de_general.feature.journal.domain.POLISH_INSTRUCTION
import com.adll.de_general.feature.journal.domain.TITLE_INSTRUCTION
import com.adll.de_general.feature.journal.domain.decodeTags
import com.adll.de_general.feature.journal.domain.localDateAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * An [LlmEngine] that is a plain flow.
 *
 * `CLAUDE.md`'s warning about `advanceUntilIdle()` is specifically about Ktor's `MockEngine`, which
 * runs off the coroutine test scheduler. A `flow { }` does not — it runs in the collector's
 * coroutine — so virtual time really is a synchronisation point here.
 */
private class FakeEngine(
    private val correction: List<String> = listOf("Walked ", "home."),
    private val titleAnswer: String = "Walking home",
    private val failLoadWith: String? = null,
    private val failTitle: Boolean = false,
) : LlmEngine {

    private val _state = MutableStateFlow<EngineState>(EngineState.Idle)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    var loads = 0
    var unloads = 0

    /** Every prompt, in order, so a test can tell the two passes apart. */
    val prompts = mutableListOf<String>()

    override suspend fun load(modelPath: Path) {
        loads++
        _state.value = failLoadWith
            ?.let { EngineState.Failed(it) }
            ?: EngineState.Ready(loadMillis = 12)
    }

    override suspend fun unload() {
        unloads++
        _state.value = EngineState.Idle
    }

    override fun generate(prompt: String): Flow<String> = flow {
        prompts += prompt
        if (prompt.contains(TITLE_INSTRUCTION)) {
            check(!failTitle) { "the naming pass failed" }
            emit(titleAnswer)
        } else {
            correction.forEach { emit(it) }
        }
    }
}

class CreateJournalViewModelTest {

    /** Thursday 22 October 2026, 08:45 PM in New York. */
    private val now = 1_792_716_300_000L

    private val newYork = TimeZone.of("America/New_York")

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main.immediate, which has no implementation off
        // Android until a test dispatcher is installed here.
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fixture(
        engine: FakeEngine = FakeEngine(),
        dao: FakeJournalDao = FakeJournalDao(),
        clock: () -> Long = { now },
    ): Triple<CreateJournalViewModel, FakeJournalDao, FakeEngine> {
        val viewModel = CreateJournalViewModel(
            repository = JournalRepository(dao, clock),
            engine = engine,
            modelPath = "/models/gemma.gguf".toPath(),
            modelLabel = "Gemma 3 1B · Q4_K_M",
            now = clock,
            zone = { newYork },
        )
        viewModel.startComposing("entry-1")
        return Triple(viewModel, dao, engine)
    }

    /**
     * The rotation guard. `DisposableEffect`'s `onDispose` fires on rotation too, which is why the
     * draft is keyed on the back-stack entry id instead — and why the same id must be a no-op.
     */
    @Test
    fun startComposingTwiceWithTheSameSessionKeepsTheDraft() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()
        viewModel.onBodyChange("half a thought")

        viewModel.startComposing("entry-1")

        assertEquals("half a thought", viewModel.uiState.value.body)
    }

    @Test
    fun aNewSessionStartsOnABlankPage() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()
        viewModel.onBodyChange("half a thought")
        viewModel.onMoodChange(JournalMood.Calm)

        viewModel.startComposing("entry-2")

        val state = viewModel.uiState.value
        assertEquals("", state.body)
        assertNull(state.mood)
        // The label survives a reset; it comes from the pinned spec, not from the draft.
        assertEquals("Gemma 3 1B · Q4_K_M", state.modelLabel)
    }

    @Test
    fun savingWritesEveryFieldAndThenAsksTheGraphToClose() = runTest(dispatcher) {
        val (viewModel, dao, _) = fixture()
        viewModel.onTitleChange("The long way home")
        viewModel.onBodyChange("Walked back along the canal.")
        viewModel.onMoodChange(JournalMood.Reflective)
        viewModel.onTagDraftChange("rain, walking")
        viewModel.commitTagDraft()

        viewModel.save()
        advanceUntilIdle()

        val saved = dao.rows.value.single()
        assertEquals("The long way home", saved.title)
        assertEquals("Walked back along the canal.", saved.rawText)
        assertEquals("Reflective", saved.mood)
        assertEquals(listOf("rain", "walking"), decodeTags(saved.tags))
        assertTrue(viewModel.uiState.value.finished)

        // Clearing the flag leaves the draft alone, so the exit animation still renders the entry.
        viewModel.acknowledgeClose()
        assertFalse(viewModel.uiState.value.finished)
        assertEquals("Walked back along the canal.", viewModel.uiState.value.body)
    }

    /** A tag typed but not turned into a chip was still typed on purpose. */
    @Test
    fun aHalfTypedTagIsCommittedBySave() = runTest(dispatcher) {
        val (viewModel, dao, _) = fixture()
        viewModel.onBodyChange("hello")
        viewModel.onTagDraftChange("rain")

        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("rain"), decodeTags(dao.rows.value.single().tags))
    }

    @Test
    fun backdatingFilesTheEntryOnTheChosenDayAtTheTimeSaveWasPressed() = runTest(dispatcher) {
        val (viewModel, dao, _) = fixture()
        viewModel.onBodyChange("hello")
        viewModel.onAnchorChange(DateAnchor.Yesterday)

        viewModel.save()
        advanceUntilIdle()

        val saved = dao.rows.value.single()
        assertEquals(LocalDate(2026, 10, 21), localDateAt(saved.timestamp, newYork))
        // Not midnight: the time of day is the one the clock read when Save was pressed.
        assertEquals(now - 86_400_000L, saved.timestamp)
    }

    @Test
    fun aFailedWriteKeepsTheDraftAndSaysWhatWentWrong() = runTest(dispatcher) {
        val dao = FakeJournalDao().apply { failInsertWith = "The database is locked." }
        val (viewModel, _, _) = fixture(dao = dao)
        viewModel.onBodyChange("hello")

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("The database is locked.", state.errorMessage)
        assertEquals("hello", state.body)
        assertFalse(state.finished)
        assertFalse(state.saving)
    }

    @Test
    fun refiningLoadsTheModelOnceAndNeverUnloadsIt() = runTest(dispatcher) {
        val (viewModel, _, engine) = fixture()
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()
        assertEquals(
            PolishState.Ready("Walking home", "Walked home.", 0),
            viewModel.uiState.value.polish.zeroed(),
        )

        viewModel.discardPolish()
        viewModel.refine()
        advanceUntilIdle()

        // Loaded once; the second refine found the engine already ready.
        assertEquals(1, engine.loads)
        // Never. A composer that unloaded would rip the weights out from under the Chat screen.
        assertEquals(0, engine.unloads)
    }

    /**
     * Two passes, not two fields in one answer. The naming pass is asked about the **corrected**
     * text, because titling clean prose is an easier job than titling prose with typos in it.
     */
    @Test
    fun namingIsASecondPassOverTheCorrectedText() = runTest(dispatcher) {
        val (viewModel, _, engine) = fixture()
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()

        assertEquals(2, engine.prompts.size)
        assertTrue(engine.prompts[0].contains(POLISH_INSTRUCTION))
        assertTrue(engine.prompts[0].contains("walked home"))

        // The corrected text, not the original the first pass was given.
        assertTrue(engine.prompts[1].contains(TITLE_INSTRUCTION))
        assertTrue(engine.prompts[1].contains("Walked home."))
    }

    /** The polish prompt is about prose. A title sent there gets folded into the entry. */
    @Test
    fun theTitleIsNeverSentToThePolishPass() = runTest(dispatcher) {
        val (viewModel, _, engine) = fixture()
        viewModel.onTitleChange("The long way home")
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()

        assertFalse(engine.prompts[0].contains("The long way home"))
    }

    /** No point spending 770 MB and a second pass on a title that would not be applied. */
    @Test
    fun refiningDoesNotSpendAPassOnATitleYouAlreadyWrote() = runTest(dispatcher) {
        val (viewModel, _, engine) = fixture()
        viewModel.onTitleChange("The long way home")
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()

        assertEquals(1, engine.prompts.size)
        assertNull((viewModel.uiState.value.polish as PolishState.Ready).title)
    }

    /** A title is a nicety. Losing it must not cost the correction, which is what was asked for. */
    @Test
    fun aFailedNamingPassDoesNotCostTheCorrection() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture(engine = FakeEngine(failTitle = true))
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()

        assertEquals(
            PolishState.Ready(null, "Walked home.", 0),
            viewModel.uiState.value.polish.zeroed(),
        )
    }

    @Test
    fun keepingASuggestionReplacesBothFields() = runTest(dispatcher) {
        val (viewModel, dao, _) = fixture()
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()
        viewModel.acceptPolish()

        val state = viewModel.uiState.value
        assertEquals("Walked home.", state.body)
        assertEquals("Walking home", state.title)
        assertTrue(state.canUndo)
        // The card stops offering the suggestion: it is in the fields now.
        assertEquals(PolishState.Idle, state.polish)

        viewModel.save()
        advanceUntilIdle()

        val saved = dao.rows.value.single()
        assertEquals("Walked home.", saved.rawText)
        assertEquals("Walking home", saved.title)
        // One text, in one column. There is no second version to keep.
        assertNull(saved.fixedText)
        assertNull(saved.feelingColor)
        assertNull(saved.aiQuestion)
    }

    /** A suggested title never overwrites one the person typed. */
    @Test
    fun keepingASuggestionLeavesATitleYouWroteAlone() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()
        viewModel.onTitleChange("The long way home")
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()
        viewModel.acceptPolish()

        assertEquals("The long way home", viewModel.uiState.value.title)
        assertEquals("Walked home.", viewModel.uiState.value.body)
    }

    @Test
    fun undoPutsBothFieldsBackAsTheyWere() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()
        viewModel.acceptPolish()
        viewModel.undoPolish()

        val state = viewModel.uiState.value
        assertEquals("walked home", state.body)
        assertEquals("", state.title)
        assertFalse(state.canUndo)
    }

    /** An undo that also discarded a sentence written after accepting would be a worse trap. */
    @Test
    fun theUndoOfferGoesAwayOnceYouEditTheAcceptedText() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()
        viewModel.acceptPolish()
        assertTrue(viewModel.uiState.value.canUndo)

        viewModel.onBodyChange("Walked home. Slowly.")

        assertFalse(viewModel.uiState.value.canUndo)
        // And calling it anyway is refused rather than silently reverting the new sentence.
        viewModel.undoPolish()
        assertEquals("Walked home. Slowly.", viewModel.uiState.value.body)
    }

    /** The record outlives the offer, so typing the accepted text back brings the undo back. */
    @Test
    fun typingTheAcceptedTextBackRestoresTheUndoOffer() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()
        viewModel.acceptPolish()

        viewModel.onBodyChange("Walked home. Slowly.")
        assertFalse(viewModel.uiState.value.canUndo)

        viewModel.onBodyChange("Walked home.")
        assertTrue(viewModel.uiState.value.canUndo)

        viewModel.undoPolish()
        assertEquals("walked home", viewModel.uiState.value.body)
    }

    /** A polish of text that no longer exists is thirty seconds of CPU for nobody. */
    @Test
    fun editingWhileRefiningCancelsTheRun() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()
        viewModel.onBodyChange("walked home")
        viewModel.refine()

        viewModel.onBodyChange("walked home a")
        advanceUntilIdle()

        assertEquals(PolishState.Idle, viewModel.uiState.value.polish)
    }

    @Test
    fun aRefusedLoadSurfacesTheEnginesOwnMessage() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture(
            engine = FakeEngine(failLoadWith = "The model file could not be opened."),
        )
        viewModel.onBodyChange("walked home")

        viewModel.refine()
        advanceUntilIdle()

        assertEquals(
            PolishState.Failed("The model file could not be opened."),
            viewModel.uiState.value.polish,
        )
        assertEquals("Try refining again", viewModel.uiState.value.refineLabel)
    }

    @Test
    fun closingAnEmptyComposerDoesNotAskFirst() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()

        viewModel.close()

        assertFalse(viewModel.uiState.value.confirmingDiscard)
        assertTrue(viewModel.uiState.value.finished)
    }

    @Test
    fun closingWithADraftAsksBeforeThrowingItAway() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()
        viewModel.onBodyChange("half a thought")

        viewModel.close()
        assertTrue(viewModel.uiState.value.confirmingDiscard)
        assertFalse(viewModel.uiState.value.finished)

        viewModel.cancelDiscard()
        assertFalse(viewModel.uiState.value.confirmingDiscard)
        assertEquals("half a thought", viewModel.uiState.value.body)

        viewModel.close()
        viewModel.confirmDiscard()
        assertTrue(viewModel.uiState.value.finished)
    }

    @Test
    fun aFutureDateIsNeverAccepted() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()

        // 23 October 2026 as the picker reports it: UTC midnight. In New York it is still the
        // 22nd, which is exactly why this value is read in UTC and not the device's zone.
        viewModel.onDatePicked(1_792_713_600_000L)

        assertEquals(DateAnchor.Today, viewModel.uiState.value.anchor)
        assertFalse(viewModel.uiState.value.pickingDate)
    }

    @Test
    fun tappingTheSelectedMoodClearsIt() = runTest(dispatcher) {
        val (viewModel, _, _) = fixture()

        viewModel.onMoodChange(JournalMood.Calm)
        assertEquals(JournalMood.Calm, viewModel.uiState.value.mood)

        viewModel.onMoodChange(JournalMood.Calm)
        assertNull(viewModel.uiState.value.mood)
    }
}

/** The duration is real and therefore unpredictable; zero it so the text can be compared. */
private fun PolishState.zeroed(): PolishState =
    if (this is PolishState.Ready) copy(millis = 0) else this
