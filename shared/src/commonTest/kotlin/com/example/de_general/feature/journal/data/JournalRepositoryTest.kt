package com.example.de_general.feature.journal.data

import com.example.de_general.feature.journal.domain.JournalEntry
import com.example.de_general.feature.journal.domain.JournalMood
import com.example.de_general.feature.journal.domain.decodeTags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

/**
 * A [JournalDao] with no Room and no database file.
 *
 * This is the whole reason the repository exists as a seam: the DAO is a plain interface, so the
 * repository's own behaviour — stamping the clock, trimming the draft — is testable in `commonTest`
 * on every target.
 *
 * `internal` rather than private so `CreateJournalViewModelTest` can drive a real repository
 * through it instead of keeping a second, slightly different copy of the same fake.
 *
 * **The ordering has to match the DAO's queries.** Room sorts by `timestamp DESC, id DESC`; a fake
 * that sorted by timestamp alone would let a test assert an order Room does not produce, which is
 * precisely the bug the tiebreak was added to prevent.
 */
internal class FakeJournalDao : JournalDao {
    val rows = MutableStateFlow<List<JournalEntry>>(emptyList())
    private var nextId = 1L

    /** Set to make [insert] throw, for the "the write did not land" paths. */
    var failInsertWith: String? = null

    override suspend fun insert(entry: JournalEntry): Long {
        failInsertWith?.let { throw IllegalStateException(it) }
        val id = nextId++
        rows.value = rows.value + entry.copy(id = id)
        return id
    }

