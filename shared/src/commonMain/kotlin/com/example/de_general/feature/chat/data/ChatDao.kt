package com.example.de_general.feature.chat.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.de_general.feature.chat.domain.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * The chat table.
 *
 * A plain interface like [com.example.de_general.feature.journal.data.JournalDao], which is what
 * lets the repository above it be tested against a fake with no Room and no database file.
 */
@Dao
interface ChatDao {

    @Insert
    suspend fun insert(message: ChatMessage): Long

    /**
     * Oldest first — the opposite of the journal.
     *
     * A transcript is read top to bottom, and the ordering is the DAO's job rather than something
     * each caller remembers to reverse.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeAll(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    suspend fun getAll(): List<ChatMessage>

    @Delete
    suspend fun delete(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
