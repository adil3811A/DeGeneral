package com.example.de_general.feature.journal.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single journal entry.
 *
 * [rawText] and [timestamp] are written the moment the user saves. The three AI
 * columns are filled in afterwards, once the model has processed the entry, so
 * they stay null until then.
 *
 * [title], [mood] and [tags] came later (schema 4) and are nullable for the same reason the AI
 * columns are: `ALTER TABLE ADD COLUMN` on a `NOT NULL` column demands a `DEFAULT`, and any
 * default here would be a value the person did not choose. Null means "not recorded", which is
 * exactly what is true of every entry written before the composer existed.
 */
@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /**
     * The entry, as the person chose to save it.
     *
     * This used to be documented "exactly what the user wrote, untouched", and it is not that any
     * more. The composer's Refine action **replaces** the text in the editor when the suggestion is
     * accepted, so after that this column holds the model's wording — reviewed, possibly edited
     * further, and saved deliberately. The undo for that swap lives in the composer only; once
     * Save is pressed the pre-refine draft is gone and no column holds it.
     */
    @ColumnInfo(name = "raw_text")
    val rawText: String,

    /**
     * A grammar-corrected version of [rawText] produced *after* the fact, by a batch pass over
     * existing entries.
     *
     * Null for everything the composer writes: there, the correction is applied in the editor and
     * there is only ever one text. Nothing fills this today — `getUnprocessed()` and
     * `saveAiResult` are the seam a later batch pass would use.
     */
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

    /** The person's own one-line label for the entry. Never derived from [rawText]. */
    @ColumnInfo(name = "title")
    val title: String? = null,

    /**
     * The mood the person picked, stored as a [JournalMood] constant's `name`.
     *
     * Deliberately **not** [feelingColor]. That column is the model's reading and `saveAiResult`
     * overwrites it; this one is the person's and nothing but the composer writes it. Read it back
     * with [journalMoodOrNull], which returns null for a name this build does not know rather than
     * throwing the way Room's enum support would.
     */
    @ColumnInfo(name = "mood")
    val mood: String? = null,

    /**
     * Tags, newline-joined — see `JournalTags.kt` for why one column rather than a junction table,
     * and why the separator is a newline. Encode and decode through `encodeTags`/`decodeTags`;
     * nothing should be splitting this string inline.
     */
    @ColumnInfo(name = "tags")
    val tags: String? = null,
)
