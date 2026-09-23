package com.adll.de_general.feature.chat.domain

/**
 * How a conversation is spelled for Gemma.
 *
 * Pure text in, pure text out — no Compose, no platform types, no engine — so `commonTest` covers
 * every rule on every target. That is the same reason `Compatibility.kt` and `InstallCopy.kt` are
 * shaped this way, and it matters more here than usual: there is no engine yet, so this file is
 * the only part of the pipeline that can be *proved* correct before one exists.
 */

/**
 * Gemma's turn markers.
 *
 * The template is `<start_of_turn>{role}\n{text}<end_of_turn>`, with roles `user` and `model`, and
 * a trailing open `model` turn for the answer to land in. Worth knowing: **Gemma has no system
 * role.** The framing below is folded into the first user turn instead, which is what the model
 * card prescribes — inventing a `<start_of_turn>system` would produce a turn the model has never
 * been trained on.
 */
private const val TURN_START = "<start_of_turn>"
private const val TURN_END = "<end_of_turn>"

/**
 * What the companion is for.
 *
 * Deliberately modest. It is not told it has memory, tools, or any knowledge of this person beyond
 * the entries actually handed to it.
 */
internal const val SYSTEM_FRAMING: String =
    "You are Mindful Scribe, a private journalling companion running on this person's own " +
        "device. Be warm, brief and concrete. Ask at most one question. Never invent details " +
        "about their life that they have not told you."

/** Introduces the entries inside the first user turn. */
internal const val CONTEXT_HEADING = "Recent journal entries, newest first:"

/**
 * The prompt for the whole conversation so far.
 *
 * [turns] is every message in the transcript, oldest first, **including the one just sent** — by
 * the time this is called the new message is already a row in the database, so there is no
 * separate "pending message" argument to get out of step with it.
 *
 * [journalContext] is the raw text of the person's most recent entries, newest first — however
 * many exist, up to `CONTEXT_ENTRY_COUNT`. An empty list
 * omits the heading entirely rather than sending an empty section, which reads to a model as
 * "there are entries and they are blank".
 *
 * The transcript is replayed in full. A 1B model's context window is small, but truncating is a
 * decision that belongs where the token count is known, and nothing counts tokens today because
 * nothing tokenises.
 *
 * Returns an empty string for an empty [turns] — there is nothing to ask.
 */
internal fun buildPrompt(
    journalContext: List<String>,
    turns: List<ChatMessage>,
): String {
    if (turns.isEmpty()) return ""

    return buildString {
        val first = turns.first()
        appendTurn(first.chatRole, opening(journalContext, first.text.trim()))
        turns.drop(1).forEach { appendTurn(it.chatRole, it.text.trim()) }

        // The open turn the model is expected to complete.
        append(TURN_START)
        append(ChatRole.Model.column)
        append('\n')
    }
}

/** The framing, the entries and the first thing the person said, as one turn's worth of text. */
private fun opening(journalContext: List<String>, firstMessage: String): String = buildString {
    append(SYSTEM_FRAMING)
    if (journalContext.isNotEmpty()) {
        append("\n\n")
        append(CONTEXT_HEADING)
        journalContext.forEach { entry ->
            append("\n- ")
            append(entry.trim())
        }
    }
    append("\n\n")
    append(firstMessage)
}

private fun StringBuilder.appendTurn(role: ChatRole, text: String) {
    append(TURN_START)
    append(role.column)
    append('\n')
    append(text)
    append(TURN_END)
    append('\n')
}
