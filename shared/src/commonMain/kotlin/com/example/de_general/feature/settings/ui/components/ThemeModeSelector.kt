package com.example.de_general.feature.settings.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulTheme
import com.example.de_general.core.ui.theme.ThemeMode

/**
 * System · Light · Dark, as three pills.
 *
 * Shaped like the journal's `PresetChip` — copied, not imported: a feature never imports another
 * feature. If a third place wants this pill it moves to `core/ui/components` in its own change.
 *
 * One choice of three, so it is a `selectableGroup` of radio-role pills: TalkBack reads "Dark,
 * selected, 3 of 3" rather than three unrelated buttons.
 */
@Composable
fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing

    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        ThemeMode.entries.forEach { mode ->
            ThemeModePill(
                label = mode.label,
                selected = mode == selected,
                onClick = { onSelect(mode) },
            )
        }
    }
}

@Composable
private fun ThemeModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val spacing = MindfulTheme.spacing

    Surface(
        // Clipped before selectable so the ripple is a pill, not a rectangle behind one.
        modifier = Modifier
            .clip(MindfulShapes.full)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = MindfulShapes.full,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        // The design's 1px hairline. A stroke width, not a spacing decision.
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
        )
    }
}
