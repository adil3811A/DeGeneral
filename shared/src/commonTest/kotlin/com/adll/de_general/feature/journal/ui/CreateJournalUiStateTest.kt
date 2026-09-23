package com.adll.de_general.feature.journal.ui

import com.adll.de_general.feature.journal.domain.DateAnchor
import com.adll.de_general.feature.journal.domain.JournalMood
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The composer's derived state, as pure logic.
 *
 * Several of these pin *copy*. That is not overreach: each of those strings exists to replace a
 * claim the Stitch design made that this app cannot make, and a test is the only thing standing
 * between a helpful later edit and the claim coming back.
 */
class CreateJournalUiStateTest {

    /** Thursday 22 October 2026, 08:45 PM in New York. Checked, not copied from the mock. */
    private val now = 1_792_716_300_000L

    private val newYork = TimeZone.of("America/New_York")

    private fun state(
        body: String = "",
        title: String = "",
        anchor: DateAnchor = DateAnchor.Today,
        polish: PolishState = PolishState.Idle,
        undo: PolishUndo? = null,
        saving: Boolean = false,
        mood: JournalMood? = null,
        tags: List<String> = emptyList(),
        tagDraft: String = "",
    ) = CreateJournalUiState(
        sessionKey = "test",
        title = title,
        body = body,
        mood = mood,
        anchor = anchor,
        tags = tags,
        tagDraft = tagDraft,
        nowMillis = now,
        zone = newYork,
        saving = saving,
        polish = polish,
        undo = undo,
        modelLabel = "Gemma 3 1B · Q4_K_M",
    )

    // These four moved here from JournalUiStateTest when the composer left the archive screen.

    @Test
    fun anEmptyDraftCannotBeSaved() {
        assertFalse(state(body = "").canSave)
    }

    @Test
    fun aWhitespaceOnlyDraftCannotBeSaved() {
        assertFalse(state(body = "   \n  ").canSave)
    }

    @Test
    fun aDraftAlreadyBeingSavedCannotBeSavedAgain() {
        assertFalse(state(body = "something", saving = true).canSave)
    }

    @Test
    fun aDraftWithTextCanBeSaved() {
        assertTrue(state(body = "something").canSave)
    }

    /** A title with no entry under it is a label for nothing. */
    @Test
    fun aTitleAloneIsNotAnEntry() {
        assertFalse(state(title = "The long way home").canSave)
    }

    @Test
    fun closingOnlyAsksWhenThereIsSomethingToLose() {
        assertFalse(state().hasDraft)
        assertTrue(state(body = "x").hasDraft)
        assertTrue(state(title = "x").hasDraft)
        assertTrue(state(mood = JournalMood.Calm).hasDraft)
        assertTrue(state(tags = listOf("rain")).hasDraft)
        assertTrue(state(tagDraft = "rai").hasDraft)
        assertTrue(state(anchor = DateAnchor.Yesterday).hasDraft)
    }

    private val kept = PolishUndo(
        previousTitle = "",
        previousBody = "walked home",
        appliedTitle = "Walking home",
        appliedBody = "Walked home.",
    )

    @Test
    fun anUndoIsOfferedRightAfterAnAccept() {
        assertTrue(state(title = "Walking home", body = "Walked home.", undo = kept).canUndo)
    }

    @Test
    fun thereIsNothingToUndoBeforeAnAccept() {
        assertFalse(state(body = "walked home").canUndo)
        assertFalse(
            state(body = "x", polish = PolishState.Ready(null, "X.", 10)).canUndo,
        )
    }

    /**
     * The trap this closes: an undo that also silently discarded a sentence written *after*
     * accepting would be worse than no undo at all. So the offer withdraws the moment putting the
     * swap back would stop being a pure reversal — for either field.
     */
    @Test
    fun editingEitherFieldWithdrawsTheUndoOffer() {
        val accepted = state(title = "Walking home", body = "Walked home.", undo = kept)

        assertFalse(accepted.copy(body = "Walked home, slowly.").canUndo)
        assertFalse(accepted.copy(title = "The long way").canUndo)

        // Typing the accepted text back makes it a pure reversal again.
        assertTrue(accepted.copy(body = "x").copy(body = "Walked home.").canUndo)
    }

    @Test
    fun wordsAreCountedNotEstimated() {
        assertEquals(0, state(body = "").wordCount)
        assertEquals(0, state(body = "   \n ").wordCount)
        assertEquals(1, state(body = "one").wordCount)
        assertEquals(3, state(body = "one  two\nthree").wordCount)

        assertEquals("0 words", state(body = "").wordCountLabel)
        assertEquals("1 word", state(body = "one").wordCountLabel)
        assertEquals("72 words", state(body = (1..72).joinToString(" ")).wordCountLabel)
    }

