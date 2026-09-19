package com.example.de_general.di

import com.example.de_general.core.data.DatabaseFactory
import com.example.de_general.core.data.DeGeneralDatabase
import com.example.de_general.core.data.createDatabase
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
 * [deviceProbe], [modelStorage] and [databaseFactory] are passed in rather than built here because
 * each is an `expect class` whose constructor differs per target: Android needs a `Context`, iOS
 * needs nothing. [now] is supplied per platform too, with no default, which keeps a wall clock out
 * of common code and forces both entry points to be explicit about where the time comes from.
 */
class AppContainer(
    val deviceProbe: DeviceProbe,
    modelStorage: ModelStorage,
    databaseFactory: DatabaseFactory,
    private val now: () -> Long,
    httpClient: HttpClient = HttpClient(),
) {
    val modelInstaller: ModelInstaller = ModelInstaller(httpClient, modelStorage)

    // Lazy: this container is built in Activity.onCreate on the main thread, and opening a SQLite
    // file there is not something onboarding should pay for. Someone who never finishes setup
    // never opens the database at all.
    private val database: DeGeneralDatabase by lazy { databaseFactory.createDatabase() }

    val journalRepository: JournalRepository by lazy {
        JournalRepository(database.journalDao(), now)
    }
}
