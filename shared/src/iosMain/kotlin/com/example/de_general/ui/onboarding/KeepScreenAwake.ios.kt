package com.example.de_general.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

/** Unverified, like the rest of the iOS side — see `DeviceProbe.ios.kt`. */
@Composable
actual fun KeepScreenAwake(enabled: Boolean) {
    DisposableEffect(enabled) {
        UIApplication.sharedApplication.idleTimerDisabled = enabled
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}
