package com.example.de_general.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulSpacing
import com.example.de_general.core.ui.theme.MindfulTheme
import com.example.de_general.core.ui.theme.MindfulTier

/**
 * The building blocks both onboarding screens share.
 *
 * Every one of these reads its colour, type, spacing and shape from the theme. If something here
 * has a hex or a bare `dp` padding in it, that is a bug — see `docs/THEME.md`.
 */

/**
 * A surface at one of the design system's depth tiers: tonal fill, hairline, optional soft shadow.
 * Use this instead of `Card`, which brings Material's own elevation opinions.
 */
@Composable
fun TieredSurface(
    tier: MindfulTier,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit,
) {
    var box = modifier
        .then(if (tier.shadow > 0.dp) Modifier.shadow(tier.shadow, shape) else Modifier)
        .background(tier.container, shape)
    if (tier.borderAlpha > 0f) {
        box = box.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = tier.borderAlpha),
            shape,
        )
    }
    Box(box) { content() }
}

/** A card at depth tier 1. See [MindfulSpacing.cardPadding] for why it is not the design's 24dp. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    TieredSurface(tier = MindfulTheme.elevation.card, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MindfulTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.md),
            content = content,
        )
    }
}

/**
 * The small pill badges — "100% Offline", "Private On-Device AI", "Zero Cloud Footprint".
 *
 * Tinted at low alpha rather than filled, so a row of them reads as reassurance rather than as a
 * row of buttons competing for a tap.
 */
@Composable
fun Badge(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = MindfulTheme.moods.privacyShield,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.xs),
        modifier = modifier
            .background(tint.copy(alpha = 0.12f), MindfulShapes.full)
            .padding(horizontal = MindfulTheme.spacing.md, vertical = MindfulTheme.spacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** "Step 1 of 3 · Device Check". */
@Composable
fun StepChip(step: Int, total: Int, label: String, modifier: Modifier = Modifier) {
    Text(
        text = "Step $step of $total · $label",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MindfulShapes.full)
            .padding(horizontal = MindfulTheme.spacing.md, vertical = MindfulTheme.spacing.sm),
    )
}

/** A label above a value, used across the resource-footprint card. */
@Composable
fun LabelledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.xs)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleLarge)
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
