package com.example.de_general.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single journal entry.
 *
 * [rawText] and [timestamp] are written the moment the user saves. The three AI
 * columns are filled in afterwards, once the model has processed the entry, so
 * they stay null until then.
 */
@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Exactly what the user wrote, untouched. */
    @ColumnInfo(name = "raw_text")
    val rawText: String,

    /** The AI's grammar-corrected version of [rawText]. */
    @ColumnInfo(name = "fixed_text")
    val fixedText: String? = null,

    /** Hex colour the AI picked for the entry's mood, e.g. "#FF7A45". */
    @ColumnInfo(name = "feeling_color")
    val feelingColor: String? = null,

    /** The follow-up question the AI generated for this entry. */
    @ColumnInfo(name = "ai_question")
    val aiQuestion: String? = null,

    /** Creation time, in milliseconds since the Unix epoch. */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
)
