package com.adll.de_general.feature.onboarding.domain

/**
 * The one local model this app runs.
 *
 * Everything here is pinned deliberately. The size and digest were read from the Hugging Face
 * API for this exact file, and the installer refuses anything that does not match — a truncated
 * or substituted 769 MB download should fail at the door, not three screens later inside the
 * inference engine.
 */
data class ModelSpec(
    val id: String,
    val displayName: String,
    val quantization: String,
    val parameterCount: String,
    val fileName: String,
    val downloadUrl: String,
    /** Exact byte length of the file. Used for progress, and checked before hashing. */
    val sizeBytes: Long,
    /** Lowercase hex SHA-256 of the file. */
    val sha256: String,
    /** Disk the install needs, including headroom for the partial file during download. */
    val requiredDiskBytes: Long,
    /** Rough working set once loaded: weights mapped in, plus KV cache. */
    val requiredRamBytes: Long,
)

/**
 * `gemma-3-1b-it-Q4_K_M.gguf`, served by `ggml-org` — the llama.cpp maintainers' own org.
 *
 * That mirror is used rather than `google/gemma-3-1b-it-qat-q4_0-gguf` because the Google repo is
 * gated behind a licence click, which a mobile app cannot complete on the user's behalf.
 */
val GemmaThreeOneB = ModelSpec(
    id = "gemma-3-1b-it-q4km",
    displayName = "Gemma 3 1B Instruct",
    quantization = "Q4_K_M",
    parameterCount = "1B",
    fileName = "gemma-3-1b-it-Q4_K_M.gguf",
    downloadUrl =
        "https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf",
    sizeBytes = 806_058_240L,
    sha256 = "8ccc5cd1f1b3602548715ae25a66ed73fd5dc68a210412eea643eb20eb75a135",
    // The file itself plus ~12%: during download the .part file exists alongside nothing else,
    // but the filesystem wants slack and the promote-on-success rename needs room to breathe.
    requiredDiskBytes = 903_000_000L,
    // 769 MiB of weights mapped in, plus KV cache and runtime overhead.
    requiredRamBytes = 1_200_000_000L,
)
