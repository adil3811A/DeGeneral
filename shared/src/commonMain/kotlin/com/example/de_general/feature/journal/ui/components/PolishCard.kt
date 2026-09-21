package com.example.de_general.feature.journal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.components.SectionCard
import com.example.de_general.core.ui.icons.Check
import com.example.de_general.core.ui.icons.MindfulIcons
import com.example.de_general.core.ui.icons.Psychology
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulTheme
import com.example.de_general.feature.journal.ui.PolishState

private val IconSize: Dp = 20.dp
private val ButtonHeight: Dp = 52.dp

/**
 * The local polish pass.
 *
 * Every string here is either measured or named: the model label comes from the pinned `ModelSpec`,
 * the duration is timed across the real call, and the button says which of the two slow things is
 * happening ("Loading the model…" / "Refining…"). The design's "(Local 0ms)" and "Neural Core" are
 * both gone — one was a latency nothing measured, the other a name for a chip that does not exist.
 *
 * The suggestion is rendered as a **pull-quote** in italic Newsreader, which is the treatment
 * `docs/THEME.md` names for exactly this, and it is not editable here — accepting it moves it into
 * the body field, which is where editing happens.
 *
 * "Keep this version" **replaces** the entry, and the title with it when one was suggested. The
 * card then shows a single undo, and shows it only while putting the swap back would be a pure
 * reversal. After Save there is no undo: the entry is what the fields hold.
 */
@Composable
fun PolishCard(
    state: PolishState,
    refineLabel: String,
    refineCaption: String,
    refinedInLabel: String?,
    modelLabel: String,
    canRefine: Boolean,
    canUndo: Boolean,
    undoLabel: String,
    onRefine: () -> Unit,
    onAccept: () -> Unit,
    onDiscard: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing
    val quote = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic)

    SectionCard(modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                MindfulIcons.Psychology,
                contentDescription = null,
                tint = MindfulTheme.moods.privacyShield,
                modifier = Modifier.size(IconSize),
            )
            Text("Refine with the local model", style = MaterialTheme.typography.headlineSmall)
        }

        Text(
            modelLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = onRefine,
            enabled = canRefine,
            shape = MindfulShapes.full,
            modifier = Modifier.fillMaxWidth().height(ButtonHeight),
        ) {
            Text(refineLabel, style = MaterialTheme.typography.labelLarge)
        }

        when (state) {
            PolishState.Idle, PolishState.Loading -> Unit

            is PolishState.Running ->
                // Empty until the first token lands. The button already says "Refining…", so an
                // empty quote box would be a second, quieter way of saying the same thing.
                if (state.partial.isNotBlank()) Text(state.partial, style = quote)

            // The correction is finished and stays on screen while the title is written, so the
            // card does not blank out between the two passes.
            is PolishState.Naming -> Text(state.text, style = quote)

            is PolishState.Ready -> {
                if (state.title != null) {
                    SuggestedTitle(state.title)
                }
                Text(state.text, style = quote)
                if (refinedInLabel != null) {
                    Text(
                        refinedInLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    refineCaption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onAccept, shape = MindfulShapes.full) {
                        Text("Keep this version", style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(onClick = onDiscard) {
                        Text("Discard", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            is PolishState.Failed -> Text(
                state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (canUndo) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    MindfulIcons.Check,
                    contentDescription = null,
                    tint = MindfulTheme.moods.privacyShield,
                    modifier = Modifier.size(IconSize),
                )
                Text(
                    undoLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onUndo) {
                    Text("Undo", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/**
 * The title the model suggested, above the corrected entry.
 *
 * Labelled, because a bare line of serif text above a pull-quote would read as the entry's first
 * sentence rather than as a separate thing being offered.
 */
@Composable
private fun SuggestedTitle(title: String) {
    // Its own Column so the label hugs the title, rather than inheriting the card's 16dp rhythm
    // and floating halfway between the title and whatever sits above it.
    Column(verticalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.xs)) {
        Text(
            "SUGGESTED TITLE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}
