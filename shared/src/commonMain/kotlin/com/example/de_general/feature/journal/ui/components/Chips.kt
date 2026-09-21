package com.example.de_general.feature.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulTheme
import com.example.de_general.feature.journal.domain.JournalMood

/**
 * The composer's three pill chips.
 *
 * Under the feature rather than in `core`, for the reason `DiagnosticRow` is: [MoodChip]
 * pattern-matches a journal domain type, so it belongs to the journal. [PresetChip] and [TagChip]
 * follow it because the three are one visual family and splitting them across two modules to
 * satisfy a rule nobody is breaking would be worse.
 *
 * All three are pills (`MindfulShapes.full`) — the design system's "pill architecture" covers
 * buttons, mood chips and category filters alike.
 */

/** The design system's 6px chip dot. A component dimension, not a spacing decision. */
private val ChipDotSize: Dp = 6.dp

/**
 * One mood.
 *
 * Selected, it fills with that mood's accent container; unselected it is a quiet tonal pill. The
 * dot carries the accent either way, so the colour is readable before you commit to it.
 *
 * Two of the six moods share an accent with another two — see [JournalMood] for why the palette was
 * not extended to six. Only one chip is ever selected, and the label is what distinguishes them.
 */
@Composable
fun MoodChip(
    mood: JournalMood,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MindfulTheme.moods[mood.accent]
    val spacing = MindfulTheme.spacing

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MindfulShapes.full,
        color = if (selected) accent.container else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) {
            accent.onContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .size(ChipDotSize)
                    .background(accent.accent, CircleShape),
            )
            Text(mood.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A plain choice pill — Today, Yesterday, Pick a date. */
@Composable
fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MindfulShapes.full,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
        )
    }
}

/**
 * One tag, with a remove affordance.
 *
 * The "×" is a character, not an icon: `MindfulIcons` has no close glyph, and every new glyph is
 * Material Symbols path data that has to be split at fixed columns — which `CLAUDE.md` names as the
 * standard way to silently corrupt a shape. `EntryCard` set this precedent with its Delete text
 * button. The tap target carries a real label, so it is not a mystery to a screen reader.
 */
@Composable
fun TagChip(tag: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MindfulTheme.spacing

    Surface(
        modifier = modifier,
        shape = MindfulShapes.full,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(start = spacing.md, end = spacing.sm, top = spacing.sm, bottom = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("#$tag", style = MaterialTheme.typography.labelLarge)
            Text(
                "×",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable(onClickLabel = "Remove the tag $tag", onClick = onRemove)
                    .padding(horizontal = spacing.xs),
            )
        }
    }
}
