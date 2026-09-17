package com.example.de_general.ai

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readAvailable
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.blackholeSink
import okio.buffer

/** Where the installer does its blocking file work. `Dispatchers.IO` is JVM-only. */
internal expect val installerDispatcher: CoroutineDispatcher

/**
 * Downloads [GemmaThreeOneB] and proves it arrived intact.
 *
 * Three properties matter more than speed here:
 *
 *  - **Resumable.** The file is 769 MB. Losing it to a dropped connection or a backgrounded app
 *    is unacceptable, so bytes land in a `.part` file and a resume sends `Range: bytes=<n>-`.
 *  - **Verified.** A partial or substituted file must fail here, loudly, not later inside the
 *    inference engine where the error would be incomprehensible.
 *  - **Never half-installed.** The real filename only ever appears via [ModelFiles.promote],
 *    after the digest matched. There is no marker file to drift out of sync.
 */
class ModelInstaller(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem,
    directory: Path,
    private val spec: ModelSpec = GemmaThreeOneB,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val dispatcher: CoroutineDispatcher = installerDispatcher,
) {
    /**
     * Takes the filesystem and directory rather than a [ModelStorage] so the whole download and
     * verify path can be driven in `commonTest` against an in-memory filesystem, on every target.
     */
    constructor(
        httpClient: HttpClient,
        storage: ModelStorage,
        spec: ModelSpec = GemmaThreeOneB,
    ) : this(httpClient, storage.fileSystem, storage.modelsDirectory, spec)

    private val files = ModelFiles(fileSystem, directory, spec)

    private val _state = MutableStateFlow<InstallState>(InstallState.NotInstalled)
    val state: StateFlow<InstallState> = _state.asStateFlow()

    val modelPath: Path get() = files.target

    init {
        refresh()
    }

    /** Re-reads the install state from disk. Cheap; safe to call on every screen entry. */
    fun refresh() {
        _state.value = when {
            files.isInstalled() -> InstallState.Installed
            files.partialBytes() > 0L -> InstallState.Paused(
                DownloadProgress(files.partialBytes(), spec.sizeBytes, bytesPerSecond = 0.0),
            )
            else -> InstallState.NotInstalled
        }
    }

    /**
     * Runs the install to completion.
     *
     * Cancelling the calling coroutine is how you pause: the partial file is kept and the state
     * settles on [InstallState.Paused]. Call again to resume.
     */
    suspend fun install() {
        if (files.isInstalled()) {
            _state.value = InstallState.Installed
            return
        }

        try {
            withContext(dispatcher) {
                files.ensureDirectory()
                download()
                verifyAndPromote()
            }
        } catch (cancellation: CancellationException) {
            // A pause, an app exit, or a screen leaving composition. Keep the bytes.
            _state.value = InstallState.Paused(
                DownloadProgress(files.partialBytes(), spec.sizeBytes, bytesPerSecond = 0.0),
            )
            throw cancellation
        } catch (failure: Throwable) {
            _state.value = classify(failure)
        }
    }

    /** Throws away everything on disk and resets to [InstallState.NotInstalled]. */
    fun deleteInstall() {
        files.deleteAll()
        _state.value = InstallState.NotInstalled
    }

    // --- download ---------------------------------------------------------------------------

    private suspend fun download() {
        var startOffset = files.partialBytes()
        if (startOffset >= spec.sizeBytes) return // already complete; go straight to verifying

        // One retry budget: if the server ignores our Range header we start over from zero.
        var attemptedRestart = false

        while (true) {
            val resumed = startOffset > 0L
            val restartRequired = httpClient.prepareGet(spec.downloadUrl) {
                if (resumed) header(HttpHeaders.Range, "bytes=$startOffset-")
            }.execute { response ->
                when {
                    resumed && response.status == HttpStatusCode.PartialContent -> {
                        stream(response, startOffset)
                        false
                    }

                    resumed && response.status == HttpStatusCode.OK -> {
                        // The server ignored the range and is sending the whole file. Appending
                        // it would silently corrupt the download, so discard and start clean.
                        if (attemptedRestart) {
                            error("Server ignored the resume request twice (${response.status})")
                        }
                        true
                    }

                    !resumed && response.status == HttpStatusCode.OK -> {
                        stream(response, 0L)
                        false
                    }

                    else -> error("Unexpected response ${response.status} for ${spec.fileName}")
                }
            }

            if (!restartRequired) return

            attemptedRestart = true
            files.deletePartial()
            startOffset = 0L
        }
    }

    private suspend fun stream(response: HttpResponse, startOffset: Long) {
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(BUFFER_BYTES)
        var downloaded = startOffset

        // Smoothed rate. An instantaneous figure jitters wildly on mobile networks and makes the
        // ETA jump around, which reads as the app being confused rather than the network being.
        var bytesPerSecond = 0.0
        var windowStart = timeSource.markNow()
        var windowBytes = 0L
        var lastEmit = timeSource.markNow()

        emit(downloaded, bytesPerSecond)

        val sink = fileSystem.appendingSink(files.partial).buffer()
        try {
            while (true) {
                currentCoroutineContext().ensureActive()

                val read = channel.readAvailable(buffer)
                if (read == -1) break
                if (read == 0) continue

                sink.write(buffer, 0, read)
                downloaded += read
                windowBytes += read

                val windowElapsed = windowStart.elapsedNow()
                if (windowElapsed >= RATE_WINDOW) {
                    val sample = windowBytes / windowElapsed.inWholeMilliseconds.toDouble() * 1000
                    bytesPerSecond = if (bytesPerSecond == 0.0) {
                        sample
                    } else {
                        RATE_SMOOTHING * sample + (1 - RATE_SMOOTHING) * bytesPerSecond
                    }
                    windowStart = timeSource.markNow()
                    windowBytes = 0L
                }

                if (lastEmit.elapsedNow() >= EMIT_INTERVAL) {
                    sink.flush() // so a kill costs at most one interval of bytes
                    emit(downloaded, bytesPerSecond)
                    lastEmit = timeSource.markNow()
                }
            }
        } finally {
            // close() flushes first, so bytes read before a pause still reach the .part file.
            // That is the whole reason a pause is cheap.
            sink.close()
        }

        emit(downloaded, bytesPerSecond)
    }

    private fun emit(downloaded: Long, bytesPerSecond: Double) {
        _state.value = InstallState.Downloading(
            DownloadProgress(downloaded, spec.sizeBytes, bytesPerSecond),
        )
    }

    // --- verify -----------------------------------------------------------------------------

    private fun verifyAndPromote() {
        _state.value = InstallState.Verifying

        val onDisk = files.partialBytes()
        if (onDisk != spec.sizeBytes) {
            files.deletePartial()
            _state.value = InstallState.Failed(
                InstallFailure.ChecksumMismatch,
                "Downloaded ${formatBytes(onDisk)} but expected ${formatBytes(spec.sizeBytes)}. " +
                    "The download was incomplete — start it again.",
            )
            return
        }

        val digest = sha256Of(files.partial)
        if (!digest.equals(spec.sha256, ignoreCase = true)) {
            files.deletePartial()
            _state.value = InstallState.Failed(
                InstallFailure.ChecksumMismatch,
                "The downloaded weights did not match their published checksum, so they were " +
                    "discarded. Check your connection and try again.",
            )
            return
        }

        files.promote()
        _state.value = InstallState.Installed
    }

    private fun sha256Of(path: Path): String {
        val hashing = HashingSink.sha256(blackholeSink())
        val sink = hashing.buffer()
        try {
            val source = fileSystem.source(path)
            try {
                sink.writeAll(source)
            } finally {
                source.close()
            }
        } finally {
            // The digest is only complete once everything has been pushed through and closed.
            sink.close()
        }
        return hashing.hash.hex()
    }

    // --- failure classification -------------------------------------------------------------

    /**
     * Turns a thrown exception into something the user can act on.
     *
     * Kotlin Multiplatform common code cannot see JVM or Darwin exception types, so this leans on
     * the class name and message. That is coarse, and on purpose: the point is to choose between
     * "check your connection", "free up space" and "something went wrong", not to diagnose.
     */
    private fun classify(failure: Throwable): InstallState.Failed {
        val text = "${failure::class.simpleName} ${failure.message}".lowercase()
        return when {
            "enospc" in text || "no space" in text || "space left" in text ->
                InstallState.Failed(
                    InstallFailure.OutOfSpace,
                    "Ran out of storage while downloading. Free up about " +
                        "${formatBytes(spec.requiredDiskBytes)} and try again.",
                )

            "unexpected response" in text || "ignored the resume" in text ->
                InstallState.Failed(
                    InstallFailure.Server,
                    failure.message ?: "The model server responded unexpectedly.",
                )

            "unknownhost" in text || "connect" in text || "timeout" in text ||
                "socket" in text || "network" in text || "resolve" in text ->
                InstallState.Failed(
                    InstallFailure.Network,
                    "Lost the connection. Your progress is kept — resume when you are back online.",
                )

            else -> InstallState.Failed(
                InstallFailure.Unknown,
                failure.message ?: "The download stopped unexpectedly.",
            )
        }
    }

    private companion object {
        const val BUFFER_BYTES = 64 * 1024

        /** How often the UI is told about progress. */
        val EMIT_INTERVAL = 250.milliseconds

        /** How much traffic to average before updating the rate. */
        val RATE_WINDOW = 500.milliseconds

        /** Weight of the newest sample in the exponential moving average. */
        const val RATE_SMOOTHING = 0.3
    }
}
