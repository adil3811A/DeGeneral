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

## Not built yet

`LlmEngine` is an interface with **no implementation**. Running the model needs llama.cpp through
the NDK on Android and an XCFramework on iOS, and arrives with the journal screen. When it does,
the install screen's fourth milestone ("Engine ready") should become a real load-and-warm-up
instead of "the digest matched".

The iOS `actual`s for `DeviceProbe`, `ModelStorage` and `KeepScreenAwake` are written and compile
for `iosArm64`/`iosSimulatorArm64`, but have **never been run** — they were written on Linux with
no Mac available. Treat their runtime behaviour as unproven.

## Testing

```bash
./gradlew :shared:testAndroidHostTest
```

31 tests: the compatibility table, the formatting helpers, the onboarding entry point, and the
installer driven end to end against an in-memory filesystem and a scripted server (clean download,
resume, server-ignores-`Range`, corrupt body, truncated body, 404, pause, delete).

One trap if you add installer tests: **`MockEngine` does not run on the coroutine test
scheduler**, so `advanceUntilIdle()` is not a synchronisation point for it. Any test that tries to
interleave with the engine by virtual time is a race and will hang intermittently. Either let the
request run to completion, or use a handler that never responds at all.
