package com.adll.de_general.feature.onboarding.domain

/**
 * Where the model install has got to.
 *
 * The four setup milestones the install screen draws map onto these:
 *  1. hardware check — decided before the installer runs, by [checkCompatibility]
 *  2. weights — [Downloading] / [Paused]
 *  3. SHA-256 audit — [Verifying]
 *  4. engine ready — [Installed]
 */
sealed interface InstallState {
    /** Nothing downloaded yet. */
    data object NotInstalled : InstallState

    data class Downloading(val progress: DownloadProgress) : InstallState

    /** Stopped by the user; the partial file is kept and [DownloadProgress.bytesDownloaded] holds. */
    data class Paused(val progress: DownloadProgress) : InstallState

    /** Hashing the completed file. No progress figure: it is a single fast pass over the file. */
    data object Verifying : InstallState

    data object Installed : InstallState

    data class Failed(val reason: InstallFailure, val message: String) : InstallState
}

enum class InstallFailure {
    /** No connection, DNS failure, timeout, connection reset. Retrying is reasonable. */
    Network,

    /** Ran out of room mid-write. */
    OutOfSpace,

    /** The finished file's digest did not match [ModelSpec.sha256]. */
    ChecksumMismatch,

    /** Server answered something unusable — 404, 403, a 200 to a range request we cannot use. */
    Server,

    Unknown,
}

/**
 * A progress sample.
 *
 * @param bytesPerSecond smoothed, not instantaneous — see the EWMA in [ModelInstaller].
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val bytesPerSecond: Double,
) {
    /** 0f..1f, and 0f rather than NaN when the total is unknown. */
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f else (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)

    val percent: Int get() = (fraction * 100).toInt()

    /** Seconds left at the current smoothed rate, or null when it cannot be estimated yet. */
    val secondsRemaining: Long?
        get() {
            if (bytesPerSecond <= 0.0 || totalBytes <= 0L) return null
            val remaining = totalBytes - bytesDownloaded
            if (remaining <= 0L) return 0L
            return (remaining / bytesPerSecond).toLong()
        }
}
