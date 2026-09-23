package com.adll.de_general.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The "Mindful Scribe" spacing scale, built on an 8pt rhythm with 4pt half-steps for compact
 * pills and micro-chips.
 *
 * Material has no spacing slot, so this rides a [staticCompositionLocalOf] alongside the theme.
 * Read it with [MindfulTheme.spacing].
 */
@Immutable
data class MindfulSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    /** Internal padding for cards and reflection modules. The design system is strict about it. */
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,

    /**
     * Internal padding for cards and reflection modules. The design system asks for 24dp; see the
     * note on [margin] for why this is 16dp instead.
     */
    val cardPadding: Dp = 16.dp,

    val gutter: Dp = 16.dp,
    val gutterTablet: Dp = 24.dp,
    val gutterDesktop: Dp = 32.dp,

    /**
     * Screen gutter. The design system specifies 20dp, tightened to 16dp because it stacks with
     * [cardPadding] on every card: at the design's numbers a phone lost 88dp of 360 to padding
     * before any content was drawn.
     */
    val margin: Dp = 16.dp,
    val marginTablet: Dp = 32.dp,
    val marginDesktop: Dp = 48.dp,
)

/** Breakpoints and reading bounds from the design system's grid anatomy. */
object MindfulLayout {
    /** Below this, a single immersive vertical stream on a 4-column grid. */
    val tabletBreakpoint: Dp = 600.dp

    /** At or above this, a 12-column grid with split views. */
    val desktopBreakpoint: Dp = 1024.dp

    /** Editing mode is bounded to this width to keep line length in the 45-65 character range. */
    val contentMaxWidth: Dp = 860.dp
}

val LocalMindfulSpacing = staticCompositionLocalOf { MindfulSpacing() }
