package com.adll.de_general.feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adll.de_general.core.ui.components.Badge
import com.adll.de_general.core.ui.components.LabelledValue
import com.adll.de_general.core.ui.components.MilestoneRow
import com.adll.de_general.core.ui.components.ProgressRing
import com.adll.de_general.core.ui.components.SectionCard
import com.adll.de_general.core.ui.components.StepChip
import com.adll.de_general.core.ui.icons.BatteryCharging
import com.adll.de_general.core.ui.icons.Lightbulb
import com.adll.de_general.core.ui.icons.Lock
import com.adll.de_general.core.ui.icons.Memory
import com.adll.de_general.core.ui.icons.MindfulIcons
import com.adll.de_general.core.ui.icons.PauseCircle
import com.adll.de_general.core.ui.icons.PieChart
import com.adll.de_general.core.ui.icons.PlayCircle
import com.adll.de_general.core.ui.icons.VerifiedUser
import com.adll.de_general.core.ui.theme.MindfulShapes
import com.adll.de_general.core.ui.theme.MindfulTheme
import com.adll.de_general.feature.onboarding.domain.CheckStatus
import com.adll.de_general.feature.onboarding.domain.InstallState
import com.adll.de_general.feature.onboarding.domain.ModelSpec
import com.adll.de_general.feature.onboarding.domain.formatBytes

/**
 * Screen 2 — fetch the weights, and prove they arrived intact.
 *
 * The four milestones match the Stitch design's shape, but the fourth is honest: the design called
 * it "quantization & cache compilation", which is not a step that exists for a GGUF file. Here it
 * means the digest matched and the file is in place. When a real inference engine lands it should
 * become a genuine load-and-warm-up, which is what that milestone always wanted to be.
 */
@Composable
fun InstallScreen(
    state: OnboardingUiState,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onRetry: () -> Unit,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing
    val install = state.install

    KeepScreenAwake(state.keepScreenAwake && install is InstallState.Downloading)

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                // safeDrawing, not safeContent: safeContent unions in systemGestures,
                // which reserves a ~40dp back-swipe strip down each edge and silently
                // doubles the horizontal inset on gesture-navigation phones.
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.margin, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Local AI engine", style = MaterialTheme.typography.titleLarge)
                StepChip(step = 2, total = 3, label = "Install")
            }

            Badge(MindfulIcons.VerifiedUser, "Zero cloud footprint")

            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(installHeadline(install), style = MaterialTheme.typography.displayMedium)
                Text(
                    installSubhead(install, state.spec),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ProgressCard(install, state.spec)

            MilestonesCard(install, state.report?.verdict)

            FootprintCard(state)

            HintRow(
                icon = MindfulIcons.BatteryCharging,
                text = "Wi-Fi is recommended. Keep the app open, or plug in, while this runs.",
            )

            KeepAwakeRow(state.keepScreenAwake, onKeepScreenAwakeChange)

            Actions(
                install = install,
                onStartOrResume = onStartOrResume,
                onPause = onPause,
                onRetry = onRetry,
                onFinish = onFinish,
            )

            Text(
                "Once installed, reflection prompts, grammar polishing and sentiment insights all " +
                    "run on this device. Your writing never reaches an external server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressCard(install: InstallState, spec: ModelSpec) {
    val spacing = MindfulTheme.spacing
    val copy = installProgressCopy(install)

    SectionCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.shapes.extraSmall,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    MindfulIcons.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(spec.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${spec.quantization} · ${formatBytes(spec.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(
                fraction = copy.ringFraction,
                primaryLabel = copy.ringPrimaryLabel,
                secondaryLabel = copy.ringSecondaryLabel,
                color = if (copy.isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }

        if (copy.transferLine != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(copy.transferLine, style = MaterialTheme.typography.bodyMedium)
                if (copy.rateLine != null) {
                    Text(
                        copy.rateLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (copy.errorMessage != null) {
            Text(
                copy.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun MilestonesCard(install: InstallState, hardwareVerdict: CheckStatus?) {
    val spacing = MindfulTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text("Setup milestones", style = MaterialTheme.typography.headlineSmall)
        SectionCard {
            setupMilestones(install, hardwareVerdict).forEach { milestone ->
                MilestoneRow(
                    title = milestone.title,
                    detail = milestone.detail,
                    state = milestone.state,
                )
            }
        }
    }
}

@Composable
private fun FootprintCard(state: OnboardingUiState) {
    val spacing = MindfulTheme.spacing
    val snapshot = state.snapshot

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                MindfulIcons.PieChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text("Resource footprint", style = MaterialTheme.typography.headlineSmall)
        }
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                LabelledValue(
                    label = "Device storage",
                    value = snapshot?.let { formatBytes(it.freeDiskBytes) } ?: "—",
                    caption = snapshot?.let { "free of ${formatBytes(it.totalDiskBytes)}" },
                    modifier = Modifier.weight(1f),
                )
                LabelledValue(
                    label = "Model footprint",
                    value = formatBytes(state.spec.sizeBytes),
                    caption = "~${formatBytes(state.spec.requiredRamBytes)} RAM when loaded",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HintRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeepAwakeRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                MindfulIcons.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f)) {
                Text("Keep screen awake", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Stops the device sleeping mid-download",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun Actions(
    install: InstallState,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onRetry: () -> Unit,
    onFinish: () -> Unit,
) {
    val spacing = MindfulTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        when (install) {
            is InstallState.Downloading -> OutlinedButton(
                onClick = onPause,
                shape = MindfulShapes.full,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(MindfulIcons.PauseCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Pause download", style = MaterialTheme.typography.labelLarge)
            }

            InstallState.Verifying -> OutlinedButton(
                onClick = {},
                enabled = false,
                shape = MindfulShapes.full,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Verifying…", style = MaterialTheme.typography.labelLarge)
            }

            InstallState.Installed -> Button(
                onClick = onFinish,
                shape = MindfulShapes.full,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(MindfulIcons.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Start writing", style = MaterialTheme.typography.labelLarge)
            }

            is InstallState.Failed -> Button(
                onClick = onRetry,
                shape = MindfulShapes.full,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Try again", style = MaterialTheme.typography.labelLarge)
            }

            is InstallState.Paused -> Button(
                onClick = onStartOrResume,
                shape = MindfulShapes.full,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(MindfulIcons.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Resume download", style = MaterialTheme.typography.labelLarge)
            }

            InstallState.NotInstalled -> Button(
                onClick = onStartOrResume,
                shape = MindfulShapes.full,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Download model", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (install == InstallState.Installed) {
            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text("Continue", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
