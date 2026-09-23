package com.adll.de_general.feature.journal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.adll.de_general.feature.journal.domain.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    /** Returns the id assigned to the new row. */
    @Insert
    suspend fun insert(entry: JournalEntry): Long

    @Update
    suspend fun update(entry: JournalEntry)

    @Delete
    suspend fun delete(entry: JournalEntry)

    /**
     * Newest first, with `id DESC` as the tiebreak.
     *
     * The tiebreak is not decoration. Backdating stamps an entry with a chosen date at the current
     * time of day, which makes two entries sharing a millisecond plausible for the first time; with
     * no tiebreak SQLite may return them in either order, and differently between reads. `id DESC`
     * means "the one written later comes first", which is the same rule the timestamp expresses.
     *
     * Queries are not part of the exported schema, so changing this needs no version bump — but
     * `FakeJournalDao` has to match it, or a test asserts an order Room does not produce.
     */
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: Long): JournalEntry?

    /** The same order as [observeAll], and it has to stay the same order. */
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC, id DESC")
    suspend fun getAll(): List<JournalEntry>

    /** Entries the AI has not processed yet. Oldest first, `id ASC` for the same reason. */
    @Query("SELECT * FROM journal_entries WHERE fixed_text IS NULL ORDER BY timestamp ASC, id ASC")
    suspend fun getUnprocessed(): List<JournalEntry>

    /**
     * Writes back the three AI-generated columns for an entry.
     *
     * Not used by the composer. Refine there replaces the text in the editor, so the entry is
     * saved once with one body and `fixed_text` stays null. This stays for a later batch pass over
     * old entries. Note what it would do if the composer did use it: it sets `feeling_color`,
     * which is the model's reading of an entry — the person's own mood is a different column.
     */
    @Query(
        """
        UPDATE journal_entries
        SET fixed_text = :fixedText,
            feeling_color = :feelingColor,
            ai_question = :aiQuestion
        WHERE id = :id
        """
    )
    suspend fun saveAiResult(
        id: Long,
        fixedText: String,
        feelingColor: String,
        aiQuestion: String,
    )

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAll()
}
