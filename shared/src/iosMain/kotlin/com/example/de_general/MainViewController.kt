package com.example.de_general

import androidx.compose.ui.window.ComposeUIViewController
import com.example.de_general.core.data.DatabaseFactory
import com.example.de_general.di.AppContainer
import com.example.de_general.feature.onboarding.domain.DeviceProbe
import com.example.de_general.feature.onboarding.domain.ModelStorage
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

fun MainViewController() = ComposeUIViewController {
    App(
        AppContainer(
            deviceProbe = DeviceProbe(),
            modelStorage = ModelStorage(),
            databaseFactory = DatabaseFactory(),
            now = { (NSDate().timeIntervalSince1970 * 1000).toLong() },
        )
    )
}
