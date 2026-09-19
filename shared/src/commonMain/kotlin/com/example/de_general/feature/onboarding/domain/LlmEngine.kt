package com.example.de_general.feature.onboarding.domain

import okio.Path

/**
 * The seam where local inference will plug in.
 *
 * **There is no implementation yet, on purpose.** These two onboarding screens get the weights
 * onto the device and prove they are intact; running them needs llama.cpp built through the NDK
 * on Android and an XCFramework on iOS, which arrives with the journal screen.
 *
 * Declaring the interface now keeps the install flow from growing an accidental dependency on
 * whatever that backend turns out to be. The install screen's fourth milestone reads "Engine
 * ready" and is satisfied by a verified file; once there is a real engine it should become a
 * genuine load-and-warm-up, which is the honest version of the milestone.
 */
interface LlmEngine {
    /** Loads the model at [modelPath] into memory. Slow; call off the main thread. */
    suspend fun load(modelPath: Path)

    /** Frees the model and any KV cache. */
    suspend fun unload()

    val isLoaded: Boolean
}
