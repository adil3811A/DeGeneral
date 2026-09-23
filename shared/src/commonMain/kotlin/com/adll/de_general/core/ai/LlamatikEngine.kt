package com.adll.de_general.core.ai

import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import com.llamatik.library.platform.LlamaSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path
import kotlin.time.TimeSource

/**
 * [LlmEngine] on top of Llamatik, which bundles llama.cpp for every target this module has.
 *
 * Ordinary common code, not an `expect`/`actual`: Llamatik's `LlamaBridge` is itself an
 * `expect object` with actuals for `android`, `iosArm64` and `iosSimulatorArm64`, so one
 * implementation covers both platforms. On iOS llama.cpp is statically linked into the cinterop
 * klib, which is what lets this work with no Xcode or CMake step.
 *
 * **Unverified on iOS**, like the rest of the iOS side — see `DeviceProbe.ios.kt`.
 *
 * Two things about Llamatik's API drive the shape below:
 *  - `LlamaSession.stream` is **blocking**, so it runs on [dispatcher], never the caller's thread.
 *  - `reset`/`close` must not be called while `stream` is running, so [lock] serialises load,
 *    generate and unload against each other. One conversation, one session, one KV cache.
 *
 * [generate] holds [lock] for the whole stream, which is what makes that second claim true rather
 * than aspirational: an [unload] arriving mid-generation now *waits* instead of closing the session
 * out from under a running native call. The wait is bounded by [MAX_TOKENS] and happens on the
 * caller's scope, off the main thread. Freeing memory a few seconds late is strictly better than
 * freeing it underneath llama.cpp.
 */
class LlamatikEngine(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : LlmEngine {

    private val _state = MutableStateFlow<EngineState>(EngineState.Idle)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val lock = Mutex()

    /**
     * Read and written only under [lock] — by [load], [unload] and [generate] alike — so the mutex
     * supplies the ordering a `@Volatile` used to be standing in for.
     */
    private var session: LlamaSession? = null

    override suspend fun load(modelPath: Path) {
        lock.withLock {
            if (session != null) return
            _state.value = EngineState.Loading

            val outcome = withContext(dispatcher) {
                runCatching {
                    val started = timeSource.markNow()
                    // Llamatik takes a plain filesystem path. The installer already guarantees the
                    // file exists at this path and that its SHA-256 matched before it got the name.
                    check(LlamaBridge.initGenerateModel(modelPath.toString())) {
                        "The model file could not be opened."
                    }
                    LlamaBridge.updateGenerateParams(
                        temperature = TEMPERATURE,
                        maxTokens = MAX_TOKENS,
                        topP = TOP_P,
                        topK = TOP_K,
                        repeatPenalty = REPEAT_PENALTY,
                        contextLength = CONTEXT_LENGTH,
                        numThreads = THREADS,
                        useMmap = true,
                        flashAttention = false,
                        batchSize = BATCH_SIZE,
                    )
                    val created = checkNotNull(LlamaBridge.createSession("chat")) {
                        "The model loaded but no inference session could be created."
                    }
                    created to started.elapsedNow().inWholeMilliseconds
                }
            }

            outcome
                .onSuccess { (created, millis) ->
                    session = created
                    _state.value = EngineState.Ready(millis)
                }
                .onFailure { failure ->
                    runCatching { LlamaBridge.shutdown() }
                    session = null
                    _state.value = EngineState.Failed(
                        failure.message ?: "The model could not be loaded.",
                    )
                }
        }
    }

    override suspend fun unload() {
        lock.withLock {
            val open = session ?: return
            session = null
            withContext(dispatcher) {
                runCatching {
                    open.close()
                    LlamaBridge.shutdown()
                }
            }
            _state.value = EngineState.Idle
        }
    }

    /**
     * One generation at a time, and never concurrently with [load] or [unload].
     *
     * The lock is taken here rather than inside [streamOnce] because it has to span the *whole*
     * stream: `LlamaSession.stream` blocks until the answer is finished, and releasing early would
     * put us back where we started. `flow { }` is what lets a `suspend` lock acquisition wrap a
     * cold flow — `callbackFlow`'s block cannot suspend before its first send.
     */
    override fun generate(prompt: String): Flow<String> = flow {
        lock.withLock { emitAll(streamOnce(prompt)) }
    }.flowOn(dispatcher)

    private fun streamOnce(prompt: String): Flow<String> = callbackFlow {
        val open = session
        if (open == null) {
            close(IllegalStateException("The model is not loaded."))
            return@callbackFlow
        }

        // Each turn starts from a clean KV cache: the whole conversation is already in `prompt`,
        // so replaying it on top of retained state would double every earlier turn.
        open.reset()

        open.stream(
            prompt,
            object : GenStream {
                override fun onDelta(text: String) {
                    trySend(text)
                }

                override fun onComplete() {
                    close()
                }

                override fun onError(message: String) {
                    close(IllegalStateException(message))
                }
            },
        )

        // `stream` blocks until it is done, so by here generation has finished or thrown. This
        // only fires when the collector walked away first — cancelling mid-answer.
        awaitClose { open.cancel() }
    }
        // Unbounded, and not an arbitrary choice: `onDelta` arrives on the blocking native
        // thread where there is no way to suspend, so it can only `trySend`. At the default
        // capacity a slow frame would drop tokens and silently corrupt the reply. An answer is
        // capped at MAX_TOKENS, so "unbounded" is bounded in practice.
        //
        // It stays on *this* flow rather than moving out to `generate`: `buffer` fuses into the
        // `callbackFlow`'s own channel, and only here does it actually widen the channel that
        // `trySend` writes into.
        .buffer(Channel.UNLIMITED)
}

/**
 * Sampling and context settings.
 *
 * Chosen for a 1B model doing short reflective replies on a phone CPU, not tuned against measured
 * output — if replies read as repetitive or rambling, this is the place to change, and the tuning
 * belongs in one named constant rather than scattered at a call site.
 */
private const val TEMPERATURE = 0.7f
private const val TOP_P = 0.95f
private const val TOP_K = 40
private const val REPEAT_PENALTY = 1.1f

/** Long enough for a thoughtful paragraph, short enough that a slow phone still finishes. */
private const val MAX_TOKENS = 512

/**
 * The KV window. Gemma 3 handles far more, but every token of context costs memory on a device
 * that also has to hold the weights, and `ChatPrompt.kt` replays the whole transcript.
 */
private const val CONTEXT_LENGTH = 4096

/** Leave cores for the UI. Generation must not make the app feel frozen. */
private const val THREADS = 4

private const val BATCH_SIZE = 256
