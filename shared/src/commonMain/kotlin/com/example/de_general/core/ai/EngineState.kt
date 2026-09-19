package com.example.de_general.core.ai

/**
 * What the local model is doing right now.
 *
 * The chat screen shows this directly, so every value here has to be something the engine actually
 * knows rather than something the UI assumes. That is why [Ready] carries a *measured* load time
 * and not an estimate — `docs/LOCAL_AI.md` is explicit that a number the app did not measure has no
 * business on screen.
 */
sealed interface EngineState {

    /** No model loaded. The weights may be on disk; nothing has opened them. */
    data object Idle : EngineState

    /** Reading ~770 MB off disk. Takes real seconds, which is why it has a state of its own. */
    data object Loading : EngineState

    /**
     * Loaded and ready to generate.
     *
     * @param loadMillis how long [LlmEngine.load] actually took, timed across the call.
     */
    data class Ready(val loadMillis: Long) : EngineState

    /**
     * The model could not be loaded, or generation failed.
     *
     * @param message shown to the user as-is, so it has to read like a sentence.
     */
    data class Failed(val message: String) : EngineState
}
