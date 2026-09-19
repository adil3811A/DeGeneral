package com.example.de_general.feature.chat.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The prompt is the only part of the chat pipeline that can be proved correct before an engine
 * exists, so it is worth pinning hard. Every rule here is one that would be invisible on screen and
 * only show up as the model answering strangely.
 */
class ChatPromptTest {

    private fun user(text: String, id: Long = 0) =
        ChatMessage(id = id, role = ChatRole.User.column, text = text, timestamp = id)

    private fun model(text: String, id: Long = 0) =
        ChatMessage(id = id, role = ChatRole.Model.column, text = text, timestamp = id)

    @Test
    fun anEmptyConversationHasNothingToAsk() {
        assertEquals("", buildPrompt(journalContext = emptyList(), turns = emptyList()))
    }

    @Test
    fun aSingleTurnCarriesTheFramingAndOpensAModelTurn() {
        val prompt = buildPrompt(emptyList(), listOf(user("hello")))

        assertEquals(
            "<start_of_turn>user\n" +
                SYSTEM_FRAMING + "\n\n" +
                "hello<end_of_turn>\n" +
                "<start_of_turn>model\n",
            prompt,
        )
    }

    /**
     * Gemma has no system role. Inventing one would produce a turn the model has never been
     * trained on, so the framing has to ride inside the first user turn.
     */
    @Test
    fun thereIsNoSystemTurn() {
        val prompt = buildPrompt(emptyList(), listOf(user("hello")))

        assertFalse(prompt.contains("<start_of_turn>system"))
        assertTrue(prompt.startsWith("<start_of_turn>user\n"))
    }

    @Test
    fun journalEntriesAppearOnceInTheFirstTurnOnly() {
        val prompt = buildPrompt(
            journalContext = listOf("walked the cedar trail", "slept badly"),
            turns = listOf(user("morning", 1), model("steady now", 2), user("evening", 3)),
        )

        assertEquals(1, prompt.split(CONTEXT_HEADING).size - 1)
        assertTrue(prompt.contains("- walked the cedar trail"))
        assertTrue(prompt.contains("- slept badly"))
        // The heading belongs to the opening turn, before anything the model said.
        assertTrue(prompt.indexOf(CONTEXT_HEADING) < prompt.indexOf("steady now"))
    }

    /** An empty section reads to a model as "there are entries and they are blank". */
    @Test
    fun noEntriesMeansNoHeadingAtAll() {
        val prompt = buildPrompt(emptyList(), listOf(user("hello")))

        assertFalse(prompt.contains(CONTEXT_HEADING))
    }

    @Test
    fun everyTurnIsReplayedInOrderWithItsOwnRole() {
        val prompt = buildPrompt(
            emptyList(),
            listOf(user("first", 1), model("second", 2), user("third", 3)),
        )

        assertEquals(
            listOf("user", "model", "user", "model"),
            Regex("<start_of_turn>(\\w+)").findAll(prompt).map { it.groupValues[1] }.toList(),
        )
        assertTrue(prompt.indexOf("first") < prompt.indexOf("second"))
        assertTrue(prompt.indexOf("second") < prompt.indexOf("third"))
    }

    /** The last thing in the prompt must be an open model turn, or there is nothing to complete. */
    @Test
    fun thePromptEndsWaitingForTheModel() {
        val prompt = buildPrompt(emptyList(), listOf(user("hello"), model("steady now")))

        assertTrue(prompt.endsWith("<start_of_turn>model\n"))
        // Open, not closed: the final turn has no <end_of_turn> after it.
        assertEquals(2, prompt.split("<end_of_turn>").size - 1)
    }

    @Test
    fun surroundingWhitespaceIsTrimmedOffEveryTurn() {
        val prompt = buildPrompt(listOf("  an entry  "), listOf(user("  hello  ")))

        assertTrue(prompt.contains("- an entry\n"))
        assertTrue(prompt.contains("hello<end_of_turn>"))
    }
}
