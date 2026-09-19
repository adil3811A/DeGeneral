package com.example.de_general.feature.journal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.components.FloatingNavBarDefaults
import com.example.de_general.core.ui.components.SectionCard
import com.example.de_general.core.ui.icons.ErrorCircle
import com.example.de_general.core.ui.icons.MindfulIcons
import com.example.de_general.core.ui.theme.MindfulScribeTheme
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulTheme
import com.example.de_general.feature.journal.domain.JournalEntry

/**
 * Writing, and what has been written.
 *
 * Takes state and callbacks, never a `NavController` and never the repository — which is what keeps
 * it previewable. Entries are rendered without a date: formatting an epoch timestamp in common code
 * needs a date library this module does not depend on yet, and the newest-first ordering already
 * carries the sequence. A guessed date would be a number the app did not measure.
 */
@Composable
fun JournalScreen(
    state: JournalUiState,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (JournalEntry) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing
    val editor = remember { FocusRequester() }

    // The bottom bar's pencil button has no way to reach into this composition, so it bumps a
    // counter on the shared state instead and the editor answers here. Zero is the initial value
    // and means nobody has asked, which is why the screen does not steal focus on first open.
    LaunchedEffect(state.composeRequest) {
        if (state.composeRequest > 0) editor.requestFocus()
    }

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

            SectionCard {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(editor),
                    placeholder = { Text("What happened today?") },
                    minLines = 3,
                )
                Button(
                    onClick = onSave,
                    enabled = state.canSave,
                    shape = MindfulShapes.full,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(
                        if (state.saving) "Saving…" else "Save entry",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

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
                        modifier = Modifier.size(20.dp),
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
                        EntryCard(entry = entry, onDelete = { onDelete(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: JournalEntry, onDelete: () -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.sm),
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
    }
}

@Preview
@Composable
private fun JournalScreenPreview() {
    MindfulScribeTheme {
        JournalScreen(
            state = JournalUiState(
                loading = false,
                entries = listOf(
                    JournalEntry(id = 2, rawText = "Slept badly, but the walk helped.", timestamp = 0L),
                    JournalEntry(id = 1, rawText = "First entry.", timestamp = 0L),
                ),
                draft = "",
            ),
            onDraftChange = {},
            onSave = {},
            onDelete = {},
            onDismissError = {},
        )
    }
}
