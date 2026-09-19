# DeGeneral

A private journal app. Everything runs on the user's device — entries never leave it, and the AI
reflection is a local Gemma 3 1B model, not an API call. That promise is the product; treat any
change that would weaken it as a bug, not a tradeoff.

Kotlin Multiplatform, Compose Multiplatform, Android + iOS.
Android is the working target; iOS compiles but has never been run.

## Hard rules — these are not judgement calls

### 1. Never run a build. Ever.

**No `./gradlew` invocation of any kind** — not `assembleDebug`, not a test task, not a compile
check, not a sync, not `help`. Builds happen on Adil's machine, run by Adil.

This is not "ask first" — it is "don't". Write the code, then tell him exactly what to run and what
you expect it to show. If the code is unverified, say so plainly; do not reach for Gradle to settle
it yourself.

Reading the repo is fine: `git status`, `git diff`, `git log`, `grep`, `cat`, and non-Gradle checks
like `python3 tools/check_theme_tokens.py` are all fair game.

### 2. Never commit. Ever.

**No `git commit`, `git add`, `git push`, branch creation, merges, rebases or tags.** Adil reads
every change himself before it enters the history.

Leave your work in the working tree, summarise what changed and why, and stop. Even when he says
"looks good" — that is approval of the code, not an instruction to commit. Wait to be asked in
those words.

When he does ask: **never add a `Co-Authored-By` trailer or any self-attribution.** The history
reads as his work.

## The house rules

### Theme
Read `docs/THEME.md` before touching anything visual. Short version:

- **No hex literals in a screen.** If a colour isn't in the theme, it belongs in the theme first.
- **No raw `dp` for padding.** Use `MindfulTheme.spacing`. The design system runs on an 8pt rhythm
  and ad-hoc values break it quietly.
- Cards use `spacing.cardPadding`; buttons and chips are pills (`MindfulShapes.full`).
- Use `safeDrawingPadding()`, never `safeContentPadding()` — the latter unions in `systemGestures`
  and silently adds ~40dp per side on gesture-navigation phones.
- The theme is ported from a Stitch design system. Stitch is upstream; `ui/theme/` is downstream.
  Fix colours in Stitch and re-port, don't hand-edit `Color.kt`.
  `python3 tools/check_theme_tokens.py` guards the palette against drift.

### Local AI
Read `docs/LOCAL_AI.md`. The model, its size and its SHA-256 are pinned in `ModelSpec.kt` and are
verified against the live source — change all three together or none of them.

### Navigation
Routes are `@Serializable` objects in `navigation/Destinations.kt`. **Screens never see a
`NavController`** — they take callbacks (`onContinue`, `onFinish`). Keep it that way; it is what
makes them previewable. Shared view models scope to the nav *graph* entry, not the destination.

### Tests
Write them; Adil runs them (`./gradlew :shared:testAndroidHostTest` — his command, not yours).

Logic that can be a pure function should be one, so it can be tested in `commonTest` on every
target — `checkCompatibility` and the start-destination mapping are the pattern to follow.

Watch out: `MockEngine` does not run on the coroutine test scheduler, so `advanceUntilIdle()` is not
a synchronisation point for it. Tests that interleave with it by virtual time hang intermittently.

## Honesty rules

This app makes privacy and hardware claims to the user, so the bar is higher than usual:

- **Never show a number the app did not measure.** The Stitch design promised NPU detection, a
  tokens-per-second estimate and a "92" compatibility score. None are obtainable, so none shipped.
  `docs/LOCAL_AI.md` has the full list — don't helpfully add them back.
- Don't claim the app encrypts anything. Android encrypts app-private storage at rest; we ride on
  that and the copy says "private sandbox".
- When something is unverified, say so. A green build is not a rendered screen, and a compiling iOS
  target is not a working one.

## Things I keep having to re-derive

- No AVD on this machine by default, and no Mac — so UI changes cannot be visually verified here.
  Hand them to Adil to run rather than implying they were checked.
- `material-icons-extended` stopped at Compose 1.7.3 and this project is on 1.12. Icons are
  hand-built from Material Symbols path data in `ui/icons/MindfulIcons.kt`. Split those path
  strings at fixed columns, never on whitespace — the spaces are significant to the path grammar.
