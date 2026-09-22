package com.example.de_general.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The five depth tiers of the "Mindful Scribe" design system.
 *
 * The system deliberately avoids hard drop shadows: surfaces rise toward the reader through tonal
 * washes and hairline boundaries, with shadow used only as a faint ambient wash on genuinely
 * floating elements. So a tier is a *container colour plus a hairline plus an optional soft
 * shadow*, not a raw elevation number.
 *
 * Two places where Compose cannot reproduce the design exactly, left visible rather than papered
 * over:
 *  - The design specifies two-layer shadows (`0px 4px 16px -2px …, 0px 1px 4px 0px …`).
 *    `Modifier.shadow` renders a single layer, so [MindfulTier.shadow] is one softer
 *    approximation of the pair.
 *  - Tier 3 is frosted glass (`backdrop-filter: blur(16px) saturate(140%)`). Compose Multiplatform
 *    has no portable backdrop blur, so the tier falls back to an opaque tonal surface.
 *    TODO: revisit once a portable blur lands; it is a one-line change here.
 */
@Immutable
data class MindfulTier(
    /** The surface colour that carries the tier. */
    val container: Color,
    /** Single-layer approximation of the design's soft ambient shadow. */
    val shadow: Dp,
    /** Alpha for the 1px hairline boundary, drawn in `outlineVariant`. Zero means no border. */
    val borderAlpha: Float,
)

@Immutable
data class MindfulElevation(
    /** Level 0 — base canvas. Background reading and continuous body prose. */
    val canvas: MindfulTier,
    /** Level 1 — timeline entry cards and daily prompts. Tonal, no shadow, hairline bounded. */
    val card: MindfulTier,
    /** Level 2 — insight sheets, active mood selectors, quick-add modals. */
    val floating: MindfulTier,
    /** Level 3 — navigation and floating control bars. Frosted in the design; see the file KDoc. */
    val bar: MindfulTier,
    /** Level 4 — privacy audit modals, passcode and biometric prompts. */
    val dialog: MindfulTier,
    /** Ambient/spot shadow colour. Per scheme — see [LightShadowTint] and [DarkShadowTint]. */
    val shadowTint: Color,
)

/** Warm charcoal, per Mindful Scribe's `rgba(60, 52, 42, …)` shadows. */
val LightShadowTint = Color(0xFF3C342A)

/**
 * Plain black, per Nocturnal Sanctuary's `rgba(0, 0, 0, …)` shadows. A warm cast would read as mud
 * against an obsidian canvas.
 */
val DarkShadowTint = Color(0xFF000000)

/**
 * The tiers, derived from whichever scheme is active — so dark mode needs no second table.
 *
 * Nocturnal Sanctuary's prose names its own tier surfaces (`#212624` for cards, for instance).
 * Those are the hyphenated tokens `ColorDark.kt` deliberately does not port, so the tiers follow
 * the snake_case scheme here too. Its tier 3 asks for 80% translucency plus `blur(20px)`; that is
 * the same missing portable blur as the light tier 3, noted in the file KDoc.
 */
fun mindfulElevation(colors: ColorScheme, shadowTint: Color): MindfulElevation = MindfulElevation(
    canvas = MindfulTier(colors.surface, 0.dp, borderAlpha = 0f),
    card = MindfulTier(colors.surfaceContainerLow, 0.dp, borderAlpha = 0.4f),
    floating = MindfulTier(colors.surfaceContainerLowest, 4.dp, borderAlpha = 0f),
    bar = MindfulTier(colors.surfaceContainer, 2.dp, borderAlpha = 0.3f),
    dialog = MindfulTier(colors.surfaceContainerLowest, 12.dp, borderAlpha = 0f),
    shadowTint = shadowTint,
)

val LocalMindfulElevation =
    staticCompositionLocalOf { mindfulElevation(MindfulScribeLightColors, LightShadowTint) }
