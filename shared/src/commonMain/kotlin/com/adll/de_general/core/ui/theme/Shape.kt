package com.adll.de_general.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The "Mindful Scribe" roundness scale (`roundness: ROUND_FULL`).
 *
 * Curves are generous throughout — the design system treats them as a calming signal, not a
 * decoration. Nested children should use the parent radius minus the internal padding so corners
 * stay concentric.
 */
val MindfulScribeShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // rounded.sm    — habit checkboxes, small chips
    small = RoundedCornerShape(16.dp),       // rounded       — media attachments, text highlights
    medium = RoundedCornerShape(24.dp),      // rounded.md    — journal entry cards, AI panels
    large = RoundedCornerShape(32.dp),       // rounded.lg    — bottom sheets, large surfaces
    extraLarge = RoundedCornerShape(48.dp),  // rounded.xl    — full-bleed containers
)

/** Shapes the design system uses that Material's [Shapes] has no slot for. */
object MindfulShapes {
    /**
     * `rounded-full`. The pill architecture: primary buttons, mood chips, category filters,
     * the audio recording indicator and the on-device privacy badge.
     */
    val full = CircleShape
}
