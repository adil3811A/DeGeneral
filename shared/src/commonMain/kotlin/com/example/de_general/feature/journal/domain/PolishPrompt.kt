package com.example.de_general.feature.journal.domain

/**
 * How a polish pass is spelled for Gemma.
 *
 * Shaped exactly like `feature/chat/domain/ChatPrompt.kt`, for the same reason: pure text in, pure
 * text out, so `commonTest` can prove it on every target. Gemma's turn markers, and the framing
 * folded into the user turn because **Gemma has no system role** — a `<start_of_turn>system` would
 * be a turn the model was never trained on.
 *
 * **One ask per call.** [buildPolishPrompt] asks for spelling, grammar and punctuation;
 * [buildTitlePrompt] asks for a title. Neither asks for a mood colour or a follow-up question, and
 * crucially neither asks for *both* of its own things at once. A 1B model asked for several fields
 * in one answer produces malformed structure often enough to need a parser and a failure path
 * behind it, and `docs/LOCAL_AI.md` already refused the chat screen's "Suggested Journal Prompt"
 * card on exactly that ground. Two short prompts cost a second pass over the entry and buy back
 * the whole class of parsing failures.
 *
 * What comes back from each is prose, and prose is the only thing that has to be true of it. The
 * title still gets [normalizeSuggestedTitle] on the way out — not to parse structure, but because
 * a small model will wrap a title in quotes or prefix it with "Title:" often enough to matter.
 */

/**
 * Gemma's turn markers and roles.
 *
 * Spelled out here rather than borrowed from `feature/chat`'s `ChatRole`: a feature never
 * imports another feature. They are the same two strings, and they are the model's, not the
 * chat screen's.
 */
private const val TURN_START = "<start_of_turn>"
private const val TURN_END = "<end_of_turn>"
private const val USER_ROLE = "user"
private const val MODEL_ROLE = "model"

/**
 * The whole ask.
 *
 * Worded to close off the three things a helpful small model does unprompted: rewriting for style,
 * adding a sentence of its own, and answering *about* the text instead of returning it. "Reply with
 * the corrected entry and nothing else" is doing most of the work.
 */
internal const val POLISH_INSTRUCTION: String =
    "Correct the spelling, grammar and punctuation in the journal entry below. Keep the writer's " +
        "own words, voice, meaning and line breaks. Do not add, remove, reorder or rephrase " +
        "anything, and do not comment on the entry. Reply with the corrected entry and nothing " +
        "else."

/**
 * The prompt for polishing [body].
 *
 * **The title is deliberately not a parameter.** A title is a label, not prose; sending it here
 * would invite the model to fold it into the entry, and there is no correct way to unfold that
 * afterwards. Naming is [buildTitlePrompt]'s job, in a call of its own.
 *
 * Returns an empty string for a blank [body] — there is nothing to correct.
 */
internal fun buildPolishPrompt(body: String): String {
    val entry = body.trim()
    if (entry.isEmpty()) return ""

    return buildString {
        append(TURN_START)
        append(USER_ROLE)
        append('\n')
        append(POLISH_INSTRUCTION)
        append("\n\n")
        append(entry)
        append(TURN_END)
        append('\n')

        // The open turn the corrected entry lands in.
        append(TURN_START)
        append(MODEL_ROLE)
        append('\n')
    }
}

/**
 * What a title is allowed to be.
 *
 * Short, because it is a label on a card and not a summary. Told twice what *not* to return,
 * because the two things a small model does unasked are wrapping the answer in quotes and
 * explaining itself first.
 */
internal const val TITLE_INSTRUCTION: String =
    "Give the journal entry below a short title. Four words or fewer. Use the writer's own words " +
        "where you can, and never invent a detail the entry does not contain. Reply with the " +
        "title and nothing else: no quotation marks, no label, no explanation."

/**
 * The prompt for naming [body].
 *
 * A second call rather than a second field on [buildPolishPrompt]'s answer — see the file KDoc.
 * Returns an empty string for a blank [body]; an entry with nothing in it has nothing to be called.
 */
internal fun buildTitlePrompt(body: String): String {
    val entry = body.trim()
    if (entry.isEmpty()) return ""

    return buildString {
        append(TURN_START)
        append(USER_ROLE)
        append('\n')
        append(TITLE_INSTRUCTION)
        append("\n\n")
        append(entry)
        append(TURN_END)
        append('\n')
        append(TURN_START)
        append(MODEL_ROLE)
        append('\n')
    }
}

/** A title long enough to be useful on a card, short enough to stay on one line. */
private const val MAX_TITLE_LENGTH = 60

/** What a small model prefixes a title with when it decides to be helpful. */
private val TITLE_PREFIXES = listOf("title:", "suggested title:", "here is the title:", "entry:")

/**
 * [raw] as a title, or null if there is no usable one in it.
 *
 * Total and pure, so `commonTest` pins every rule. This is deliberately **not** a structured-output
 * parser — the ask is already one plain line. It cleans up the four things a 1B model does to a
 * short answer anyway:
 *
 *  - answers on several lines, the first being the title and the rest an unasked-for explanation;
 *  - wraps it in quotation marks, straight or curly;
 *  - prefixes it with "Title:";
 *  - ends it with a full stop, which a title does not take.
 *
 * Anything left over is capped at [MAX_TITLE_LENGTH]. A blank result is null rather than `""`, so
 * "the model had nothing" and "the model said nothing" are the same thing to every caller.
 */
internal fun normalizeSuggestedTitle(raw: String): String? {
    var title = raw.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: return null

    TITLE_PREFIXES.forEach { prefix ->
        if (title.length > prefix.length && title.take(prefix.length).lowercase() == prefix) {
            title = title.drop(prefix.length).trim()
        }
    }

    title = title.trim('"', '\'', '\u201c', '\u201d', '\u2018', '\u2019').trim()
    title = title.trimEnd('.', ',', ';', ':').trim()

    return title.takeIf { it.isNotEmpty() }?.take(MAX_TITLE_LENGTH)?.trim()
}
