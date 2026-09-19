package com.example.de_general

import androidx.compose.ui.window.ComposeUIViewController
import com.example.de_general.core.data.DatabaseFactory
import com.example.de_general.di.AppContainer
import com.example.de_general.feature.onboarding.domain.DeviceProbe
import com.example.de_general.feature.onboarding.domain.ModelStorage
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * One container for the life of the process.
 *
 * Held at file scope rather than built inside the composable, for the same reason Android
 * moved it into `Application`: it owns the inference engine, and a second one would mean a
 * second copy of the weights.
 */
private val container: AppContainer by lazy {
    AppContainer(
        deviceProbe = DeviceProbe(),
        modelStorage = ModelStorage(),
        databaseFactory = DatabaseFactory(),
        now = { (NSDate().timeIntervalSince1970 * 1000).toLong() },
    )
}

fun MainViewController() = ComposeUIViewController { App(container) }
