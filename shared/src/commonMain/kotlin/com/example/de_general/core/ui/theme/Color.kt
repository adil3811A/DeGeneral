package com.example.de_general.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens for the "Mindful Scribe" design system.
 *
 * Every value below is copied verbatim from the `namedColors` map of the Stitch design system
 * attached to project `13240507270798585972` (AI Daily Journal UI). The design system's prose
 * narrates a few slightly different hexes (canvas `#FBF9F5`, surface-container-low `#F3EFE9`);
 * the token map wins, because that is what the generated screens actually render.
 *
 * Light only. Stitch's `colorMode` is `LIGHT` and it produced no dark tokens, so none are
 * invented here. See [MindfulScribeTheme].
 */

// Primary — restorative sage.
private val Primary = Color(0xFF324F47)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF4A675F)
private val OnPrimaryContainer = Color(0xFFC4E4DA)
private val InversePrimary = Color(0xFFAECDC3)
private val PrimaryFixed = Color(0xFFC9E9DF)
private val PrimaryFixedDim = Color(0xFFAECDC3)
private val OnPrimaryFixed = Color(0xFF02201A)
private val OnPrimaryFixedVariant = Color(0xFF304C45)

// Secondary — warm sand.
private val Secondary = Color(0xFF6B5C4C)
private val OnSecondary = Color(0xFFFFFFFF)
private val SecondaryContainer = Color(0xFFF1DCC8)
private val OnSecondaryContainer = Color(0xFF706050)
private val SecondaryFixed = Color(0xFFF4DFCB)
private val SecondaryFixedDim = Color(0xFFD7C3B0)
private val OnSecondaryFixed = Color(0xFF241A0D)
private val OnSecondaryFixedVariant = Color(0xFF524436)

// Tertiary — twilight lavender.
private val Tertiary = Color(0xFF4F465A)
private val OnTertiary = Color(0xFFFFFFFF)
private val TertiaryContainer = Color(0xFF685D73)
private val OnTertiaryContainer = Color(0xFFE6D8F2)
private val TertiaryFixed = Color(0xFFECDDF7)
private val TertiaryFixedDim = Color(0xFFCFC1DA)
private val OnTertiaryFixed = Color(0xFF20182A)
private val OnTertiaryFixedVariant = Color(0xFF4C4357)

// Error.
private val ErrorColor = Color(0xFFBA1A1A)
private val OnError = Color(0xFFFFFFFF)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)

// Surfaces — unbleached paper, warm and low chroma.
private val Background = Color(0xFFFDF9F5)
private val OnBackground = Color(0xFF1C1C19)
private val Surface = Color(0xFFFDF9F5)
private val OnSurface = Color(0xFF1C1C19)
private val SurfaceVariant = Color(0xFFE6E2DE)
private val OnSurfaceVariant = Color(0xFF414846)
private val SurfaceDim = Color(0xFFDDD9D6)
private val SurfaceBright = Color(0xFFFDF9F5)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF7F3EF)
private val SurfaceContainer = Color(0xFFF1EDE9)
private val SurfaceContainerHigh = Color(0xFFEBE7E4)
private val SurfaceContainerHighest = Color(0xFFE6E2DE)
private val SurfaceTint = Color(0xFF47645C)
private val InverseSurface = Color(0xFF31302E)
private val InverseOnSurface = Color(0xFFF4F0EC)

// Outlines.
private val Outline = Color(0xFF727976)
private val OutlineVariant = Color(0xFFC1C8C4)

/**
 * The single colour scheme for the app.
 *
 * `scrim` is left at the Material default — the Stitch design system does not define one.
 */
val MindfulScribeLightColors: ColorScheme = lightColorScheme(
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
