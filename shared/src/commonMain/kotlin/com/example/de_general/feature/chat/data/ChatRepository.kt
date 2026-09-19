package com.example.de_general.feature.chat.data

import com.example.de_general.feature.chat.domain.ChatMessage
import com.example.de_general.feature.chat.domain.ChatRole
import kotlinx.coroutines.flow.Flow

/**
 * How many past journal entries the companion is given as context.
 *
 * Three, because that is what the design's chip says and because a 1B model's context window is
 * small. Whatever this number is, the chip reports what was actually found — never this constant.
 */
const val CONTEXT_ENTRY_COUNT: Int = 3

/**
 * The conversation, as the rest of the app sees it.
 *
 * Deliberately thin, like [com.example.de_general.feature.journal.data.JournalRepository]. It
 * stamps the clock and trims the text, and that is all the judgement it has.
 *
 * [recentJournalEntries] and [saveInsight] are lambdas rather than a `JournalRepository`, and that
 * is a layering decision, not squeamishness: **a feature never imports another feature.** `di`
 * already knows about both, so it is the right place to tie the two together — see `AppContainer`.
 *
 * [now] is injected for the same reason it is in the journal: so this is testable in `commonTest`
 * against a fake DAO, with no dependency on what time it happens to be.
 */
class ChatRepository(
    private val dao: ChatDao,
    private val now: () -> Long,
    private val recentJournalEntries: suspend (count: Int) -> List<String>,
    private val saveInsight: suspend (text: String) -> Unit,
) {

    /** The transcript, oldest first. Room re-emits on every write to `chat_messages`. */
    fun observeMessages(): Flow<List<ChatMessage>> = dao.observeAll()

    /**
     * The transcript right now, oldest first.
     *
     * Read straight from the table rather than from the observed copy, because a caller that
     * has just written a row cannot know whether the flow has re-emitted yet. Building a prompt
     * from a stale list would silently drop the message being answered.
     */
    suspend fun messages(): List<ChatMessage> = dao.getAll()

    /** Records what the person said, stamped now. Returns the id assigned to the new row. */
    suspend fun sendUserMessage(text: String): Long = dao.insert(
        ChatMessage(role = ChatRole.User.column, text = text.trim(), timestamp = now()),
    )

    /**
     * Records what the model produced, with what it cost.
     *
     * [tokensPerSecond] and [generationMillis] are measured by the caller across the actual
     * generation; they are nullable so that a reply which finished without a usable timing
     * stores nothing rather than a made-up number.
     */
    suspend fun recordModelMessage(
        text: String,
        tokensPerSecond: Double? = null,
        generationMillis: Long? = null,
    ): Long = dao.insert(
        ChatMessage(
            role = ChatRole.Model.column,
            text = text.trim(),
            timestamp = now(),
            tokensPerSecond = tokensPerSecond,
            generationMillis = generationMillis,
        ),
    )

    /**
     * The entries handed to the model as context, newest first.
     *
     * Asks for [CONTEXT_ENTRY_COUNT] and returns however many exist. A new user gets an empty
     * list, and the caller says "no entries yet" rather than claiming a number.
     */
    suspend fun journalContext(): List<String> = recentJournalEntries(CONTEXT_ENTRY_COUNT)

    /** Writes a companion message into the journal — what the "Save insight" chip does. */
    suspend fun saveAsJournalEntry(text: String) = saveInsight(text)

    suspend fun clear() = dao.deleteAll()

    suspend fun delete(message: ChatMessage) = dao.delete(message)
}
