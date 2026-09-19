package com.example.de_general.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.icons.Check
import com.example.de_general.core.ui.icons.Download
import com.example.de_general.core.ui.icons.ErrorCircle
import com.example.de_general.core.ui.icons.MindfulIcons
import com.example.de_general.core.ui.icons.Schedule
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulTheme

/**
 * One row of the "Setup Milestones" list.
 *
 * The active row breathes gently rather than spinning. A spinner on a step that takes ten minutes
 * reads as a hang; a slow pulse reads as patience, which is closer to the truth and closer to what
 * the design system asks of every animation here.
 */
@Composable
fun MilestoneRow(
    title: String,
    detail: String,
    state: MilestoneState,
    modifier: Modifier = Modifier,
) {
    val accent = when (state) {
        MilestoneState.Done -> MindfulTheme.moods.calm.accent
        MilestoneState.Active -> MaterialTheme.colorScheme.primary
        MilestoneState.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
        MilestoneState.Failed -> MaterialTheme.colorScheme.error
    }

    val pulse by rememberInfiniteTransition(label = "milestone").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .alpha(if (state == MilestoneState.Active) pulse else 1f)
                .background(accent.copy(alpha = 0.14f), MindfulShapes.full),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = state.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state == MilestoneState.Pending) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(state.label, style = MaterialTheme.typography.labelSmall, color = accent)
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val MilestoneState.label: String
    get() = when (this) {
        MilestoneState.Done -> "Done"
        MilestoneState.Active -> "In progress"
        MilestoneState.Pending -> "Queued"
        MilestoneState.Failed -> "Failed"
    }

private val MilestoneState.icon: ImageVector
    get() = when (this) {
        MilestoneState.Done -> MindfulIcons.Check
        MilestoneState.Active -> MindfulIcons.Download
        MilestoneState.Pending -> MindfulIcons.Schedule
        MilestoneState.Failed -> MindfulIcons.ErrorCircle
    }
