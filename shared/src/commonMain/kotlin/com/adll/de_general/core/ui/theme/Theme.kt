package com.adll.de_general.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/**
 * The app's theme, ported from two sibling design systems in Stitch (project
 * `13240507270798585972`, AI Daily Journal UI): **Mindful Scribe** for light and **Nocturnal
 * Sanctuary** for dark. Neither palette is hand-picked at this layer — see `docs/THEME.md`.
 *
 * [darkTheme] defaults to the system setting so previews and tests need not pass it. The app itself
 * passes the resolved [ThemeMode] — System, Light or Dark as chosen in Settings.
 *
 * Material's own slots ([MaterialTheme.colorScheme], [MaterialTheme.typography],
 * [MaterialTheme.shapes]) carry everything Material has a home for. The rest of the design system
 * — spacing, depth tiers, mood accents — rides alongside on composition locals, reachable through
 * [MindfulTheme]. Typography, shapes and spacing are shared by both schemes; the two design systems
 * agree on all three.
 */
@Composable
fun MindfulScribeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) MindfulScribeDarkColors else MindfulScribeLightColors
    val moods = if (darkTheme) MindfulMoodPaletteDark else MindfulMoodPalette
    val elevation = remember(darkTheme) {
        mindfulElevation(colors, if (darkTheme) DarkShadowTint else LightShadowTint)
    }

    CompositionLocalProvider(
        LocalMindfulSpacing provides MindfulSpacing(),
        LocalMindfulElevation provides elevation,
        LocalMoodPalette provides moods,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = mindfulScribeTypography(),
            shapes = MindfulScribeShapes,
            content = content,
        )
    }
}

/**
 * The parts of the design system Material has no slot for.
 *
 * Mirrors the shape of [MaterialTheme], so reading a token looks the same either way:
 * `MaterialTheme.colorScheme.primary` next to `MindfulTheme.spacing.lg`.
 */
object MindfulTheme {
    val spacing: MindfulSpacing
        @Composable @ReadOnlyComposable get() = LocalMindfulSpacing.current

    val elevation: MindfulElevation
        @Composable @ReadOnlyComposable get() = LocalMindfulElevation.current

    val moods: MoodPalette
        @Composable @ReadOnlyComposable get() = LocalMoodPalette.current

    /** Pill and other shapes Material's [MaterialTheme.shapes] has no slot for. */
    val shapes: MindfulShapes get() = MindfulShapes
}
