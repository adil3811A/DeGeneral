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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adll.de_general.core.ui.components.Badge
import com.adll.de_general.core.ui.components.ProgressRing
import com.adll.de_general.core.ui.components.SectionCard
import com.adll.de_general.core.ui.components.StepChip
import com.adll.de_general.core.ui.icons.ArrowForward
import com.adll.de_general.core.ui.icons.ChevronRight
import com.adll.de_general.core.ui.icons.Lock
import com.adll.de_general.core.ui.icons.Memory
import com.adll.de_general.core.ui.icons.MindfulIcons
import com.adll.de_general.core.ui.icons.VerifiedUser
import com.adll.de_general.core.ui.icons.WifiOff
import com.adll.de_general.core.ui.theme.MindfulShapes
import com.adll.de_general.core.ui.theme.MindfulTheme
import com.adll.de_general.feature.onboarding.domain.CompatibilityReport
import com.adll.de_general.feature.onboarding.domain.ModelSpec
import com.adll.de_general.feature.onboarding.domain.formatBytes
import com.adll.de_general.feature.onboarding.ui.components.DiagnosticRow
import com.adll.de_general.feature.onboarding.ui.components.DiagnosticRowSkeleton

/**
 * Screen 1 — welcome, and an honest account of whether this phone can run the model.
 *
 * Every figure on this screen was measured on the device. The Stitch design also promised NPU
 * detection, a tokens-per-second estimate and a 0-100 compatibility score; none of those can be
 * obtained truthfully before the model has ever run, so they are not here. See the deviation
 * table in the plan and `docs/THEME.md` for the house rules this follows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    state: OnboardingUiState,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing
    var privacySheetOpen by remember { mutableStateOf(false) }

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
                Text("Mindful Scribe", style = MaterialTheme.typography.titleLarge)
                StepChip(step = 1, total = 3, label = "Device check")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Badge(MindfulIcons.WifiOff, "100% offline")
                Badge(MindfulIcons.VerifiedUser, "On-device AI")
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text("Welcome to Mindful Scribe", style = MaterialTheme.typography.displayMedium)
                Text(
                    "Your private sanctuary for thought. Mindful Scribe runs language " +
                        "intelligence directly on this phone, so what you write never leaves it. " +
                        "First, let's see what your hardware can do.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            VerdictCard(state.report, state.probing)

            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Text("Hardware diagnostic", style = MaterialTheme.typography.headlineSmall)
                SectionCard {
                    val report = state.report
                    if (report == null) {
                        repeat(4) { index ->
                            DiagnosticRowSkeleton()
                            if (index < 3) Divider()
                        }
                    } else {
                        report.checks.forEachIndexed { index, check ->
                            DiagnosticRow(check)
                            if (index < report.checks.lastIndex) Divider()
                        }
                    }
                }
            }

            ModelCard(state.spec)

            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Button(
                    onClick = onContinue,
                    shape = MindfulShapes.full,
                    enabled = state.report?.canInstall == true,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("Continue to model setup", style = MaterialTheme.typography.labelLarge)
                    Icon(
                        MindfulIcons.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = spacing.sm).size(18.dp),
                    )
                }

                if (state.report?.canInstall == false) {
                    Text(
                        "This device cannot run the model. You can still write — the AI " +
                            "reflections simply stay switched off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { privacySheetOpen = true }) {
                        Icon(
                            MindfulIcons.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "  Privacy guarantees",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    TextButton(onClick = onSkip) {
                        Text("Skip for now", style = MaterialTheme.typography.labelLarge)
                        Icon(
                            MindfulIcons.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }

    if (privacySheetOpen) {
        ModalBottomSheet(onDismissRequest = { privacySheetOpen = false }) {
            PrivacySheetContent(onDismiss = { privacySheetOpen = false })
        }
    }
}

@Composable
private fun VerdictCard(report: CompatibilityReport?, probing: Boolean) {
    val spacing = MindfulTheme.spacing
    val copy = verdictCopy(report, probing)

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(
                fraction = copy.ringFraction,
                primaryLabel = copy.ringPrimaryLabel,
                secondaryLabel = copy.ringSecondaryLabel,
                diameter = 116.dp,
                thickness = 10.dp,
                color = when (copy.tone) {
                    VerdictTone.Blocked -> MaterialTheme.colorScheme.error
                    VerdictTone.Caution -> MindfulTheme.moods.happy.accent
                    VerdictTone.Positive -> MaterialTheme.colorScheme.primary
                },
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(copy.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    copy.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ModelCard(spec: ModelSpec) {
    val spacing = MindfulTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text("Your AI companion", style = MaterialTheme.typography.headlineSmall)
        SectionCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(spec.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${spec.parameterCount} parameters · ${spec.quantization} · " +
                            formatBytes(spec.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "Tuned for short reflective prompts, gentle grammar repair and sentiment " +
                    "mirroring. It runs on the CPU, entirely offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrivacySheetContent(onDismiss: () -> Unit) {
    val spacing = MindfulTheme.spacing

    Column(
        modifier = Modifier.padding(
            start = spacing.margin,
            end = spacing.margin,
            bottom = spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text("Zero-cloud guarantee", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Your entries never cross a network socket. The model weights are downloaded once, " +
                "stored in this app's private sandbox, and every reflection is generated inside " +
                "your device's own memory.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrivacyPoint("Full functionality in airplane mode, once the model is installed.")
        PrivacyPoint("Nothing you write is uploaded, logged or synced.")
        PrivacyPoint(
            "The one time this app uses the network is the model download on the next screen.",
        )
        Text(
            "On Android 10 and later the operating system encrypts app-private storage at rest. " +
                "The app itself adds no separate encryption layer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onDismiss,
            shape = MindfulShapes.full,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("I understand", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PrivacyPoint(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.sm)) {
        Icon(
            MindfulIcons.Lock,
            contentDescription = null,
            tint = MindfulTheme.moods.privacyShield,
            modifier = Modifier.size(18.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}
