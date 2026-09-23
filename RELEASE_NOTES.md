# DeGeneral v1.0: first release

**A private journal with an AI companion that runs on your phone and never sends your writing anywhere.**

DeGeneral is a journaling app with a small language model built in: Google's **Gemma 3 1B**,
running locally on your phone's CPU. It can help you tidy what you've written, suggest a title,
and talk through what's been on your mind. There's no account, no server and no cloud sync. Your
entries and conversations stay in the app's private storage on your device.

The only time the app uses the network is to download the model once, during setup.

---

## ✨ What's in 1.0

### A setup that checks your phone and shows its working
- A **device check** before anything is downloaded: 64-bit processor, available memory, free
  storage, temperature and battery. Every figure it shows is measured on your phone.
- Heat and low battery give you a warning, not a block: they mean "not right now", not "never".
- **A model download you can pause and resume** (about 806 MB). It survives the app being closed.
- **Integrity-checked:** the downloaded model is verified against a pinned SHA-256 hash before it's
  ever loaded. A corrupt or tampered file is deleted, never used.

### Journal
- A clean archive of your entries, newest first, each with its date and mood.
- Filter by **All**, **Today**, **Yesterday**, or **pick any date** from a calendar.
- Delete entries you no longer want.

### A distraction-free composer
- Full-screen writing with a title, a live word count, and **tags**.
- Choose a **mood**: Calm, Joyful, Reflective, Grateful, Energized or Pensive.
- **Backdate** an entry to yesterday or any earlier day.
- **Refine grammar & spelling** with the on-device model. It fixes spelling, grammar and
  punctuation, and suggests a title if you haven't written one. You review the result first and
  can accept it, discard it, or undo it afterwards.
- Nothing is saved until you press **Save**, and the app asks before throwing away unsaved text.

### Chat: a companion that has read your journal
- Talk with the local model, which is given your **three most recent entries** as context so it
  can respond to what you've actually been writing about.
- Replies **stream in** as they're generated.
- **Reflect deeper** asks it to go further on its last answer.
- **Save insight** turns a reply you find useful into a new journal entry.
- **Clear chat** wipes the conversation (after asking). Your journal isn't touched.
- The model is loaded into memory only while Chat is open and released when you leave it.

### Appearance
- **Light**, **Dark**, or **follow the system**. Your choice is remembered from the first frame of
  the next launch.
- Two hand-tuned themes: *Mindful Scribe* (light) and *Nocturnal Sanctuary* (dark).

---

## 🔒 Privacy, stated plainly

- Your entries and chats are stored **only on your device**, in the app's private sandbox.
- The AI runs **on your phone**. Nothing you write is sent to an API.
- DeGeneral **doesn't add its own encryption**. It relies on Android's built-in encryption of
  app-private storage.
- The app **never shows a number it didn't measure**. You won't find a made-up "compatibility
  score" or speed estimate. Features that aren't built yet say so instead of pretending.

---

## 📱 Requirements

| | |
|---|---|
| **OS** | Android 8.0 (API 26) or newer |
| **Processor** | 64-bit (arm64 / x86_64) |
| **Memory** | about 1.2 GB available for the model |
| **Storage** | about 900 MB free for the model |
| **Network** | Needed once, to download the model (about 806 MB, Wi-Fi recommended) |

The app is built to run on a range of phones, but it has only been tested on a small number of
devices. Responses come faster on newer phones.

---

## ⚠️ Known limitations

- **Android only for now.** The iOS target compiles but hasn't been run on a device yet.
- Most of **Settings** is marked "not built yet". Only Appearance works in 1.0.
- Attachments in chat aren't supported yet.
- The model is small (1B parameters). It's good for reflection and tidying up text, but it can
  get things wrong, and it isn't a source of facts or advice.

---

## 🛠️ Under the hood

For anyone curious how it's built:

- **Kotlin Multiplatform + Compose Multiplatform**: one shared codebase for UI and logic,
  targeting Android and iOS.
- **On-device inference** with Llamatik (a Kotlin Multiplatform wrapper around llama.cpp),
  running a Q4_K_M-quantised GGUF of Gemma 3 1B Instruct.
- **Room (KMP)** for local storage, with versioned schemas and migrations.
- **Type-safe Compose Navigation** with `@Serializable` routes. Screens take callbacks rather
  than a `NavController`, which keeps every screen previewable.
- **Feature-first architecture**: `core/` holds shared building blocks, and each feature owns its
  own `domain`, `data` and `ui` layers. Features never import each other.
- **A design system ported from Google Stitch**, with a script that checks the Kotlin palette
  hasn't drifted from the source design.
- Material Symbols icons **hand-built from path data** (the extended icons library stops at
  Compose 1.7).
- Pure-function domain logic, unit-tested in `commonTest`.

---

## 🙏 Credits

- [Gemma 3](https://ai.google.dev/gemma) by Google DeepMind, used under the
  [Gemma Terms of Use](https://ai.google.dev/gemma/terms).
- [llama.cpp](https://github.com/ggml-org/llama.cpp) and Llamatik for on-device inference.
- JetBrains for Kotlin and Compose Multiplatform.

Built as a personal project by Adil. Feedback and issues are welcome on
[GitHub](https://github.com/adil3811A/DeGeneral/issues).
