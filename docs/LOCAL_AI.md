# The local AI engine

How the model gets onto the device, and what the onboarding screens promise about it.

Code lives in `shared/src/commonMain/kotlin/com/example/de_general/feature/onboarding/domain/`
(engine) and `.../feature/onboarding/ui/` (the two screens). The strings and figures those
screens show are derived in `InstallCopy.kt` and `WelcomeCopy.kt` beside them — pure functions
with no Compose import, so every milestone label and progress line is covered by `commonTest`
rather than only by looking at the screen.

## The model

One model, pinned in `ModelSpec.kt`. There is no picker.

| | |
|---|---|
| File | `gemma-3-1b-it-Q4_K_M.gguf` |
| Source | `huggingface.co/ggml-org/gemma-3-1b-it-GGUF` |
| Size | `806,058,240` bytes |
| SHA-256 | `8ccc5cd1…eb75a135` |

`ggml-org` is the llama.cpp maintainers' own org and is **not gated**. Do not switch to
`google/gemma-3-1b-it-qat-q4_0-gguf`: it sits behind a licence click that an app cannot complete
for the user.

Changing the model means changing the size and digest in the same commit. Verify against the live
source before you do:

```bash
curl -s "https://huggingface.co/api/models/ggml-org/gemma-3-1b-it-GGUF?blobs=true" \
  | python3 -c "import sys,json; print([s['lfs'] for s in json.load(sys.stdin)['siblings'] if 'Q4_K_M' in s['rfilename']])"
```

## Install pipeline

`ModelInstaller` owns it, as a `StateFlow<InstallState>`:

```
NotInstalled → Downloading → Verifying → Installed
                    ↕                ↘
                 Paused            Failed
```

Four properties are load-bearing. Do not regress them:

1. **Resumable.** Bytes land in `<name>.gguf.part`; a resume sends `Range: bytes=<n>-`.
2. **206 or restart.** If a resume gets `200`, the server ignored the range and is sending the
   whole file. Appending that would silently corrupt the download, so the partial is discarded and
   the transfer restarts once. Twice in a row is a hard failure.
3. **Verified before promotion.** The real filename only ever appears via an atomic rename after
   the SHA-256 matched. A bad file is deleted, never left behind to resume.
4. **Install state is the filesystem.** No marker file, no database row — `ModelFiles.isInstalled()`
   is "the target exists at the expected size". Nothing can drift out of sync with itself.

Pausing is literally cancelling the coroutine. `ModelInstaller` catches the cancellation, parks on
`Paused` and keeps the partial; closing the buffered sink flushes what was already read. There is
deliberately no separate pause flag that could disagree with the wire.

## Compatibility checks

`Compatibility.kt` is a pure function over a `DeviceSnapshot` — no Compose, no platform types — so
every rule is testable. Four checks: processor, memory, storage, thermal & battery.

Heat and low battery **warn but never block**; they are a "not right now", not a "not ever".

Two fields are nullable because the answer is genuinely unavailable, and a guess would be worse:

- `availableRamBytes` — iOS exposes total physical memory but no free figure, so the rule falls
  back to assuming half of total is usable, and the UI says "8.0 GB total" rather than inventing a
  free number.
- `thermal` — Android only reports it from API 29.

## What the screens deliberately do not claim

The Stitch design promised hardware facts that no mobile API can supply. These were replaced
rather than faked, and should stay replaced:

| Design promised | Why it is not there |
|---|---|
| "Neural Accelerator (NPU): Supported" | Android exposes no NPU-detection API, and llama.cpp runs on the CPU anyway. Shows cores + ABI instead. |
| "Estimated Speed: 24 tok/s" | Unknowable until the model has actually run. Bring it back as a measured figure. |
| Compatibility score "92" | Invented. The ring shows how many real checks passed. |
| "Encrypted weights" | The app encrypts nothing. Android 10+ encrypts app-private storage at rest; the copy says "private sandbox". |
| Stage 4 "Quantization & cache compilation" | Not a step that exists for a GGUF file. |

## The engine

`core/ai/LlmEngine.kt` is the seam — in `core`, not under a feature, because onboarding installs
the weights and chat runs them, and a feature never imports another feature. One implementation,
`LlamatikEngine`, in `commonMain`.

### Why llama.cpp, and not LiteRT-LM

Checked against the live Hugging Face API:

| Repo | Format | Gated? |
|---|---|---|
| `litert-community/Gemma3-1B-IT` | `.task` / `.litertlm` | **Yes** — unauthenticated GET returns `401 GatedRepo` |
| `ggml-org/gemma-3-1b-it-GGUF` | `.gguf` | **No** — `"gated": false` |

`ModelInstaller` does a plain unauthenticated `GET` with `Range` resume. It cannot complete a
licence click, and an app that promises nothing leaves the device has no business shipping a
Hugging Face token. Every Gemma build in LiteRT-LM format is gated, so **the ungated artefact
decides the runtime, not the other way round** — the same reasoning that already rules out
`google/gemma-3-1b-it-qat-q4_0-gguf`.

Google's runtime is the better engineered one. It is not the one we can download a model for.

