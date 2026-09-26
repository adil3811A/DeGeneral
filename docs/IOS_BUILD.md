# Building the iOS app from Linux, with no Apple developer account

TL;DR: push to `master`, GitHub Actions builds an **unsigned IPA** on a free
macOS runner, and you sideload it onto the phone, where **SideStore or AltStore
signs it with a free Apple ID at install time**. No Mac needed at any point, no
developer account, no certificate, no money.

- Workflow: [`.github/workflows/ios-unsigned-ipa.yml`](../.github/workflows/ios-unsigned-ipa.yml)
- Output: a `DeGeneral-iOS-unsigned-<run number>.ipa` build artifact, downloadable from the
  workflow run page.

## Why it works this way

**Why not build on this Linux machine?** Kotlin/Native *cross-compiles* the Apple targets from
any host — but it *links* them by driving Apple's own linker, which only exists on macOS. A
local Linux build of the iOS app is not possible; this is a toolchain fact, not a config
problem. The Mac has to come from somewhere, and GitHub gives public repos free macOS runner
minutes.

**Why unsigned?** With no Apple developer account there is no signing certificate, so nothing
built in CI can be signed — and it doesn't need to be. The sideload tools re-sign the app
themselves with a free personal Apple ID at *install* time. That is the supported path for
IPA distribution without the $99/year programme; it's the same mechanism AltStore uses for
its own app.

## Getting the IPA

1. Push to `master` (the workflow also runs on manual dispatch, from the Actions tab →
   "iOS unsigned IPA" → "Run workflow").
2. When the run finishes, download the **DeGeneral-iOS-unsigned-…** artifact from the run page
   and unzip it — the `.ipa` is inside.

## Getting it onto the phone (still no account)

A free Apple ID is required, but only as an identity for signing — no developer programme, no
payment. Any Apple ID works, including a spare one; the signing entitlements are tied to the
Apple ID, not to DeGeneral.

| Route | What you need | Notes |
|---|---|---|
| **SideStore** (recommended) | iPhone + the SideStore app + a free Apple ID | Installs and *re-signs on-device*, so the app can refresh itself without a computer. Pairing the phone once with a computer (or WireGuard-based pairing) is needed during setup. |
| **AltServer-Linux** ([NyaMisty fork](https://github.com/NyaMisty/AltServer-Linux)) | This Linux machine + USB cable + free Apple ID | Runs natively on Linux — no Mac or Windows anywhere. Installs the IPA over USB and re-signs every 7 days when the phone is plugged in. |
| **Sideloadly** (in a VM or on a friend's machine) | Windows or macOS + USB + free Apple ID | The fallback if the two above give trouble. |

### The free-account limits, stated plainly

- The signature **expires after 7 days**; the app must be re-signed (SideStore and AltStore do
  this for you, on-device or on plug-in). An expired app won't launch until refreshed.
- A free Apple ID can hold at most **3 sideloaded apps** at once.
- If the IPA is ever rebuilt with a different signing identity, the phone sees it as a
  different app — data doesn't carry over. Stick to one tool and one Apple ID.

### Linux-specific notes

- AltServer-Linux needs `usbmuxd` (and usually `libimobiledevice`) installed and running to
  see the phone.
- Its bundled `ldid` is old; there is an open issue about apps signed for very new iOS
  versions failing to launch (`EBADEXEC`). If that bites, re-sign on-device via SideStore
  instead.
- The device must be trusted and unlocked during the first install.

## iOS status — read this before celebrating a green build

The workflow proves the iOS app **compiles and packages**. It says nothing about it running:
**the iOS target has never been run on a device or a simulator.** First launch may well hit
issues the Android build never showed. Treat the first sideload as the actual test, and expect
to iterate.

Things worth checking on first run, in order:

1. Does it launch at all (a Kotlin/Native crash at startup is the classic first failure)?
2. Does onboarding's device check behave sensibly on iOS, or does it show Android-only
   assumptions?
3. Does the model download complete and pass the SHA-256 check on iOS?
4. Does the model actually load and infer (llamatik's iOS backend has never been exercised
   here)?

## Relationship to the rest of the docs

- `LOCAL_AI.md` — the pinned model. The iOS build downloads the same `gemma-3-1b-it-Q4_K_M.gguf`
  and verifies the same SHA-256; don't loosen either per-platform.
- `THEME.md` — unchanged; the theme is shared code.
- `CLAUDE.md` — the honesty rules above (unverified ≠ working) apply doubly to iOS.
