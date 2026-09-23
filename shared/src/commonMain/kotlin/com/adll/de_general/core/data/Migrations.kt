package com.adll.de_general.core.data

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

/**
 * 2 → 3: measured generation speed.
 *
 * Two nullable columns, so existing rows need no backfill — and null is the honest value for
 * them. They were written before anything could time a reply, and inventing a figure for them
 * is exactly what this app does not do.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `tokens_per_second` REAL")
        connection.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `generation_millis` INTEGER")
    }
}

/**
 * 3 → 4: the Create Journal composer.
 *
 * Three nullable columns on `journal_entries`, appended after `timestamp` in the same order the
 * entity declares them — Room compares column order, so the order of these three `ALTER`s is part
 * of the contract, not a matter of taste.
 *
 * `mood` is a column of its own rather than a reuse of `feeling_color`. `saveAiResult` writes
 * `feeling_color`, so folding the two together would let a later AI pass quietly overwrite a mood
 * the person picked by hand. `feeling_color` stays what it always was: the model's reading.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `journal_entries` ADD COLUMN `title` TEXT")
        connection.execSQL("ALTER TABLE `journal_entries` ADD COLUMN `mood` TEXT")
        connection.execSQL("ALTER TABLE `journal_entries` ADD COLUMN `tags` TEXT")
    }
}

/**
 * Every migration, in order. Passed to the builder in [createDatabase].
 *
 * Adding a migration above and forgetting to add it here is the one mistake in this file that is
 * not a compile error: it crashes a device upgrading with real entries, and never a clean install,
 * so no fresh run catches it.
 */
val DeGeneralMigrations: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