### Llamatik

`com.llamatik:library` — a KMP wrapper bundling llama.cpp, in `commonMain` because it publishes
`android`, `iosArm64` and `iosSimulatorArm64`. On iOS llama.cpp is statically linked into the
cinterop klib, so there is no Xcode, CMake or XCFramework step. It reads GGUF, so the pinned model,
its digest and the whole installer are untouched.

Three things to know:

- Its AAR declares `minSdkVersion 26`, which is what pins `android-minSdk` in the version catalog.
- Its klibs are built with Kotlin 2.2.21 while this project is on 2.4.20. That skew is the first
  thing to suspect if the iOS or Android compile breaks after a Kotlin upgrade.
- It is a single-maintainer dependency carrying the app's most important capability. That is why
  `LlmEngine` stays an interface: swapping the runtime is one file.

`LlamaBridge` also offers `applyChatTemplate(...)` using the template embedded in the GGUF. We do
not use it — `feature/chat/domain/ChatPrompt.kt` is the tested source of truth and also carries the
journal context. If replies ever come back malformed, compare the two:
`LlamaBridge.getModelChatTemplate()` returns Gemma's own.

### Lifetime

The weights are ~770 MB, so `AppContainer` is application-scoped (`DeGeneralApplication` on
Android, a file-scoped `val` on iOS). Before that the container was rebuilt in
`MainActivity.onCreate`, which would have meant a second copy of the model on every rotation.

The Chat screen loads on entry and unloads on exit, through `ChatViewModel.loadEngine()` /
`unloadEngine()`. Unloading runs on `viewModelScope` — the view model is scoped to the main nav
graph and outlives the screen, whereas a scope remembered in the composable is cancelled on the way
out and would abandon the unload half-done.

The install screen's fourth milestone ("Engine ready") is still satisfied by a verified file rather
than a real load-and-warm-up. That is still the honest version of that milestone to build.

## What the chat screen deliberately does not claim

Same rule as the onboarding screens, applied to the Stitch screen "Companion Chat - Local AI":

| Design promised | Why it is not there |
|---|---|
| "Gemma-2B Local • 0ms Latency" | Wrong model, and a latency nothing measured. The chip reads `Gemma 3 1B · Q4_K_M` from `ModelSpec`, plus the engine's own state. |
| A typing / "Synthesizing" indicator | Replaced by the real thing: tokens appear as the model produces them. |
| "Add to Morning Intention", "Explore Scripts", "Privacy Vault" | No such features. |
| Voice dictation | No speech-to-text anywhere in the app. |
| "Encrypted" on message rows | The app encrypts nothing of its own. Same rule as the journal. |
| The "Suggested Journal Prompt" card | Needs structured output from a model that has never run. |

What *is* real: the offline badge, the context chip (it counts entries that exist — `0` says "no
entries yet"), and both reply actions — "Reflect deeper" sends a genuine follow-up turn, "Save
insight" writes a real journal entry.

### Two numbers that are now allowed on screen

This file spent a long time with no speed figure at all, because an estimated one is a lie. Both of
these are **measured**, which is the condition that entry always set:

- **tokens/sec under each reply** — counted across the actual generation and stored on the row
  (`tokens_per_second`, `generation_millis`, database version 3). Null on older replies and on the
  person's own turns, and absent from the UI rather than guessed. One honest caveat: the engine
  emits *deltas*, usually but not necessarily one token each, so it is chunks/sec wearing a
  tokens/sec label.
- **model load time** on the status chip — timed across `LlmEngine.load`.

The prompt is assembled even though nothing consumes it. `feature/chat/domain/ChatPrompt.kt` is a
pure function over Gemma's chat template (`<start_of_turn>user` / `<start_of_turn>model`, with the
framing folded into the first user turn because Gemma has no system role), and `ChatPromptTest`
pins it. It is the one part of the pipeline that can be proved correct before an engine exists.

## Chat storage

`chat_messages`, added in database **version 2** with an explicit `Migration(1, 2)` in
`core/data/Migrations.kt`, and the two measurement columns in **version 3**. `createDatabase()` has no destructive fallback on purpose: a missing
migration should fail loudly in development rather than quietly delete someone's journal, because
entries never leave the device and there is no copy to restore from.

The iOS `actual`s for `DeviceProbe`, `ModelStorage` and `KeepScreenAwake` are written and compile
for `iosArm64`/`iosSimulatorArm64`, but have **never been run** — they were written on Linux with
no Mac available. Treat their runtime behaviour as unproven.

## Testing

```bash
./gradlew :shared:testAndroidHostTest
```

The compatibility table, the formatting helpers, the onboarding entry point, the Gemma prompt
template, and the installer driven end to end against an in-memory filesystem and a scripted
server (clean download, resume, server-ignores-`Range`, corrupt body, truncated body, 404,
pause, delete).

One trap if you add installer tests: **`MockEngine` does not run on the coroutine test
scheduler**, so `advanceUntilIdle()` is not a synchronisation point for it. Any test that tries to
interleave with the engine by virtual time is a race and will hang intermittently. Either let the
request run to completion, or use a handler that never responds at all.
