package com.adll.de_general.feature.chat.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The role column is the only encoded value in the chat table, so the mapping either round-trips
 * or a transcript comes back with the wrong speaker on every line.
 */
class ChatRoleTest {

    @Test
    fun everyRoleRoundTripsThroughItsColumn() {
        ChatRole.entries.forEach { role ->
            assertEquals(role, ChatRole.fromColumn(role.column))
        }
    }

    /**
     * The stored strings match Gemma's own chat-template roles, so the column and the prompt agree
     * without a translation table. Changing one of these means changing `ChatPrompt.kt` too.
     */
    @Test
    fun theColumnsAreGemmasRoleNames() {
        assertEquals("user", ChatRole.User.column)
        assertEquals("model", ChatRole.Model.column)
    }

    /**
     * A row written by a future version with a role this build has never heard of must not take
     * the Chat tab down with it. One turn attributed to the wrong speaker is the cheaper failure.
     */
    @Test
    fun anUnknownRoleFallsBackRatherThanThrowing() {
        assertEquals(ChatRole.Model, ChatRole.fromColumn("system"))
        assertEquals(ChatRole.Model, ChatRole.fromColumn(""))
    }

    @Test
    fun aMessageReadsItsOwnRole() {
        val message = ChatMessage(role = ChatRole.User.column, text = "hi", timestamp = 0L)
        assertEquals(ChatRole.User, message.chatRole)
    }
}
