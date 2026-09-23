package com.adll.de_general.feature.journal.data

import com.adll.de_general.feature.journal.domain.JournalEntry
import com.adll.de_general.feature.journal.domain.JournalMood
import com.adll.de_general.feature.journal.domain.encodeTags
import kotlinx.coroutines.flow.Flow

/**
 * The journal, as the rest of the app sees it.
 *
 * Deliberately thin. The only judgement it makes is the two things a view model should not invent
 * and a DAO cannot do: stamping the time an entry was written, and trimming the draft once so three
 * call sites do not each do it differently.
 *
 * [now] is injected rather than read from a global clock, which is what lets this be tested in
 * `commonTest` on every target against a fake [JournalDao] — no Room, no database file, no
 * dependency on what time it happens to be.
 */
class JournalRepository(
    private val dao: JournalDao,
    private val now: () -> Long,
) {

    /** Newest first. Room re-emits on every write to `journal_entries`. */
    fun observeEntries(): Flow<List<JournalEntry>> = dao.observeAll()

    suspend fun entries(): List<JournalEntry> = dao.getAll()

    suspend fun entry(id: Long): JournalEntry? = dao.getById(id)

    /**
     * Writes an entry. Returns the id assigned to the new row.
     *
     * Every parameter after [rawText] is defaulted, which is what keeps the chat screen's
     * `saveInsight` — a bare `write(text)` — meaning exactly what it did before: an insight has no
     * title, no mood the person picked, no tags, and it happened now.
     *
     * Normalisation happens here, once, rather than at each of the call sites:
     *  - the body is trimmed;
     *  - a blank [title] becomes null, never `""`, so "no title" has one representation;
     *  - [tags] go through `encodeTags`, which also de-duplicates and caps them;
     *  - [mood] is stored as the constant's name;
     *  - a null [timestamp] means now.
     *
     * [timestamp] arrives as an already-resolved epoch-milli — `resolveAnchor` does the backdating
     * arithmetic, because that needs a time zone and this class deliberately does not know what one
     * is. [now] still stamps every entry that was not backdated.
     *
     * [fixedText] is here rather than only on `saveAiResult` so a correction can be attached to a
     * row that does not exist yet. The composer does not use it — an accepted refine is applied in
     * the editor, so [rawText] already *is* the corrected text and there is no second version to
     * store. It stays for a batch pass that wants to write both at once.
     */
    suspend fun write(
        rawText: String,
        title: String? = null,
        mood: JournalMood? = null,
        tags: List<String> = emptyList(),
        fixedText: String? = null,
        timestamp: Long? = null,
    ): Long = dao.insert(
        JournalEntry(
            rawText = rawText.trim(),
            fixedText = fixedText?.trim()?.takeIf { it.isNotEmpty() },
            title = title?.trim()?.takeIf { it.isNotEmpty() },
            mood = mood?.name,
            tags = encodeTags(tags),
            timestamp = timestamp ?: now(),
        ),
    )

    suspend fun update(entry: JournalEntry) = dao.update(entry)

    suspend fun delete(entry: JournalEntry) = dao.delete(entry)

    /** Entries the model has not processed yet. Nothing calls this until `LlmEngine` is real. */
    suspend fun unprocessed(): List<JournalEntry> = dao.getUnprocessed()

    suspend fun saveAiResult(
        id: Long,
        fixedText: String,
        feelingColor: String,
        aiQuestion: String,
    ) = dao.saveAiResult(id, fixedText, feelingColor, aiQuestion)

    suspend fun deleteAll() = dao.deleteAll()
}
