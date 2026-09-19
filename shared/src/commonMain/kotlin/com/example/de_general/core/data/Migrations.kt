package com.example.de_general.core.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Schema history.
 *
 * Every version bump needs a migration here and a line in [DeGeneralMigrations], because
 * [createDatabase] deliberately does **not** enable destructive fallback: a missing migration
 * should be a crash on the developer's machine, not a silently wiped journal on someone's phone.
 *
 * The SQL has to match what Room derives from the `@Entity` exactly — Room validates the schema it
 * finds against the one it expected and refuses to open the database if they differ, down to
 * column order and the backtick quoting. The reliable way to write one is to copy `createSql` out
 * of the generated `shared/schemas/…/<version>.json` and substitute the table name for
 * `${'$'}{TABLE_NAME}`.
 */

/**
 * 1 → 2: the Chat tab.
 *
 * Adds `chat_messages` and touches nothing else, so an existing journal survives the upgrade
 * untouched. That is the case worth testing on a device: install over a build that already has
 * entries, not over a clean one.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `chat_messages` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`role` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL)",
        )
    }
}

/** Every migration, in order. Passed to the builder in [createDatabase]. */
val DeGeneralMigrations: Array<Migration> = arrayOf(MIGRATION_1_2)
