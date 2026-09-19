package com.example.de_general.feature.journal.data

import com.example.de_general.feature.journal.domain.JournalEntry
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

    /** Writes the user's text, stamped now. Returns the id assigned to the new row. */
    suspend fun write(rawText: String): Long =
        dao.insert(JournalEntry(rawText = rawText.trim(), timestamp = now()))

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
