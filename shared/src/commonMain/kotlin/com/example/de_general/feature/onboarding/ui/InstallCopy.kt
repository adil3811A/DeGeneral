package com.example.de_general.feature.onboarding.ui

import com.example.de_general.core.ui.components.MilestoneState
import com.example.de_general.feature.onboarding.domain.CheckStatus
import com.example.de_general.feature.onboarding.domain.DownloadProgress
import com.example.de_general.feature.onboarding.domain.InstallState
import com.example.de_general.feature.onboarding.domain.ModelSpec
import com.example.de_general.feature.onboarding.domain.formatBytes
import com.example.de_general.feature.onboarding.domain.formatRemaining
import com.example.de_general.feature.onboarding.domain.formatSpeed

/**
 * Every string and figure the install screen shows, derived from [InstallState] alone.
 *
 * Compose-free on purpose. This is the same rule [com.example.de_general.feature.onboarding.domain.checkCompatibility]
 * follows: logic that can be a pure function should be one, so it can be tested in `commonTest` on
 * every target rather than only inside a running composition.
 *
 * Colours are not decided here — they are a boolean or an enum, and the screen maps them to the
 * theme. That is what keeps this file free of a Compose import.
 */

/** The progress sample for the states that carry one; null for the states that do not. */
fun InstallState.progressOrNull(): DownloadProgress? = when (this) {
    is InstallState.Downloading -> progress
    is InstallState.Paused -> progress
    else -> null
}

fun installHeadline(install: InstallState): String = when (install) {
    InstallState.NotInstalled -> "Install the local AI engine"
    is InstallState.Downloading -> "Downloading the model"
    is InstallState.Paused -> "Download paused"
    InstallState.Verifying -> "Verifying the download"
    InstallState.Installed -> "Ready to write"
    is InstallState.Failed -> "Download stopped"
}

fun installSubhead(install: InstallState, spec: ModelSpec): String = when (install) {
    InstallState.NotInstalled ->
        "${formatBytes(spec.sizeBytes)}, downloaded once and kept on this device. " +
            "Nothing is sent anywhere."
    is InstallState.Downloading -> "Streaming into this app's private storage."
    is InstallState.Paused -> "Your progress is kept. Resume whenever you like."
    InstallState.Verifying -> "Checking the file against its published SHA-256 digest."
    InstallState.Installed -> "The model is installed and verified."
    is InstallState.Failed -> "Nothing was installed. You can try again."
}

/**
 * What the progress card draws.
 *
 * [transferLine] and [rateLine] are null when there is nothing measured to say — the screen omits
 * the row rather than printing a zero it did not measure.
 */
data class InstallProgressCopy(
    val ringFraction: Float,
    val ringPrimaryLabel: String,
    val ringSecondaryLabel: String,
    val isError: Boolean,
    val transferLine: String?,
    val rateLine: String?,
    val errorMessage: String?,
)

fun installProgressCopy(install: InstallState): InstallProgressCopy {
    val progress = install.progressOrNull()
    val showTransfer = progress != null && install != InstallState.Installed

    return InstallProgressCopy(
        ringFraction = when (install) {
            InstallState.Installed -> 1f
            else -> progress?.fraction ?: 0f
        },
        ringPrimaryLabel = when (install) {
            InstallState.Installed -> "100%"
            InstallState.Verifying -> "…"
            else -> "${progress?.percent ?: 0}%"
        },
        ringSecondaryLabel = when (install) {
            InstallState.Installed -> "Installed"
            InstallState.Verifying -> "Verifying"
            is InstallState.Paused -> "Paused"
            is InstallState.Failed -> "Stopped"
            else -> "Downloaded"
        },
        isError = install is InstallState.Failed,
        transferLine = if (showTransfer && progress != null) {
            "${formatBytes(progress.bytesDownloaded)} of ${formatBytes(progress.totalBytes)}"
        } else {
            null
        },
        rateLine = if (showTransfer && progress != null && install is InstallState.Downloading) {
            buildString {
                append(formatSpeed(progress.bytesPerSecond))
                progress.secondsRemaining?.let {
                    append(" · ")
                    append(formatRemaining(it))
                    append(" remaining")
                }
            }
        } else {
            null
        },
        errorMessage = (install as? InstallState.Failed)?.message,
    )
}

/** Which of the four setup milestones a row is. Fixed set, fixed order. */
enum class MilestoneKind { Hardware, Weights, Audit, Engine }

data class MilestoneCopy(
    val kind: MilestoneKind,
    val title: String,
    val detail: String,
    val state: MilestoneState,
)

/** The four setup milestones, in the order the screen draws them. */
fun setupMilestones(
    install: InstallState,
    hardwareVerdict: CheckStatus?,
): List<MilestoneCopy> = listOf(
    MilestoneCopy(
        kind = MilestoneKind.Hardware,
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
    ),
    MilestoneCopy(
        kind = MilestoneKind.Weights,
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
    ),
    MilestoneCopy(
        kind = MilestoneKind.Audit,
        title = "SHA-256 audit",
        detail = "Checks the download byte-for-byte against its published digest",
        state = when (install) {
            InstallState.Verifying -> MilestoneState.Active
            InstallState.Installed -> MilestoneState.Done
            is InstallState.Failed -> MilestoneState.Failed
            else -> MilestoneState.Pending
        },
    ),
    MilestoneCopy(
        kind = MilestoneKind.Engine,
        title = "Engine ready",
        detail = "Weights verified and in place, ready to load",
        state = if (install == InstallState.Installed) {
            MilestoneState.Done
        } else {
            MilestoneState.Pending
        },
    ),
)