    override suspend fun update(entry: JournalEntry) {
        rows.value = rows.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun delete(entry: JournalEntry) {
        rows.value = rows.value.filterNot { it.id == entry.id }
    }

    override fun observeAll(): Flow<List<JournalEntry>> =
        rows.map { list -> list.sortedWith(newestFirst) }

    override suspend fun getById(id: Long): JournalEntry? = rows.value.firstOrNull { it.id == id }

    override suspend fun getAll(): List<JournalEntry> = rows.value.sortedWith(newestFirst)

    override suspend fun getUnprocessed(): List<JournalEntry> =
        rows.value.filter { it.fixedText == null }.sortedWith(oldestFirst)

    override suspend fun saveAiResult(
        id: Long,
        fixedText: String,
        feelingColor: String,
        aiQuestion: String,
    ) {
        rows.value = rows.value.map {
            if (it.id == id) {
                it.copy(fixedText = fixedText, feelingColor = feelingColor, aiQuestion = aiQuestion)
            } else {
                it
            }
        }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}

private val newestFirst =
    compareByDescending<JournalEntry> { it.timestamp }.thenByDescending { it.id }

private val oldestFirst = compareBy<JournalEntry> { it.timestamp }.thenBy { it.id }

class JournalRepositoryTest {

    private val fixedNow = 1_700_000_000_000L

    private fun repository(
        dao: JournalDao,
        now: () -> Long = { fixedNow },
    ) = JournalRepository(dao, now)

    @Test
    fun aWrittenEntryIsStampedWithTheInjectedClock() = runTest {
        val dao = FakeJournalDao()
        val id = repository(dao).write("hello")

        val saved = dao.getById(id)!!
        assertEquals(fixedNow, saved.timestamp)
        assertEquals("hello", saved.rawText)
    }

    @Test
    fun theDraftIsTrimmedOnceHereRatherThanAtEveryCallSite() = runTest {
        val dao = FakeJournalDao()
        val id = repository(dao).write("   hello   ")

        assertEquals("hello", dao.getById(id)!!.rawText)
    }

    @Test
    fun aFreshEntryHasNoAiColumnsYet() = runTest {
        val dao = FakeJournalDao()
        val id = repository(dao).write("hello")

        val saved = dao.getById(id)!!
        assertNull(saved.fixedText)
        assertNull(saved.feelingColor)
        assertNull(saved.aiQuestion)
        assertTrue(dao.getUnprocessed().any { it.id == id })
    }

    @Test
    fun savingAnAiResultFillsTheThreeColumns() = runTest {
        val dao = FakeJournalDao()
        val repository = repository(dao)
        val id = repository.write("hello")

        repository.saveAiResult(id, "Hello.", "#FF7A45", "What made today feel that way?")

        val saved = dao.getById(id)!!
        assertEquals("Hello.", saved.fixedText)
        assertEquals("#FF7A45", saved.feelingColor)
        assertEquals("What made today feel that way?", saved.aiQuestion)
        assertTrue(dao.getUnprocessed().none { it.id == id })
    }

    @Test
    fun theStreamReEmitsAfterAWrite() = runTest {
        val dao = FakeJournalDao()
        val repository = repository(dao)

        assertTrue(repository.observeEntries().first().isEmpty())

        repository.write("hello")
        assertEquals(listOf("hello"), repository.observeEntries().first().map { it.rawText })
    }

    @Test
    fun theStreamReEmitsAfterADelete() = runTest {
        val dao = FakeJournalDao()
        val repository = repository(dao)
        val id = repository.write("hello")

        repository.delete(dao.getById(id)!!)

        assertTrue(repository.observeEntries().first().isEmpty())
    }

    @Test
    fun entriesComeBackNewestFirst() = runTest {
        val dao = FakeJournalDao()
        var clock = 1_000L
        val repository = repository(dao) { clock }

        repository.write("first")
        clock = 2_000L
        repository.write("second")

        assertEquals(listOf("second", "first"), repository.entries().map { it.rawText })
    }

    @Test
    fun aWrittenEntryCarriesTheTitleMoodAndTagsThePersonChose() = runTest {
        val dao = FakeJournalDao()
        val id = repository(dao).write(
            rawText = "  Walked home.  ",
            title = "  The long way home  ",
            mood = JournalMood.Reflective,
            tags = listOf("#Rain", "rain", " two words "),
        )

        val saved = dao.getById(id)!!
        assertEquals("Walked home.", saved.rawText)
        assertEquals("The long way home", saved.title)
        assertEquals("Reflective", saved.mood)
        assertEquals(listOf("Rain", "twowords"), decodeTags(saved.tags))
    }

    /** One representation for "not given", so no query has to test for both null and "". */
    @Test
    fun aBlankTitleOrNoTagsIsNullAndNeverAnEmptyString() = runTest {
        val dao = FakeJournalDao()
        val id = repository(dao).write("hello", title = "   ", tags = listOf("  ", "#"))

        val saved = dao.getById(id)!!
        assertNull(saved.title)
        assertNull(saved.tags)
    }

    /**
     * A backdated entry arrives as an already-resolved epoch-milli. The repository does not know
     * what a time zone is and must not learn — that arithmetic lives in `JournalDates.kt`.
     */
    @Test
    fun anExplicitTimestampOverridesTheInjectedClock() = runTest {
        val dao = FakeJournalDao()
        val id = repository(dao).write("hello", timestamp = 1_792_716_300_000L)

        assertEquals(1_792_716_300_000L, dao.getById(id)!!.timestamp)
    }

    /** Refine works before the row exists, so the corrected text arrives in the same insert. */
    @Test
    fun aPolishCanBeWrittenInTheSameInsertAsTheEntry() = runTest {
        val dao = FakeJournalDao()
        val id = repository(dao).write("walked home", fixedText = "Walked home.")

        val saved = dao.getById(id)!!
        assertEquals("walked home", saved.rawText)
        assertEquals("Walked home.", saved.fixedText)
        // The polish pass fills fixed_text and nothing else.
        assertNull(saved.feelingColor)
        assertNull(saved.aiQuestion)
    }

    /**
     * Why `mood` is a column of its own and never a reuse of `feeling_color`.
     *
     * `saveAiResult` writes `feeling_color`. Had the two been folded together, a later AI pass
     * would silently overwrite a mood the person picked by hand.
     */
    @Test
    fun savingAnAiResultLeavesTheUsersMoodAndTagsAlone() = runTest {
        val dao = FakeJournalDao()
        val repository = repository(dao)
        val id = repository.write(
            rawText = "hello",
            mood = JournalMood.Grateful,
            tags = listOf("rain"),
            title = "A title",
        )

        repository.saveAiResult(id, "Hello.", "#FF7A45", "What made today feel that way?")

        val saved = dao.getById(id)!!
        assertEquals("Grateful", saved.mood)
        assertEquals(listOf("rain"), decodeTags(saved.tags))
        assertEquals("A title", saved.title)
        assertEquals("#FF7A45", saved.feelingColor)
    }

    /**
     * The chat screen's `saveInsight` is a bare `write(text)`. Every default has to be right for
     * it, or that call would start inventing a title, a mood and a set of tags nobody chose.
     */
    @Test
    fun aBareWriteStillMeansWhatItAlwaysMeant() = runTest {
        val dao = FakeJournalDao()
        val id = repository(dao).write("an insight")

        val saved = dao.getById(id)!!
        assertEquals(fixedNow, saved.timestamp)
        assertNull(saved.title)
        assertNull(saved.mood)
        assertNull(saved.tags)
        assertNull(saved.fixedText)
    }

    /**
     * Backdating makes two entries sharing a millisecond plausible for the first time, so the
     * order has to be decided rather than left to SQLite. Later id first.
     */
    @Test
    fun twoEntriesOnTheSameMillisecondComeBackNewestWrittenFirst() = runTest {
        val dao = FakeJournalDao()
        val repository = repository(dao)

        repository.write("first")
        repository.write("second")

        assertEquals(listOf("second", "first"), repository.entries().map { it.rawText })
    }
}
