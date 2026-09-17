package com.example.de_general

import androidx.compose.ui.window.ComposeUIViewController
import com.example.de_general.ai.DeviceProbe
import com.example.de_general.ai.ModelStorage

fun MainViewController() = ComposeUIViewController {
    App(AppContainer(deviceProbe = DeviceProbe(), modelStorage = ModelStorage()))
}
