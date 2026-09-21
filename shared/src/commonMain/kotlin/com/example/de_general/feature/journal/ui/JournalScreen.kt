package com.example.de_general.feature.journal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.components.FloatingNavBarDefaults
import com.example.de_general.core.ui.components.SectionCard
import com.example.de_general.core.ui.icons.ErrorCircle
import com.example.de_general.core.ui.icons.MindfulIcons
import com.example.de_general.core.ui.theme.MindfulScribeTheme
import com.example.de_general.core.ui.theme.MindfulTheme
import com.example.de_general.feature.journal.domain.JournalEntry
import com.example.de_general.feature.journal.domain.decodeTags
import com.example.de_general.feature.journal.domain.formatDayLabel
import com.example.de_general.feature.journal.domain.formatTime
import com.example.de_general.feature.journal.domain.journalMoodOrNull
import kotlinx.datetime.TimeZone

private val ErrorIconSize: Dp = 20.dp

/** The design system's 6px mood dot, the same one the composer's chips carry. */
private val MoodDotSize: Dp = 6.dp

/**
 * What has been written.
 *
 * Takes state and callbacks, never a `NavController` and never the repository — which is what keeps
 * it previewable. Writing is not here: the pencil button pushes
 * [com.example.de_general.navigation.CreateJournal], a full screen of its own.
 *
 * Rows carry a real date, read from the entry's own timestamp against the zone and clock on the
 * state. Nothing here is derived from what time it happens to be when a composable runs.
 */
@Composable
fun JournalScreen(
    state: JournalUiState,
    onDelete: (JournalEntry) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                // safeDrawing, not safeContent: safeContent unions in systemGestures and
                // silently doubles the horizontal inset on gesture-navigation phones.
                .safeDrawingPadding()
                .padding(horizontal = spacing.margin, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text("Your journal", style = MaterialTheme.typography.displayMedium)

            if (state.errorMessage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        MindfulIcons.ErrorCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(ErrorIconSize),
                    )
                    Text(
                        state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissError) {
                        Text("Dismiss", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            when {
                state.loading -> Unit

                state.isEmpty -> Text(
                    "Nothing written yet. The first entry is the hardest.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> LazyColumn(
                    // The bottom bar floats over this screen rather than reserving space, so the
                    // last card has to be told to stop short of it.
                    contentPadding = PaddingValues(
                        bottom = FloatingNavBarDefaults.ContentInset,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        EntryCard(
                            entry = entry,
                            nowMillis = state.nowMillis,
                            zone = state.zone,
                            onDelete = { onDelete(entry) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(
    entry: JournalEntry,
    nowMillis: Long,
    zone: TimeZone,
    onDelete: () -> Unit,
) {
    val spacing = MindfulTheme.spacing
    val mood = journalMoodOrNull(entry.mood)
    val tags = decodeTags(entry.tags)

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${formatDayLabel(entry.timestamp, nowMillis, zone)} · " +
                    formatTime(entry.timestamp, zone),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (mood != null) {
                Spacer(
                    Modifier
                        .size(MoodDotSize)
                        .background(MindfulTheme.moods[mood.accent].accent, CircleShape),
                )
                Text(
                    mood.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (entry.title != null) {
            Text(entry.title, style = MaterialTheme.typography.headlineSmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                entry.rawText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // A text button, not an icon: MindfulIcons has no trash glyph, and the nearest
            // one (ErrorCircle) reads as "something went wrong", not "remove this".
            TextButton(onClick = onDelete) {
                Text("Delete", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (tags.isNotEmpty()) {
            Text(
                tags.joinToString(" ") { "#$it" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 08:45 PM on Thursday 22 October 2026 in `America/New_York` — a real instant, checked, so the
 * preview renders a weekday that actually matches the date. `0L` would have read "Jan 1, 1970".
 */
private const val PREVIEW_NOW = 1_792_716_300_000L

private val PreviewZone = TimeZone.of("America/New_York")

@Preview
@Composable
private fun JournalScreenPreview() {
    MindfulScribeTheme {
        JournalScreen(
            state = JournalUiState(
                loading = false,
                nowMillis = PREVIEW_NOW,
                zone = PreviewZone,
                entries = listOf(
                    JournalEntry(
                        id = 2,
                        rawText = "Slept badly, but the walk helped.",
                        timestamp = PREVIEW_NOW,
                        title = "The long way home",
                        mood = "Calm",
                        tags = "walking\nquiet",
                    ),
                    JournalEntry(id = 1, rawText = "First entry.", timestamp = PREVIEW_NOW - 86_400_000L),
                ),
            ),
            onDelete = {},
            onDismissError = {},
        )
    }
}
