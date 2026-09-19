package com.example.de_general.core.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.example.de_general.feature.journal.data.JournalDao
import com.example.de_general.feature.journal.domain.JournalEntry

const val DATABASE_FILE_NAME = "de_general.db"

/**
 * The one database.
 *
 * This is the single place `core` is allowed to import from `feature` — a Room `@Database` is the
 * aggregation point for every feature's tables, so it has to name them. Adding a table means adding
 * its entity here; it does not mean the rest of `core` may reach into a feature.
 */
@Database(
    entities = [JournalEntry::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(DeGeneralDatabaseConstructor::class)
abstract class DeGeneralDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}

// Room's KSP processor generates the `actual` object for each target.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object DeGeneralDatabaseConstructor : RoomDatabaseConstructor<DeGeneralDatabase> {
    override fun initialize(): DeGeneralDatabase
}
