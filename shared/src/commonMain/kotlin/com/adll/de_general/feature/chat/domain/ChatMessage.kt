package com.adll.de_general.feature.chat.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One turn of a conversation, as it sits in the database.
 *
 * Shaped like [com.adll.de_general.feature.journal.domain.JournalEntry]: plain columns, no
 * relations, no type converters. A conversation is a flat list and does not need to be more.
 */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** A [ChatRole.column] value. */
    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /**
     * Generation speed, **measured** across this reply — tokens emitted over elapsed time.
     *
     * Null on the person's own turns, and null on any reply written before the app could time
     * one. `docs/LOCAL_AI.md` spent a long time with no speed figure at all precisely because
     * an estimated one would be a lie; this is the real thing, so it is allowed on screen.
     */
    @ColumnInfo(name = "tokens_per_second")
    val tokensPerSecond: Double? = null,

    /** Wall-clock time the reply took to generate. Null for anything the model did not write. */
    @ColumnInfo(name = "generation_millis")
    val generationMillis: Long? = null,
)

/**
 * Who said it.
 *
 * Stored as its [column] string rather than letting Room map the enum. Two reasons: the value stays
 * readable in a database dump, and a row written by a future version with a role this build has
 * never heard of degrades to a known one instead of throwing while the Chat tab opens.
 *
 * The strings are `user` and `model` because those are the role names in Gemma's own chat
 * template, so the column and the prompt agree without a translation table in between.
 */
enum class ChatRole(val column: String) {
    /** The person. */
    User("user"),

    /** The local model, shown as "Mindful Scribe". */
    Model("model");

    companion object {
        /**
         * The role [column] names, or [Model] for anything unrecognised.
         *
         * Falling back rather than throwing is deliberate: one turn attributed to the wrong
         * speaker beats a transcript that cannot be opened at all.
         */
        fun fromColumn(column: String): ChatRole =
            entries.firstOrNull { it.column == column } ?: Model
    }
}

/**
 * Read as an extension rather than a property on the entity, so there is no chance of Room trying
 * to make a column out of it.
 */
val ChatMessage.chatRole: ChatRole get() = ChatRole.fromColumn(role)
