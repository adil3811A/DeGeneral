package com.adll.de_general.feature.onboarding.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adll.de_general.core.ui.icons.CheckCircle
import com.adll.de_general.core.ui.icons.DeveloperBoard
import com.adll.de_general.core.ui.icons.ErrorCircle
import com.adll.de_general.core.ui.icons.HardDrive
import com.adll.de_general.core.ui.icons.Memory
import com.adll.de_general.core.ui.icons.MindfulIcons
import com.adll.de_general.core.ui.icons.Thermostat
import com.adll.de_general.core.ui.icons.Warning
import com.adll.de_general.core.ui.theme.MindfulShapes
import com.adll.de_general.core.ui.theme.MindfulTheme
import com.adll.de_general.feature.onboarding.domain.CheckId
import com.adll.de_general.feature.onboarding.domain.CheckStatus
import com.adll.de_general.feature.onboarding.domain.DeviceCheck

/** One hardware diagnostic: what was measured, and whether it is good enough. */
@Composable
fun DiagnosticRow(check: DeviceCheck, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.shapes.extraSmall,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = check.id.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(check.title, style = MaterialTheme.typography.titleMedium)
                StatusPill(check.status)
            }
            Text(
                check.headline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                check.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The placeholder shown while the device probe is still running. */
@Composable
fun DiagnosticRowSkeleton(modifier: Modifier = Modifier) {
    val bone = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.md),
    ) {
        Box(Modifier.size(40.dp).background(bone, MaterialTheme.shapes.extraSmall))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.sm),
        ) {
            Box(Modifier.width(120.dp).height(14.dp).background(bone, MindfulShapes.full))
            Box(Modifier.fillMaxWidth().height(12.dp).background(bone, MindfulShapes.full))
        }
    }
}

@Composable
private fun StatusPill(status: CheckStatus) {
    val (label, color) = when (status) {
        CheckStatus.Pass -> "Ready" to MindfulTheme.moods.calm.accent
        CheckStatus.Warn -> "Tight" to MindfulTheme.moods.happy.accent
        CheckStatus.Fail -> "Blocked" to MaterialTheme.colorScheme.error
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.xs),
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), MindfulShapes.full)
            .padding(horizontal = MindfulTheme.spacing.sm, vertical = 2.dp),
    ) {
        Icon(
            imageVector = status.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private val CheckId.icon: ImageVector
    get() = when (this) {
        CheckId.Processor -> MindfulIcons.Memory
        CheckId.Memory -> MindfulIcons.DeveloperBoard
        CheckId.Storage -> MindfulIcons.HardDrive
        CheckId.ThermalAndBattery -> MindfulIcons.Thermostat
    }

private val CheckStatus.icon: ImageVector
    get() = when (this) {
        CheckStatus.Pass -> MindfulIcons.CheckCircle
        CheckStatus.Warn -> MindfulIcons.Warning
        CheckStatus.Fail -> MindfulIcons.ErrorCircle
    }

