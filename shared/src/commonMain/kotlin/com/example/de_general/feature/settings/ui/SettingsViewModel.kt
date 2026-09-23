package com.example.de_general.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.de_general.core.data.ThemePreferences
import com.example.de_general.core.ui.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Drives the Settings tab. Today that is one choice — System, Light or Dark.
 *
 * Thin on purpose: [ThemePreferences] already holds the state as a [StateFlow] and updates it
 * before it writes, so there is nothing to mirror here. The screen never sees the preferences
 * object; it gets [themeMode] and [setThemeMode] as a value and a callback.
 */
class SettingsViewModel(
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferences.mode

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreferences.setMode(mode) }
    }
}
