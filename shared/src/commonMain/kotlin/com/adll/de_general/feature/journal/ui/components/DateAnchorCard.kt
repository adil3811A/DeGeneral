package com.adll.de_general.feature.journal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adll.de_general.core.ui.components.SectionCard
import com.adll.de_general.core.ui.icons.MindfulIcons
import com.adll.de_general.core.ui.icons.Schedule
import com.adll.de_general.core.ui.theme.MindfulTheme
import com.adll.de_general.feature.journal.domain.DateAnchor

private val IconSize: Dp = 20.dp

/**
 * When this entry happened.
 *
 * [Schedule] rather than a calendar glyph — `MindfulIcons` has no calendar, and this card is about
 * *when*, which is what a clock face says. Adding a glyph means hand-splitting Material Symbols
 * path data at fixed columns, and `CLAUDE.md` is explicit about how that goes wrong.
 *
 * The card shows the date and **not** a time. The only honest time to print is the one Save will
 * stamp, and that has not happened yet — see [com.adll.de_general.feature.journal.ui.CreateJournalUiState.dateCaption].
 * The Stitch design's "Current Date" tag is gone for a related reason: it becomes a lie the moment
 * anyone backdates.
 */
@Composable
fun DateAnchorCard(
    anchor: DateAnchor,
    dateLabel: String,
    caption: String,
    onToday: () -> Unit,
    onYesterday: () -> Unit,
    onPickDate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing

    SectionCard(modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                MindfulIcons.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize),
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(dateLabel, style = MaterialTheme.typography.headlineSmall)
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            PresetChip("Today", selected = anchor == DateAnchor.Today, onClick = onToday)
            PresetChip("Yesterday", selected = anchor == DateAnchor.Yesterday, onClick = onYesterday)
            PresetChip("Pick a date", selected = anchor is DateAnchor.On, onClick = onPickDate)
        }
    }
}
