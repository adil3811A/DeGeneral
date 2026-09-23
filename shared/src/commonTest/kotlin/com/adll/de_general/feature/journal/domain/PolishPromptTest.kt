package com.adll.de_general.feature.journal.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the narrowest useful ask.
 *
 * Shaped like `ChatPromptTest`, and here for the same reason plus one more: these cases exist to
 * stop a well-meaning later edit widening the request. A 1B model asked for three fields at once
 * produces malformed structure often enough to need a parser and a failure path, and
 * `docs/LOCAL_AI.md` already refused the chat screen's prompt card on that ground.
 */
class PolishPromptTest {

    private val body = "walked home along the canal insted of takeing the bus"

    @Test
    fun theWholePromptIsOneUserTurnAndOneOpenModelTurn() {
        assertEquals(
            "<start_of_turn>user\n" +
                POLISH_INSTRUCTION + "\n\n" +
                body +
                "<end_of_turn>\n" +
                "<start_of_turn>model\n",
            buildPolishPrompt(body),
        )
    }

    /** Gemma has no system role. Inventing one would produce a turn it was never trained on. */
    @Test
    fun thereIsNoSystemTurn() {
        assertFalse(buildPolishPrompt(body).contains("system"))
    }

    @Test
    fun theBodyIsTrimmedButOtherwiseUntouched() {
        assertEquals(buildPolishPrompt(body), buildPolishPrompt("\n  $body  \n"))
        assertTrue(buildPolishPrompt("line one\nline two").contains("line one\nline two"))
    }

    @Test
    fun aBlankEntryAsksNothing() {
        assertEquals("", buildPolishPrompt(""))
        assertEquals("", buildPolishPrompt("   \n  "))
    }

    /**
     * A title is a label, not prose. Sending it to the *polish* pass invites the model to fold it
     * into the entry, and there is no correct way to unfold that afterwards. Naming is
     * [buildTitlePrompt]'s job, in a call of its own.
     */
    @Test
    fun theTitleIsNeverSent() {
        // The function takes no title at all, which is the strongest form of this guarantee. What
        // this pins is that the body is the only thing that reaches the prompt.
        val prompt = buildPolishPrompt(body)
        assertFalse(prompt.contains("The long way home"))
        assertEquals(POLISH_INSTRUCTION.length + body.length + TURN_OVERHEAD, prompt.length)
    }

    /**
     * One ask per call. Neither prompt asks for a mood colour, a follow-up question or a
     * structured envelope — `feeling_color` and `ai_question` stay null, the same refusal
     * `docs/LOCAL_AI.md` already made of the chat screen's prompt card.
     */
    @Test
    fun nothingAsksForAColourOrAQuestion() {
        val forbidden =
            listOf("colour", "color", "hex", "#rrggbb", "question", "json", "mood", "feeling")

        listOf(buildPolishPrompt(body), buildTitlePrompt(body)).forEach { prompt ->
            forbidden.forEach {
                assertFalse(
                    prompt.lowercase().contains(it),
                    "no prompt should mention \"$it\"",
                )
            }
        }
    }

    @Test
    fun theInstructionNamesOnlySpellingGrammarAndPunctuation() {
        val instruction = POLISH_INSTRUCTION.lowercase()

        assertTrue(instruction.contains("spelling"))
        assertTrue(instruction.contains("grammar"))
        assertTrue(instruction.contains("punctuation"))
        // The three things a helpful small model does unprompted, all closed off.
        assertTrue(instruction.contains("do not add"))
        assertTrue(instruction.contains("nothing else"))
    }

    // --- the naming pass ---

    @Test
    fun theTitlePromptIsOneUserTurnAndOneOpenModelTurn() {
        assertEquals(
            "<start_of_turn>user\n" +
                TITLE_INSTRUCTION + "\n\n" +
                body +
                "<end_of_turn>\n" +
                "<start_of_turn>model\n",
            buildTitlePrompt(body),
        )
    }

    @Test
    fun aBlankEntryHasNothingToBeCalled() {
        assertEquals("", buildTitlePrompt(""))
        assertEquals("", buildTitlePrompt("   \n  "))
    }

    /** Short, because it is a label on a card and not a summary. */
    @Test
    fun theTitleInstructionAsksForSomethingShortAndNothingElse() {
        val instruction = TITLE_INSTRUCTION.lowercase()

        assertTrue(instruction.contains("short title"))
        assertTrue(instruction.contains("four words or fewer"))
        assertTrue(instruction.contains("nothing else"))
        // The guard against a model that names a detail the entry does not contain.
        assertTrue(instruction.contains("never invent"))
    }

    // --- what comes back ---

    @Test
    fun aPlainTitleComesThroughUnchanged() {
        assertEquals("The long way home", normalizeSuggestedTitle("The long way home"))
    }

    @Test
    fun quotationMarksAreStripped() {
        assertEquals("The long way home", normalizeSuggestedTitle("\"The long way home\""))
        assertEquals("The long way home", normalizeSuggestedTitle("'The long way home'"))
        assertEquals("The long way home", normalizeSuggestedTitle("\u201cThe long way home\u201d"))
    }

    @Test
    fun aLabelTheModelAddedItselfIsStripped() {
        assertEquals("The long way home", normalizeSuggestedTitle("Title: The long way home"))
        assertEquals("The long way home", normalizeSuggestedTitle("title: The long way home"))
        assertEquals(
            "The long way home",
            normalizeSuggestedTitle("Suggested title: \"The long way home\""),
        )
    }

    /** A title does not take a full stop, and a small model adds one anyway. */
    @Test
    fun trailingPunctuationIsDropped() {
        assertEquals("The long way home", normalizeSuggestedTitle("The long way home."))
        assertEquals("The long way home", normalizeSuggestedTitle("The long way home,"))
    }

    /** Asked for one line, a 1B model will sometimes explain itself underneath. */
    @Test
    fun onlyTheFirstNonBlankLineIsUsed() {
        assertEquals(
            "The long way home",
            normalizeSuggestedTitle("\n\nThe long way home\n\nI chose this because it..."),
        )
    }

    @Test
    fun anOverLongTitleIsCappedRatherThanRejected() {
        val title = normalizeSuggestedTitle("word ".repeat(40))!!

        assertTrue(title.length <= 60)
        assertEquals(title, title.trim())
    }

    /** "The model had nothing" and "the model said nothing" are one thing to every caller. */
    @Test
    fun nothingUsableIsNullAndNeverAnEmptyString() {
        assertNull(normalizeSuggestedTitle(""))
        assertNull(normalizeSuggestedTitle("   \n \t "))
        assertNull(normalizeSuggestedTitle("\"\""))
        assertNull(normalizeSuggestedTitle("..."))
    }
}

/**
 * `<start_of_turn>user\n` + `\n\n` + `<end_of_turn>\n` + `<start_of_turn>model\n`.
 *
 * Spelled out as a number so [PolishPromptTest.theTitleIsNeverSent] can assert the prompt is
 * *exactly* the instruction plus the body, with nothing else smuggled in.
 */
private val TURN_OVERHEAD =
    "<start_of_turn>user\n".length + "\n\n".length +
        "<end_of_turn>\n".length + "<start_of_turn>model\n".length
