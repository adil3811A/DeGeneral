package com.adll.de_general.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import okio.Path

/**
 * The local model, as the rest of the app sees it.
 *
 * Lives in `core` rather than under a feature because two features want it: onboarding installs the
 * weights, chat runs them, and the journal's reflection pass will too. A feature never imports
 * another feature, so the shared thing lives here.
 *
 * Kept as an interface with one implementation ([LlamatikEngine]) on purpose. The runtime is a
 * third-party dependency carrying a large native payload; if it ever has to be swapped for a
 * hand-built llama.cpp module, this seam is the only thing the rest of the app is written against.
 */
interface LlmEngine {

    /** Loading, ready, or broken — see [EngineState]. The chat screen renders this directly. */
    val state: StateFlow<EngineState>

    /**
     * Loads the model at [modelPath] into memory.
     *
     * Slow — hundreds of megabytes off disk — and safe to call when already loaded, which is a
     * no-op. Failures land in [state] as [EngineState.Failed] rather than throwing, because the
     * caller is a `DisposableEffect` with nowhere to put an exception.
     */
    suspend fun load(modelPath: Path)

    /** Frees the model and its KV cache. Safe to call when nothing is loaded. */
    suspend fun unload()

    /**
     * Tokens for [prompt], as they are produced.
     *
     * Cold — collecting starts the generation, and cancelling the collection stops it mid-token
     * rather than running to completion in the background. A stream rather than one `String`
     * because a 1B model on a phone CPU is slow enough that waiting for a whole answer would feel
     * broken.
     *
     * The prompt arrives already templated; see `feature/chat/domain/ChatPrompt.kt`. An engine runs
     * text, it does not know how a conversation is spelled.
     */
    fun generate(prompt: String): Flow<String>
}
