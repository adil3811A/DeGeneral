package com.example.de_general.data

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

fun DatabaseFactory.createDatabase(): DeGeneralDatabase =
    newBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(databaseQueryContext)
        .build()
