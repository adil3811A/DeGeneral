package com.example.de_general.feature.journal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.de_general.feature.journal.domain.JournalEntry
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

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: Long): JournalEntry?

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    suspend fun getAll(): List<JournalEntry>

    /** Entries the AI has not processed yet. */
    @Query("SELECT * FROM journal_entries WHERE fixed_text IS NULL ORDER BY timestamp ASC")
    suspend fun getUnprocessed(): List<JournalEntry>

    /** Writes back the three AI-generated columns for an entry. */
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
