package com.example.de_general.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

const val DATABASE_FILE_NAME = "de_general.db"

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
