package com.example.de_general.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A specimen of the theme, in whichever scheme it is composed under: every type slot at its real
 * size, the surface and role colours, the five depth tiers, the mood accents and the pill
 * components.
 *
 * This is the reference to check a screen against — if something here looks wrong, the theme is
 * wrong, not the screen.
 */
@Composable
fun MindfulScribeSpecimen(modifier: Modifier = Modifier) {
    val spacing = MindfulTheme.spacing
    val elevation = MindfulTheme.elevation
    val moods = MindfulTheme.moods
    val type = mindfulScribeTypeTokens()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.margin, vertical = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text("Mindful Scribe", style = type.displayLgMobile)
                Text(
                    "Theme specimen — every token, at size.",
                    style = type.bodyMd,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SpecimenSection("Type scale", type.headlineMd) {
                Text("display-lg · Newsreader 40/48", style = type.displayLg)
                Text("display-lg-mobile · 32/40", style = type.displayLgMobile)
                Text("headline-lg · 28/36", style = type.headlineLg)
                Text("headline-md · 24/32", style = type.headlineMd)
                Text("headline-sm · 20/28", style = type.headlineSm)
                Text(
                    "Newsreader italic, for prompts and pull-quotes.",
                    style = type.headlineSm.copy(fontStyle = FontStyle.Italic),
                )
                Text("body-lg · Plus Jakarta Sans 18/28", style = type.bodyLg)
                Text("body-md · 16/24", style = type.bodyMd)
                Text("body-sm · 14/20", style = type.bodySm)
                Text("LABEL-LG · 14/20 w600", style = type.labelLg)
                Text("LABEL-MD · 12/16 w600", style = type.labelMd)
                Text("LABEL-SM · 11/14 w500", style = type.labelSm)
            }

            SpecimenSection("Colour roles", type.headlineMd) {
                val scheme = MaterialTheme.colorScheme
                Swatch("primary", scheme.primary, scheme.onPrimary)
                Swatch("primaryContainer", scheme.primaryContainer, scheme.onPrimaryContainer)
                Swatch("secondaryContainer", scheme.secondaryContainer, scheme.onSecondaryContainer)
                Swatch("tertiaryContainer", scheme.tertiaryContainer, scheme.onTertiaryContainer)
                Swatch("surface", scheme.surface, scheme.onSurface)
                Swatch("surfaceContainerLow", scheme.surfaceContainerLow, scheme.onSurface)
                Swatch("surfaceContainerHighest", scheme.surfaceContainerHighest, scheme.onSurface)
                Swatch("surfaceDim", scheme.surfaceDim, scheme.onSurface)
                Swatch("inverseSurface", scheme.inverseSurface, scheme.inverseOnSurface)
                Swatch("error", scheme.error, scheme.onError)
            }

            SpecimenSection("Depth tiers", type.headlineMd) {
                Tier("L0 · base canvas", elevation.canvas, type.labelLg)
                Tier("L1 · entry card", elevation.card, type.labelLg)
                Tier("L2 · floating sheet", elevation.floating, type.labelLg)
                Tier("L3 · control bar (frosted → tonal)", elevation.bar, type.labelLg)
                Tier("L4 · dialog", elevation.dialog, type.labelLg)
            }

            SpecimenSection("Mood accents", type.headlineMd) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    moods.all.forEach { MoodChip(it, type.labelMd) }
                }
                Text(
                    "feelingColor \"#E8A33D\" snaps to ${moods.nearestTo("#E8A33D").mood}",
                    style = type.bodySm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier
                        .background(moods.privacyShield.copy(alpha = 0.12f), MindfulShapes.full)
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                ) {
                    Text("On-Device Encrypted", style = type.labelSm, color = moods.privacyShield)
                }
            }

            SpecimenSection("Pill components", type.headlineMd) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = {}, shape = MindfulShapes.full, modifier = Modifier.height(48.dp)) {
                        Text("Save entry", style = type.labelLg)
                    }
                    ExtendedFloatingActionButton(onClick = {}, shape = MindfulShapes.full) {
                        Text("Write", style = type.labelLg.copy(fontStyle = FontStyle.Italic))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    listOf(
                        "extraSmall 8" to MaterialTheme.shapes.extraSmall,
                        "small 16" to MaterialTheme.shapes.small,
                        "medium 24" to MaterialTheme.shapes.medium,
                        "large 32" to MaterialTheme.shapes.large,
                    ).forEach { (label, shape) ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, style = type.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecimenSection(
    title: String,
    titleStyle: androidx.compose.ui.text.TextStyle,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.md)) {
        Text(title, style = titleStyle)
        content()
    }
}

@Composable
private fun Swatch(label: String, background: Color, foreground: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
            .padding(horizontal = MindfulTheme.spacing.md, vertical = MindfulTheme.spacing.md),
    ) {
        // The hex is read off the colour actually drawn, so the dark specimen cannot show the
        // light palette's numbers.
        Text(
            "$label ${background.toHexLabel()}",
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
        )
    }
}

private fun Color.toHexLabel(): String =
    "#" + (toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

@Composable
private fun Tier(label: String, tier: MindfulTier, style: androidx.compose.ui.text.TextStyle) {
    val shape = MaterialTheme.shapes.medium
    var modifier = Modifier
        .fillMaxWidth()
        .then(if (tier.shadow > 0.dp) Modifier.shadow(tier.shadow, shape) else Modifier)
        .background(tier.container, shape)
    if (tier.borderAlpha > 0f) {
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = tier.borderAlpha),
            shape,
        )
    }
    Box(modifier = modifier.padding(MindfulTheme.spacing.lg)) {
        Text(label, style = style, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MoodChip(accent: MoodAccent, style: androidx.compose.ui.text.TextStyle) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.xs),
        modifier = Modifier
            .height(32.dp)
            .background(accent.container, MindfulShapes.full)
            .padding(horizontal = MindfulTheme.spacing.md),
    ) {
        Box(Modifier.size(6.dp).background(accent.accent, RoundedCornerShape(3.dp)))
        Text(accent.mood.name, style = style, color = accent.onContainer)
    }
}

@Preview
@Composable
private fun MindfulScribeSpecimenPreview() {
    MindfulScribeTheme(darkTheme = false) { MindfulScribeSpecimen() }
}

/** The same specimen on Nocturnal Sanctuary. If one of the two looks wrong, that palette is wrong. */
@Preview
@Composable
private fun MindfulScribeSpecimenDarkPreview() {
    MindfulScribeTheme(darkTheme = true) { MindfulScribeSpecimen() }
}
