package com.example.de_general.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Dark colour tokens, ported from the "Nocturnal Sanctuary" design system in Stitch — the dark
 * sibling of Mindful Scribe in the same project (`13240507270798585972`, asset
 * `3d19f1af088440eaa8bba592336c39d2`, `colorMode: DARK`).
 *
 * Shaped exactly like [Color.kt][MindfulScribeLightColors] — same private names, same order — so
 * the two read side by side and `tools/check_theme_tokens.py` parses both with one function. The
 * names are file-private, so they do not clash.
 *
 * **Only the snake_case keys of `namedColors` are ported.** Nocturnal Sanctuary's map also carries
 * hyphenated duplicates lifted from its prose, and some disagree with the snake_case set
 * (`surface-container #212624` against `surface_container #1d201f`, `on-surface #f0eee9` against
 * `on_surface #e1e3e1`). The snake_case set is the Material scheme Stitch generated and lines up
 * key for key with the light map, so it wins — the same "token map wins" rule `THEME.md` applies
 * to the light system's prose. If the hyphenated values turn out to be what looks right, fix the
 * snake_case ones in Stitch and re-port; do not merge the sets here.
 *
 * The dark system is not a hue-for-hue inversion of the light one: its secondary is a second sage
 * and its tertiary a cool grey, where light has warm sand and lavender. That is upstream's call.
 */

// Primary — luminous sage.
private val Primary = Color(0xFFA5D1C3)
private val OnPrimary = Color(0xFF0A372E)
private val PrimaryContainer = Color(0xFF8AB5A8)
private val OnPrimaryContainer = Color(0xFF1D473D)
private val InversePrimary = Color(0xFF3D665B)
private val PrimaryFixed = Color(0xFFBFECDE)
private val PrimaryFixedDim = Color(0xFFA4D0C2)
private val OnPrimaryFixed = Color(0xFF00201A)
private val OnPrimaryFixedVariant = Color(0xFF254E44)

// Secondary — eucalyptus. Not the light theme's warm sand; see the file KDoc.
private val Secondary = Color(0xFFA2D0C3)
private val OnSecondary = Color(0xFF06372E)
private val SecondaryContainer = Color(0xFF255047)
private val OnSecondaryContainer = Color(0xFF94C2B5)
private val SecondaryFixed = Color(0xFFBEECDE)
private val SecondaryFixedDim = Color(0xFFA2D0C3)
private val OnSecondaryFixed = Color(0xFF00201A)
private val OnSecondaryFixedVariant = Color(0xFF234E44)

// Tertiary — cool stone grey. Not the light theme's lavender; see the file KDoc.
private val Tertiary = Color(0xFFC2C9C5)
private val OnTertiary = Color(0xFF2B322F)
private val TertiaryContainer = Color(0xFFA7AEAA)
private val OnTertiaryContainer = Color(0xFF3B423F)
private val TertiaryFixed = Color(0xFFDDE4E0)
private val TertiaryFixedDim = Color(0xFFC1C8C4)
private val OnTertiaryFixed = Color(0xFF161D1A)
private val OnTertiaryFixedVariant = Color(0xFF414845)

// Error.
private val ErrorColor = Color(0xFFFFB4AB)
private val OnError = Color(0xFF690005)
private val ErrorContainer = Color(0xFF93000A)
private val OnErrorContainer = Color(0xFFFFDAD6)

// Surfaces — warm obsidian, never pitch black.
private val Background = Color(0xFF111413)
private val OnBackground = Color(0xFFE1E3E1)
private val Surface = Color(0xFF111413)
private val OnSurface = Color(0xFFE1E3E1)
private val SurfaceVariant = Color(0xFF323534)
private val OnSurfaceVariant = Color(0xFFC0C8C4)
private val SurfaceDim = Color(0xFF111413)
private val SurfaceBright = Color(0xFF373A38)
private val SurfaceContainerLowest = Color(0xFF0C0F0E)
private val SurfaceContainerLow = Color(0xFF191C1B)
private val SurfaceContainer = Color(0xFF1D201F)
private val SurfaceContainerHigh = Color(0xFF282B29)
private val SurfaceContainerHighest = Color(0xFF323534)
private val SurfaceTint = Color(0xFFA4D0C2)
private val InverseSurface = Color(0xFFE1E3E1)
private val InverseOnSurface = Color(0xFF2E3130)

// Outlines.
private val Outline = Color(0xFF4A534F)
private val OutlineVariant = Color(0xFF414845)

/**
 * The dark colour scheme. Selected by [MindfulScribeTheme] when `darkTheme` is true.
 *
 * `scrim` is left at the Material default, as in the light scheme — Stitch defines none.
 */
val MindfulScribeDarkColors: ColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = SurfaceTint,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    error = ErrorColor,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceBright = SurfaceBright,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceDim = SurfaceDim,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
)
