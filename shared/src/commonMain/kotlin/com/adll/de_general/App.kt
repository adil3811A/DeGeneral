package com.adll.de_general

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.adll.de_general.core.ui.theme.MindfulScribeTheme
import com.adll.de_general.di.AppContainer
import com.adll.de_general.navigation.AppNavHost

/**
 * The app: the theme, and a navigation host inside it.
 *
 * Everything about *where* the user is lives in [AppNavHost] and the graph builders beside it.
 *
 * Light or dark is resolved here, once, from the Settings choice and the system setting.
 * [onDarkThemeChange] hands that same answer to the platform for the things Compose does not draw —
 * on Android, the status- and navigation-bar icons. It fires on first composition and on every
 * change, so the platform never has to resolve the choice a second time and risk disagreeing.
 */
@Composable
fun App(container: AppContainer, onDarkThemeChange: (Boolean) -> Unit = {}) {
    val mode by container.themePreferences.mode.collectAsState()
    val darkTheme = mode.isDark(systemDark = isSystemInDarkTheme())

    val currentOnDarkThemeChange by rememberUpdatedState(onDarkThemeChange)
    LaunchedEffect(darkTheme) { currentOnDarkThemeChange(darkTheme) }

    MindfulScribeTheme(darkTheme = darkTheme) {
        AppNavHost(container)
    }
}
