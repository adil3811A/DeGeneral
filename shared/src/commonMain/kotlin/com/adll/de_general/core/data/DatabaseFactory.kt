package com.adll.de_general.core.data

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext

/**
 * Platform-specific hook that knows where the database file lives.
 *
 * Android needs a `Context` to resolve that path, iOS does not, so each target
 * supplies its own constructor. Call [createDatabase] to finish the build.
 */
expect class DatabaseFactory {
    fun newBuilder(): RoomDatabase.Builder<DeGeneralDatabase>
}

/** Context Room runs its queries on. `Dispatchers.IO` is JVM-only. */
internal expect val databaseQueryContext: CoroutineContext

/**
 * Opens the database, applying [DeGeneralMigrations] on the way.
 *
 * Note what is *not* here: `fallbackToDestructiveMigration`. A schema bump without a matching
 * migration should fail loudly during development rather than quietly delete someone's journal
 * on their phone. Entries never leave the device, so there is no copy to restore from.
 */
fun DatabaseFactory.createDatabase(): DeGeneralDatabase =
    newBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(databaseQueryContext)
        .addMigrations(*DeGeneralMigrations)
        .build()
