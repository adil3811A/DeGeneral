package com.example.de_general.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/**
 * The app's theme, ported from the "Mindful Scribe" design system in Stitch
 * (project `13240507270798585972`, AI Daily Journal UI).
 *
 * Light only, on purpose: the upstream design system's `colorMode` is `LIGHT` and it defines no
 * dark tokens, so there is deliberately no [androidx.compose.foundation.isSystemInDarkTheme]
 * branch here. Adding dark means generating dark tokens in Stitch first and porting them the same
 * way [MindfulScribeLightColors] was ported — not hand-picking colours at this layer.
 *
 * Material's own slots ([MaterialTheme.colorScheme], [MaterialTheme.typography],
 * [MaterialTheme.shapes]) carry everything Material has a home for. The rest of the design system
 * — spacing, depth tiers, mood accents — rides alongside on composition locals, reachable through
 * [MindfulTheme].
 */
@Composable
fun MindfulScribeTheme(content: @Composable () -> Unit) {
    val colors = MindfulScribeLightColors
    val elevation = remember(colors) { mindfulElevation(colors) }

    CompositionLocalProvider(
        LocalMindfulSpacing provides MindfulSpacing(),
        LocalMindfulElevation provides elevation,
        LocalMoodPalette provides MindfulMoodPalette,
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