    @Test
    fun theDateLabelIsDerivedFromTheAnchorAndNeverCopied() {
        assertEquals("Thursday, Oct 22, 2026", state().dateLabel)
        assertEquals("Wednesday, Oct 21, 2026", state(anchor = DateAnchor.Yesterday).dateLabel)
        // The Stitch mock labels this very date "Thursday, Oct 24, 2026". It is a Saturday.
        // Deriving the weekday instead of copying the string is the whole point of this function.
        assertEquals(
            "Saturday, Oct 24, 2026",
            state(anchor = DateAnchor.On(LocalDate(2026, 10, 24))).dateLabel,
        )
    }

    @Test
    fun refiningIsOfferedOnlyWhenThereIsSomethingToRefine() {
        assertFalse(state(body = "").canRefine)
        assertTrue(state(body = "x").canRefine)
        assertFalse(state(body = "x", polish = PolishState.Loading).canRefine)
        assertFalse(state(body = "x", polish = PolishState.Running("X")).canRefine)
        assertFalse(state(body = "x", polish = PolishState.Naming("X.")).canRefine)
        assertFalse(state(body = "x", saving = true).canRefine)
        assertTrue(state(body = "x", polish = PolishState.Failed("nope")).canRefine)
    }

    /** Two honest labels for the two slow things, rather than one spinner covering both. */
    @Test
    fun theRefineButtonNamesWhichHalfIsHappening() {
        assertEquals("Refine grammar & spelling", state(body = "x").refineLabel)
        assertEquals("Loading the model…", state(body = "x", polish = PolishState.Loading).refineLabel)
        assertEquals("Refining…", state(body = "x", polish = PolishState.Running("")).refineLabel)
        assertEquals("Naming it…", state(body = "x", polish = PolishState.Naming("X.")).refineLabel)
        assertEquals("Try refining again", state(body = "x", polish = PolishState.Failed("n")).refineLabel)
    }

    /** Measured across the real call, or absent. Never an estimate. */
    @Test
    fun theDurationIsShownOnlyOnceSomethingHasBeenTimed() {
        assertNull(state(body = "x").refinedInLabel)
        assertNull(state(body = "x", polish = PolishState.Loading).refinedInLabel)
        assertNull(state(body = "x", polish = PolishState.Running("X")).refinedInLabel)
        assertNull(state(body = "x", polish = PolishState.Naming("X.")).refinedInLabel)
        assertEquals(
            "Refined in 1240 ms",
            state(body = "x", polish = PolishState.Ready(null, "X.", 1_240)).refinedInLabel,
        )
    }

    /**
     * The sweep.
     *
     * Every label this state can produce, across every branch, checked against the claims
     * `docs/LOCAL_AI.md` and `CLAUDE.md` say this app does not make. These exist to stop the struck
     * copy being helpfully added back.
     */
    @Test
    fun noLabelClaimsEncryptionOrALatency() {
        forbidden.forEach { phrase ->
            everyLabel().forEach { label ->
                assertFalse(
                    label.lowercase().contains(phrase),
                    "\"$label\" should not contain \"$phrase\"",
                )
            }
        }
    }

    /** "1 min read" is arithmetic about a reader nobody timed. "72 words" we counted. */
    @Test
    fun thereIsNoReadingTimeAnywhereInTheCopy() {
        everyLabel().forEach { label ->
            assertFalse(label.lowercase().contains("read"), "\"$label\" mentions reading time")
        }
    }

    @Test
    fun thePrivacyBadgeDescribesWhereTheEntryStaysAndNotWhatProtectsIt() {
        assertEquals("Stays on this device", state().privacyLabel)
    }

    /** Every branch of every derived string, so the two sweeps above actually sweep something. */
    private fun everyLabel(): List<String> {
        val polishStates = listOf(
            PolishState.Idle,
            PolishState.Loading,
            PolishState.Running("partial"),
            PolishState.Naming("done"),
            PolishState.Ready(null, "done", 1_240),
            PolishState.Ready("A title", "done", 1_240),
            PolishState.Failed("The model is not loaded."),
        )
        val anchors = listOf(
            DateAnchor.Today,
            DateAnchor.Yesterday,
            DateAnchor.On(LocalDate(2026, 1, 3)),
        )

        return buildList {
            polishStates.forEach { polish ->
                val current = state(body = "one two three", polish = polish)
                add(current.refineLabel)
                add(current.refineCaption)
                add(current.privacyLabel)
                add(current.dateCaption)
                add(current.wordCountLabel)
                add(current.undoLabel)
                current.refinedInLabel?.let { add(it) }
            }
            anchors.forEach { add(state(anchor = it).dateLabel) }
            listOf(0, 1, 72).forEach { add(state(body = (1..it).joinToString(" ")).wordCountLabel) }
        }
    }

    private val forbidden = listOf(
        "encrypt",   // the app encrypts nothing of its own
        "0ms",       // a latency nothing measured
        "neural",    // no NPU is detectable, and llama.cpp runs on the CPU
        "sync",      // there is no server and nothing syncs
        "auto-save", // nothing is saved until Save is pressed
        "autosave",
    )
}
