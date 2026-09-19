package com.example.de_general

import androidx.compose.runtime.Composable
import com.example.de_general.core.ui.theme.MindfulScribeTheme
import com.example.de_general.di.AppContainer
import com.example.de_general.navigation.AppNavHost

/**
 * The app: the theme, and a navigation host inside it.
 *
 * Everything about *where* the user is lives in [AppNavHost] and the graph builders beside it.
 */
@Composable
fun App(container: AppContainer) {
    MindfulScribeTheme {
        AppNavHost(container)
    }
}
