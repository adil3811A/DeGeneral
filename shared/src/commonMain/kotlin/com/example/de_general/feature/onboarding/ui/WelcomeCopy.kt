package com.example.de_general.feature.onboarding.ui

import com.example.de_general.feature.onboarding.domain.CheckStatus
import com.example.de_general.feature.onboarding.domain.CompatibilityReport

/**
 * The welcome screen's verdict card, derived from a [CompatibilityReport].
 *
 * Compose-free, for the same reason as [installHeadline] — see `InstallCopy.kt`. [tone] is an enum
 * rather than a colour so this compiles and tests on every target; the screen maps it to the theme.
 */

enum class VerdictTone { Positive, Caution, Blocked }

data class VerdictCopy(
    val ringFraction: Float,
    val ringPrimaryLabel: String,
    val ringSecondaryLabel: String,
    val title: String,
    val body: String,
    val tone: VerdictTone,
)

fun verdictCopy(report: CompatibilityReport?, probing: Boolean): VerdictCopy = VerdictCopy(
    // checkCompatibility always returns four checks, so `total` is never 0 in the running app.
    // Guarded anyway: a NaN here would reach ProgressRing's animateFloatAsState and then a
    // drawArc sweep, and a silently blank ring is a miserable thing to debug.
    ringFraction = if (report == null || report.total == 0) {
        0f
    } else {
        report.passed.toFloat() / report.total
    },
    ringPrimaryLabel = if (report == null) "—" else "${report.passed}/${report.total}",
    ringSecondaryLabel = if (probing) "Checking" else "Checks passed",
    title = when {
        probing -> "Reading your hardware…"
        report?.verdict == CheckStatus.Pass -> "Ready to run"
        report?.verdict == CheckStatus.Warn -> "Workable, with caveats"
        else -> "Not supported"
    },
    body = when {
        probing -> "Measuring memory, storage and thermal state."
        report?.verdict == CheckStatus.Pass ->
            "Everything the model needs is available on this device."
        report?.verdict == CheckStatus.Warn ->
            "It will run, but one or more resources are tight. Details below."
        else -> "One requirement cannot be met. Details below."
    },
    tone = when (report?.verdict) {
        CheckStatus.Fail -> VerdictTone.Blocked
        CheckStatus.Warn -> VerdictTone.Caution
        else -> VerdictTone.Positive
    },
)
