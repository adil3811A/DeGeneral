package com.example.de_general

import com.example.de_general.ai.DeviceProbe
import com.example.de_general.ai.ModelInstaller
import com.example.de_general.ai.ModelStorage
import io.ktor.client.HttpClient

/**
 * The few long-lived objects the app needs, built once at the platform entry point.
 *
 * Small enough that a dependency-injection framework would cost more than it saves. When this
 * grows past a handful of fields — a repository, a real [com.example.de_general.ai.LlmEngine] —
 * revisit that.
 */
class AppContainer(
    val deviceProbe: DeviceProbe,
    modelStorage: ModelStorage,
    httpClient: HttpClient = HttpClient(),
) {
    val modelInstaller: ModelInstaller = ModelInstaller(httpClient, modelStorage)
}
