# DeGeneral

A private journal with an AI companion that runs entirely on your phone.

You write entries, and a small language model, Gemma 3 1B, helps you reflect on them. It can
tidy your writing, suggest a title, and talk things through with you. **Everything stays on the
device.** Entries are stored in a local database, and the model runs on the phone's own CPU. There
is no account, no server and no cloud sync. The only time the app touches the network is to
download the model once, during setup.

Built with Kotlin Multiplatform and Compose Multiplatform. **Android is the working target. iOS
compiles but has never been run.**

## What the app does

### 1. Setup: can this phone run the model?

The first two screens are onboarding.

- **Welcome** checks the device honestly: processor (64-bit only), memory, free storage, and heat
  and battery. Every figure shown is measured on the device. Heat and low battery produce a
  warning, not a block, because they mean "not right now" rather than "not ever".
- **Install** downloads the model (`gemma-3-1b-it-Q4_K_M.gguf`, about 806 MB, from Hugging Face).
  The download can be **paused and resumed**, survives the app being closed, and is
  **checked against a pinned SHA-256** before it's used. A corrupt or tampered file is deleted,
  never loaded.

Once the model is installed, the app opens straight on the journal from then on.

### 2. Journal: the archive

The **Journal** tab lists everything you've written, newest first, each with its real date and
the mood you picked. You can delete entries from here.

Filter by day with the pills at the top: **All**, **Today**, **Yesterday**, or **Pick a date**,
which opens a calendar. Days follow the phone's time zone, the same way each entry's date label
does.

### 3. New entry: the composer

The pencil button opens a full-screen composer with:

- a **title** and a large writing area, with a live word count
- a **mood**: Calm, Joyful, Reflective, Grateful, Energized or Pensive
- a **date**: today, yesterday, or any earlier date you pick, for backdating an entry
- **tags**
- **Refine grammar & spelling.** The local model corrects spelling, grammar and punctuation. If
  the title is empty, it also suggests one. You see the result before anything changes, and you can
  accept it, discard it, or undo afterwards. The app shows how long the pass actually took.

Nothing is saved until you press **Save**, and closing with unsaved text asks before discarding it.

### 4. Chat: a companion that has read your journal

The **Chat** tab is a conversation with the local model. It's given your **three most recent
journal entries** as context, so it can respond to what you've actually been writing about.

- Replies stream in as the model writes them.
- **Reflect deeper** asks it to go further on its last answer.
- **Save insight** turns a reply you find useful into a new journal entry.
- **Clear chat** (the trash icon at the top) deletes the whole conversation after asking first.
  Your journal entries, including saved insights, are not touched.
- The conversation is kept on the device, like your entries.

The model's weights (about 770 MB in memory) are only loaded while the Chat tab is open, and are
released when you leave it.

### 5. Settings

- **Appearance:** choose **System**, **Light** or **Dark**. System follows the phone's dark mode.
  The choice is remembered and applied from the first frame on the next launch.
- Everything else here is honestly marked as not built yet.

## What the app does *not* claim

This app makes privacy and hardware claims, so it holds itself to a high bar:

- It **doesn't encrypt anything itself**. Entries live in the app's private sandbox, which Android
  encrypts at rest, and the app says "stays on this device", not "encrypted".
- It **never shows a number it didn't measure**. The original design promised NPU detection, a
  tokens-per-second estimate and a compatibility score. None of those can be obtained honestly,
  so none shipped. See [docs/LOCAL_AI.md](./docs/LOCAL_AI.md).
- Features that don't exist yet (attachments in chat, most of Settings) say so instead of
  pretending.

## Project layout

Feature-first. `core/` holds what every feature uses; each `feature/<name>/` holds its own
`domain`, `data` and `ui`.

```
shared/src/commonMain/kotlin/com/adll/de_general/
├── core/        ai (the local engine), data (Room database, preferences), ui (theme, components, icons)
├── feature/
│   ├── onboarding/   device checks and the model download
│   ├── journal/      the archive and the composer
│   ├── chat/         the companion
│   └── settings/     appearance
├── navigation/  routes and the nav graphs
└── di/          AppContainer, built by hand
androidApp/      Android entry point
iosApp/          iOS entry point (Xcode)
```

`CLAUDE.md` has the house rules: `core` never imports a feature, and a feature never imports
another feature.

## Docs

| Doc | What it covers |
|---|---|
| [docs/THEME.md](./docs/THEME.md) | The design system (Mindful Scribe light, Nocturnal Sanctuary dark), ported from Google Stitch. Read before touching anything visual. |
| [docs/LOCAL_AI.md](./docs/LOCAL_AI.md) | The pinned model, the resumable and verified install, and what the screens refuse to claim. |
| [docs/CREATE_JOURNAL.md](./docs/CREATE_JOURNAL.md) | The design of the composer. |
| [docs/DARK_THEME.md](./docs/DARK_THEME.md) | How dark mode and the appearance setting were built. |

## Building and testing

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open [/iosApp](./iosApp) in Xcode and run it from there (untested so far).
- Tests: `./gradlew :shared:testAndroidHostTest` (Android host) or
  `./gradlew :shared:iosSimulatorArm64Test` (iOS simulator)
- Check that the colour palettes still match Stitch: `python3 tools/check_theme_tokens.py`
