package com.adll.de_general.feature.journal.domain

/**
 * Tags, as one column of text.
 *
 * Pure functions, no Compose and no Room, so `commonTest` pins every rule on every target.
 *
 * **Why not a junction table.** A `tags` table plus a join would force `@Relation` and
 * `@Transaction` onto every read, which changes what `observeEntries()` returns and ripples into
 * `JournalUiState`, both preview fixtures, `FakeJournalDao` and every DAO query — all to serve
 * `WHERE tag = ?` lookups that no screen asks for. Keeping serialise/deserialise here as two tested
 * functions is also what would make that migration cheap later; `split(",")` scattered inline at
 * four call sites is the thing that would make it expensive.
 *
 * **Why a newline separator.** [normalizeTag] strips *all* whitespace, so a newline cannot survive
 * into a tag. The separator is therefore safe by construction rather than by a rule someone has to
 * remember — which is exactly the property a comma would not have.
 */

/** Long enough for a real phrase-tag, short enough that one cannot become a paragraph. */
private const val MAX_TAG_LENGTH = 32

/** A cap, not a guess at what is reasonable: unbounded tags make an unbounded row and a wrapped
 * chip field that pushes the body off screen. */
private const val MAX_TAGS = 12

/** See the file KDoc. Safe by construction, because [normalizeTag] removes it. */
private const val SEPARATOR = "\n"

/** What splits one typed line into several tags: the two things people actually type. */
private val INPUT_DELIMITERS = charArrayOf(',', '\n')

/**
 * One tag in canonical form, or null if [raw] does not contain one.
 *
 * Strips **all** whitespace rather than trimming — "two words" becomes "twowords" — which is what
 * makes [SEPARATOR] safe and what stops a tag from rendering as two chips' worth of text in one
 * chip. Drops a single leading `#`, because people type it and the chip draws its own; a second
 * `#` is kept, since at that point it is a character the person meant.
 */
fun normalizeTag(raw: String): String? {
    val stripped = raw.filterNot { it.isWhitespace() }.removePrefix("#")
    return if (stripped.isEmpty()) null else stripped.take(MAX_TAG_LENGTH)
}

/**
 * [raw] as a canonical tag list: each one normalised, blanks dropped, duplicates removed and the
 * whole thing capped at [MAX_TAGS].
 *
 * De-duplication is case-insensitive but **keeps the first spelling**: someone who typed "Rain"
 * and later "rain" meant one tag, and the one they typed first is the one they chose. Order is
 * otherwise preserved — it is the order they added them in, and re-sorting would be this code
 * having an opinion it was not asked for.
 */
fun normalizeTags(raw: List<String>): List<String> {
    val seen = mutableSetOf<String>()
    val tags = mutableListOf<String>()
    for (candidate in raw) {
        val tag = normalizeTag(candidate) ?: continue
        if (!seen.add(tag.lowercase())) continue
        tags += tag
        if (tags.size == MAX_TAGS) break
    }
    return tags
}

/**
 * [tags] as the column stores them, or null when there are none.
 *
 * Null rather than `""`, so "no tags" has exactly one representation in the database and a query
 * never has to test for both.
 */
fun encodeTags(tags: List<String>): String? =
    normalizeTags(tags).takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR)

/**
 * What the column holds, as a list. Total: null, `""` and anything malformed all give an empty
 * list, because a row that cannot be parsed must still open.
 *
 * Re-normalises on the way out, so a row written by an older build — or edited by hand — cannot
 * put a duplicate or an over-long tag on screen.
 */
fun decodeTags(stored: String?): List<String> =
    if (stored.isNullOrEmpty()) emptyList() else normalizeTags(stored.split(SEPARATOR))

/** One line of typed input as tags — people paste comma-separated lists as readily as typing one. */
fun splitTagInput(raw: String): List<String> = normalizeTags(raw.split(*INPUT_DELIMITERS))

/**
 * [existing] plus whatever [raw] contains, normalised as one list.
 *
 * Combined rather than appended so that the duplicate check and [MAX_TAGS] apply across the whole
 * result: adding "rain" to a list that already has "Rain" adds nothing, and the twelfth tag is the
 * last one accepted whichever call it arrived on.
 */
fun addTags(existing: List<String>, raw: String): List<String> =
    normalizeTags(existing + raw.split(*INPUT_DELIMITERS))
