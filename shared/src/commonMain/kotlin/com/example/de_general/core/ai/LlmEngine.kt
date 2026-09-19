package com.example.de_general.core.ai

import kotlinx.coroutines.flow.Flow
import okio.Path

/**
 * The seam where local inference will plug in.
 *
 * **There is still no implementation, on purpose.** Onboarding gets the weights onto the device
 * and proves they are intact; running them needs llama.cpp built through the NDK on Android and an
 * XCFramework on iOS. Neither exists, and no inference dependency is in the build.
 *
 * Lives in `core` rather than under a feature because two features want it: onboarding installs
 * the weights, chat runs them, and the journal's reflection pass will too. A feature never imports
 * another feature, so the shared thing moves here.
 *
 * Declaring the interface now keeps the install flow from growing an accidental dependency on
 * whatever the backend turns out to be. The install screen's fourth milestone reads "Engine ready"
 * and is satisfied by a verified file; once there is a real engine it should become a genuine
 * load-and-warm-up, which is the honest version of the milestone.
 */
interface LlmEngine {

    val isLoaded: Boolean

    /** Loads the model at [modelPath] into memory. Slow; call off the main thread. */
    suspend fun load(modelPath: Path)

    /** Frees the model and any KV cache. */
    suspend fun unload()

    /**
     * Tokens for [prompt], as they are produced.
     *
     * Cold — collecting starts the generation, and cancelling the collection stops it. A stream
     * rather than a single `String` because a 1B model on a phone CPU is slow enough that waiting
     * for the whole answer would feel broken, and because it lets the caller stop early.
     *
     * The prompt arrives already templated; see `feature/chat/domain/ChatPrompt.kt`. An engine is
     * responsible for running text, not for knowing how a conversation is spelled.
     */
    fun generate(prompt: String): Flow<String>
}
