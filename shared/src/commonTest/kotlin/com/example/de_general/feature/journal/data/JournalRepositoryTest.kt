package com.example.de_general.feature.journal.data

import com.example.de_general.feature.journal.domain.JournalEntry
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
 */
private class FakeJournalDao : JournalDao {
    val rows = MutableStateFlow<List<JournalEntry>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(entry: JournalEntry): Long {
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
        rows.map { list -> list.sortedByDescending { it.timestamp } }

    override suspend fun getById(id: Long): JournalEntry? = rows.value.firstOrNull { it.id == id }

    override suspend fun getAll(): List<JournalEntry> =
        rows.value.sortedByDescending { it.timestamp }

    override suspend fun getUnprocessed(): List<JournalEntry> =
        rows.value.filter { it.fixedText == null }.sortedBy { it.timestamp }

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
}
