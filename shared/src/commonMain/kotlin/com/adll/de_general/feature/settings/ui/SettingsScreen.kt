package com.adll.de_general.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.adll.de_general.core.ui.components.SectionCard
import com.adll.de_general.core.ui.theme.MindfulScribeTheme
import com.adll.de_general.core.ui.theme.MindfulTheme
import com.adll.de_general.core.ui.theme.ThemeMode
import com.adll.de_general.feature.settings.ui.components.ThemeModeSelector

/**
 * Settings. Appearance is real; the rest is still a placeholder, and it says so.
 *
 * The design sketches biometrics, offline keys and an NPU readout behind this tab. None of the
 * three exists, and two of them the app cannot honestly claim at all, so this screen promises
 * nothing until there is something to promise.
 */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = spacing.margin, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text("Settings", style = MaterialTheme.typography.displayMedium)

            SectionCard {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Text(
                    "System follows your phone's dark mode setting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ThemeModeSelector(selected = themeMode, onSelect = onThemeModeChange)
            }

            SectionCard {
                Text(
                    "More to come.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Model management, storage and the privacy details belong here. Until they " +
                        "are built there is nothing to show, so there is nothing shown.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    MindfulScribeTheme(darkTheme = false) {
        SettingsScreen(themeMode = ThemeMode.System, onThemeModeChange = {})
    }
}

@Preview
@Composable
private fun SettingsScreenDarkPreview() {
    MindfulScribeTheme(darkTheme = true) {
        SettingsScreen(themeMode = ThemeMode.Dark, onThemeModeChange = {})
    }
}
