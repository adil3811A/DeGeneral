package com.example.de_general.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
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
import com.example.de_general.ai.CheckStatus
import com.example.de_general.ai.DownloadProgress
import com.example.de_general.ai.InstallState
import com.example.de_general.ai.ModelSpec
import com.example.de_general.ai.formatBytes
import com.example.de_general.ai.formatRemaining
import com.example.de_general.ai.formatSpeed
import com.example.de_general.ui.icons.BatteryCharging
import com.example.de_general.ui.icons.Lightbulb
import com.example.de_general.ui.icons.Lock
import com.example.de_general.ui.icons.Memory
import com.example.de_general.ui.icons.MindfulIcons
import com.example.de_general.ui.icons.PauseCircle
import com.example.de_general.ui.icons.PieChart
import com.example.de_general.ui.icons.PlayCircle
import com.example.de_general.ui.icons.VerifiedUser
import com.example.de_general.ui.onboarding.components.Badge
import com.example.de_general.ui.onboarding.components.LabelledValue
import com.example.de_general.ui.onboarding.components.MilestoneRow
import com.example.de_general.ui.onboarding.components.MilestoneState
import com.example.de_general.ui.onboarding.components.ProgressRing
import com.example.de_general.ui.onboarding.components.SectionCard
import com.example.de_general.ui.onboarding.components.StepChip
import com.example.de_general.ui.theme.MindfulShapes
import com.example.de_general.ui.theme.MindfulTheme

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
                .safeContentPadding()
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
                Text(install.headline, style = MaterialTheme.typography.displayMedium)
                Text(
                    install.subhead(state.spec),
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
    val progress = install.currentProgress

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
                fraction = when (install) {
                    InstallState.Installed -> 1f
                    else -> progress?.fraction ?: 0f
                },
                primaryLabel = when (install) {
                    InstallState.Installed -> "100%"
                    InstallState.Verifying -> "…"
                    else -> "${progress?.percent ?: 0}%"
                },
                secondaryLabel = when (install) {
                    InstallState.Installed -> "Installed"
                    InstallState.Verifying -> "Verifying"
                    is InstallState.Paused -> "Paused"
                    is InstallState.Failed -> "Stopped"
                    else -> "Downloaded"
                },
                color = if (install is InstallState.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }

        if (progress != null && install != InstallState.Installed) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    "${formatBytes(progress.bytesDownloaded)} of " +
                        formatBytes(progress.totalBytes),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (install is InstallState.Downloading) {
                    Text(
                        buildString {
                            append(formatSpeed(progress.bytesPerSecond))
                            progress.secondsRemaining?.let {
                                append(" · ")
                                append(formatRemaining(it))
                                append(" remaining")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (install is InstallState.Failed) {
            Text(
                install.message,
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
            MilestoneRow(
                title = "Hardware & memory",
                detail = when (hardwareVerdict) {
                    null -> "Checked on the previous screen"
                    CheckStatus.Pass -> "This device meets every requirement"
                    CheckStatus.Warn -> "Workable, with tight resources"
                    CheckStatus.Fail -> "This device cannot run the model"
                },
                state = when (hardwareVerdict) {
                    CheckStatus.Fail -> MilestoneState.Failed
                    null -> MilestoneState.Pending
                    else -> MilestoneState.Done
                },
            )
            MilestoneRow(
                title = "Model weights",
                detail = when (install) {
                    is InstallState.Downloading -> "Streaming to this device's private storage"
                    is InstallState.Paused -> "Paused — your progress is kept"
                    InstallState.Verifying, InstallState.Installed -> "Downloaded"
                    is InstallState.Failed -> install.message
                    else -> "Not started"
                },
                state = when (install) {
                    is InstallState.Downloading -> MilestoneState.Active
                    InstallState.Verifying, InstallState.Installed -> MilestoneState.Done
                    is InstallState.Failed -> MilestoneState.Failed
                    else -> MilestoneState.Pending
                },
            )
            MilestoneRow(
                title = "SHA-256 audit",
                detail = "Checks the download byte-for-byte against its published digest",
                state = when (install) {
                    InstallState.Verifying -> MilestoneState.Active
                    InstallState.Installed -> MilestoneState.Done
                    is InstallState.Failed -> MilestoneState.Failed
                    else -> MilestoneState.Pending
                },
            )
            MilestoneRow(
                title = "Engine ready",
                detail = "Weights verified and in place, ready to load",
                state = if (install == InstallState.Installed) {
                    MilestoneState.Done
                } else {
                    MilestoneState.Pending
                },
            )
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

// --- state to copy ---------------------------------------------------------------------------

private val InstallState.currentProgress: DownloadProgress?
    get() = when (this) {
        is InstallState.Downloading -> this.progress
        is InstallState.Paused -> this.progress
        else -> null
    }

private val InstallState.headline: String
    get() = when (this) {
        InstallState.NotInstalled -> "Install the local AI engine"
        is InstallState.Downloading -> "Downloading the model"
        is InstallState.Paused -> "Download paused"
        InstallState.Verifying -> "Verifying the download"
        InstallState.Installed -> "Ready to write"
        is InstallState.Failed -> "Download stopped"
    }

private fun InstallState.subhead(spec: ModelSpec): String = when (this) {
    InstallState.NotInstalled ->
        "${formatBytes(spec.sizeBytes)}, downloaded once and kept on this device. " +
            "Nothing is sent anywhere."
    is InstallState.Downloading -> "Streaming into this app's private storage."
    is InstallState.Paused -> "Your progress is kept. Resume whenever you like."
    InstallState.Verifying -> "Checking the file against its published SHA-256 digest."
    InstallState.Installed -> "The model is installed and verified."
    is InstallState.Failed -> "Nothing was installed. You can try again."
}
