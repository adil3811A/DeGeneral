package com.example.de_general.di

import com.example.de_general.core.ai.LlamatikEngine
import com.example.de_general.core.ai.LlmEngine
import com.example.de_general.core.data.DatabaseFactory
import com.example.de_general.core.data.DeGeneralDatabase
import com.example.de_general.core.data.PreferencesStorage
import com.example.de_general.core.data.ThemePreferences
import com.example.de_general.core.data.createDatabase
import com.example.de_general.feature.chat.data.ChatRepository
import com.example.de_general.feature.journal.data.JournalRepository
import com.example.de_general.feature.onboarding.domain.DeviceProbe
import com.example.de_general.feature.onboarding.domain.ModelInstaller
import com.example.de_general.feature.onboarding.domain.ModelStorage
import io.ktor.client.HttpClient

/**
 * The few long-lived objects the app needs, built once at the platform entry point.
 *
 * Hand-rolled on purpose. A dependency-injection framework would cost more than it saves at this
 * size, and the decision is not "until it grows" — it is until constructing this by hand actually
 * hurts. Adding a field here is cheaper than adding a framework.
 *
 * [deviceProbe], [modelStorage], [databaseFactory] and [preferencesStorage] are passed in rather
 * than built here because each is an `expect class` whose constructor differs per target: Android
 * needs a `Context`, iOS needs nothing. [now] is supplied per platform too, with no default, which keeps a wall clock out
 * of common code and forces both entry points to be explicit about where the time comes from.
 */
class AppContainer(
    val deviceProbe: DeviceProbe,
    modelStorage: ModelStorage,
    databaseFactory: DatabaseFactory,
    /**
     * The wall clock, supplied per platform.
     *
     * Public because the journal composer needs to stamp a backdated entry with the *same* clock
     * the repository would have used — two clocks that agree by coincidence are two clocks that can
     * stop agreeing.
     */
    val now: () -> Long,
    preferencesStorage: PreferencesStorage,
    httpClient: HttpClient = HttpClient(),
) {
    /**
     * System, Light or Dark.
     *
     * Eager, not lazy: `App` reads it to pick the theme for the very first frame, and the read is
     * one tiny file — nothing like the cost the lazy database below is avoiding.
     */
    val themePreferences: ThemePreferences = ThemePreferences(
        fileSystem = preferencesStorage.fileSystem,
        file = preferencesStorage.directory / "theme_mode",
    )

    val modelInstaller: ModelInstaller = ModelInstaller(httpClient, modelStorage)

    // Lazy: this container is built in Activity.onCreate on the main thread, and opening a SQLite
    // file there is not something onboarding should pay for. Someone who never finishes setup
    // never opens the database at all.
    private val database: DeGeneralDatabase by lazy { databaseFactory.createDatabase() }

    val journalRepository: JournalRepository by lazy {
        JournalRepository(database.journalDao(), now)
    }

    /**
     * The chat, tied to the journal here rather than in either feature.
     *
     * `feature/chat` must not import `feature/journal`, so the two things the companion needs from
     * the journal — recent entries for context, and a way to save an insight back — arrive as
     * lambdas. This container is the one place that is allowed to know about both.
     */
    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            dao = database.chatDao(),
            now = now,
            recentJournalEntries = { count ->
                journalRepository.entries().take(count).map { it.rawText }
            },
            saveInsight = { text -> journalRepository.write(text) },
        )
    }

    /**
     * The local model.
     *
     * One instance for the whole process, which is why this container has to be
     * application-scoped and not rebuilt in `Activity.onCreate` — a second copy would start
     * loading another ~770 MB of weights on every rotation.
     *
     * Constructed eagerly but **loads nothing**: [LlamatikEngine] starts at `EngineState.Idle` and
     * only touches the weights when the Chat screen asks it to.
     */
    val llmEngine: LlmEngine = LlamatikEngine()
}
