# Create Journal — a full-screen composer behind the pencil FAB

A plan, not a record. Nothing in here is built yet.

## Why

Today the pencil FAB does not navigate anywhere. `MainNavBar` switches to the Journal tab and
bumps a counter (`JournalUiState.composeRequest`) that `JournalScreen` answers by calling
`FocusRequester.requestFocus()` on an inline `OutlinedTextField`. Writing an entry means a
three-line box wedged above the archive.

The Stitch design this app is ported from has a screen for it — **"Create Journal"**, in project
`13240507270798585972` ("AI Daily Journal UI"), the same project `docs/THEME.md` names as
upstream. It is a full page: a date anchor you can backdate, a mood selector, a title, a large
writing canvas, tags, and a local-AI polish action.

This plan builds that page. The Journal tab then becomes the read-only archive the Stitch "Journal
Archive" screen shows.

**Scope:** title, mood chips, date anchor with backdating, tags, and a *real* AI polish pass.
Photos, offline voice dictation and the AI-prompt-suggestion card do not ship. Drafts live in
memory in a graph-scoped view model — no autosave.

## Copy that cannot ship as designed

`CLAUDE.md` and `docs/LOCAL_AI.md` already settle these.

| Design says | Ships as |
|---|---|
| "On-Device Encrypted", "Offline encrypted" | **"Stays on this device"** — the app encrypts nothing |
| "Auto-saved 0s ago" + pulsing dot | deleted — nothing is saved until Save is pressed |
| "Auto-synced" | deleted — there is no server and nothing syncs |
| "Current Date" tag | deleted — a lie the moment anyone backdates |
| "1 min read" | deleted — 200 wpm is arithmetic about a reader nobody timed. "72 words" stays: we counted them |
| "Refine grammar & clarity (Local 0ms)" | **"Refine grammar & spelling"**, plus a *measured* "Refined in N ms" once it has run |
| "Neural Core" | **`Gemma 3 1B · Q4_K_M`**, derived from `GemmaThreeOneB` as the chat chip already is |
| "Create Journal" title | **"New entry"** — matches the FAB label |
| "Thought preserved" toast | deleted — no toast exists in this app, and the screen closes on save |

Two stale claims already in the tree get fixed in the same pass: `JournalScreen`'s KDoc ("needs a
date library this module does not depend on yet") stops being true, and `MoodColors.kt`'s
`privacyShield` KDoc still says it "carries the 'On-Device Encrypted' badge".

---

## 1. Prerequisite: fix the engine mutex

`core/ai/LlamatikEngine.kt:36` claims `lock` "serialises load, generate and unload against each
other". **It does not.** `load` and `unload` take it; `generate` (line 114) reads the `@Volatile
session` and calls `stream` outside it — line 49 admits this. So `unload()` can close a session
mid-`stream`, which the file's own KDoc says must never happen.

Latent today with one caller. Adding a second makes it reachable. Fix it first:

```kotlin
override fun generate(prompt: String): Flow<String> = flow {
    lock.withLock { emitAll(streamOnce(prompt)) }
}.buffer(Channel.UNLIMITED).flowOn(dispatcher)

private fun streamOnce(prompt: String): Flow<String> = callbackFlow { /* current body */ }
```

Consequence to document: an `unload` arriving during generation now *waits* (bounded by
`MAX_TOKENS = 512`, on `viewModelScope`, off the main thread). Strictly better than freeing memory
under a running native call. Not provable without a two-caller test against a fake `LlmEngine`.

---

## 2. Dependency: `kotlinx-datetime` 0.8.0

Latest stable; requires Kotlin ≥ 2.3.21 and this project is on 2.4.20.

```toml
kotlinx-datetime = "0.8.0"                                    # [versions]
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
```
plus `implementation(libs.kotlinx.datetime)` in `commonMain.dependencies`.

Two traps: `kotlin.time.Instant`/`Clock` are stable in Kotlin 2.3+, so **no `@OptIn`**; and 0.7.0
renamed `dayOfMonth`→`day`, `monthNumber`→`month` and dropped `kotlinx.datetime.Instant`, so any
snippet older than mid-2025 will not compile. Do not use the `0.8.0-0.6.x-compat` artifact.

Forces a Gradle sync and rebuilds both iOS klibs.

---

## 3. Schema version 4

Three nullable `TEXT` columns on `journal_entries`, appended after `timestamp`, each with a Kotlin
default — which is what keeps every existing construction site and `write(...)` caller compiling.

`ALTER TABLE ... ADD COLUMN` on a `NOT NULL` column needs a `DEFAULT`, and any default here is a
value the user did not choose. Nullable means no backfill and no invented data.

**`mood` is its own column, never a reuse of `feeling_color`.** `saveAiResult` does
`UPDATE ... SET feeling_color = :feelingColor`, so a later AI pass would silently overwrite a mood
the user picked by hand. `feeling_color` stays documented as the model's reading; the composer
does not write it.

Files: `feature/journal/domain/JournalEntry.kt` (+3 fields), `core/data/DeGeneralDatabase.kt`
(`version = 4`), `core/data/Migrations.kt`:

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `journal_entries` ADD COLUMN `title` TEXT")
        connection.execSQL("ALTER TABLE `journal_entries` ADD COLUMN `mood` TEXT")
        connection.execSQL("ALTER TABLE `journal_entries` ADD COLUMN `tags` TEXT")
    }
}
val DeGeneralMigrations: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
```

> **The one mistake here that is not a compile error** is forgetting the `DeGeneralMigrations`
> line. With no destructive fallback that crashes a device upgrading from v3 with real entries —
> and never a clean install, so no test catches it.

---

## 4. Three pure domain files

Compose-free, so `commonTest` covers them on every target — the `checkCompatibility` /
`InstallCopy.kt` pattern.

### `domain/JournalMood.kt`

The design's six: Calm, Joyful, Reflective, Grateful, Energized, Pensive. Each carries a `label`
and an `accent: Mood` mapping onto one of the theme's **four** `MoodAccent`s (Grateful→Happy,
Pensive→Reflective).

Six chips on four accents means two pairs share a selected colour. That is the accepted cost of
staying faithful to the design; the label disambiguates and only one chip is ever selected.
Adding accents is forbidden here — `docs/THEME.md` is explicit that Stitch is upstream, and
`tools/check_theme_tokens.py` only reads `Color.kt`, so it would not catch the drift. If six real
accents are wanted later, that is a Stitch change plus a re-port and this enum collapses to
`accent: Mood` one-to-one.

Stored as the constant's **name** in a `String?`, not a Room enum: Room's enum support throws on a
value the build does not know; `journalMoodOrNull(stored)` returns null instead. A journal should
lose a chip, not refuse to open.

Emoji are dropped — no screen here uses one, and neither Newsreader nor Plus Jakarta Sans
guarantees coverage. The design system's 6px chip dot is the intended affordance and is already a
token.

### `domain/JournalTags.kt`

One newline-joined `TEXT` column, not a junction table. A junction table forces `@Relation` +
`@Transaction` on every read, changing `observeEntries()`'s return type and rippling into
`JournalUiState`, both preview fixtures, `FakeJournalDao` and every DAO query — to serve
`WHERE tag = ?` queries **no UI issues**. Keeping serialise/deserialise as tested pure functions
is what makes the later migration cheap; scattered inline `split(",")` is the real corner.

Newline rather than comma because `normalizeTag` strips *all* whitespace, so the separator cannot
survive into a tag **by construction** rather than by a rule someone must remember.

```
normalizeTag(raw): String?   // strip whitespace, drop one leading '#', cap 32; "" -> null
normalizeTags(list)          // de-dup case-insensitively keeping first spelling, preserve order, cap 12
encodeTags(list): String?    // null for empty, never ""
decodeTags(stored)           // total: null/""/garbage -> emptyList; re-normalises on read
splitTagInput / addTags
```

### `domain/JournalDates.kt`

`DateAnchor` (`Today` | `Yesterday` | `On(LocalDate)`), `DayBucket`, and formatters taking an
explicit `TimeZone` **and** an explicit "now" — so tests pin "yesterday" without waiting for
midnight and pin a format without depending on the machine's locale.

Formats use explicit English names (`DayOfWeekNames.ENGLISH_FULL`,
`MonthNames.ENGLISH_ABBREVIATED`), not a locale-aware formatter: the design specifies a fixed
string and a locale-shifting formatter would silently stop matching it.

**Backdating stores the chosen date at the time of day Save was pressed — never midnight.**
`observeAll()` is `ORDER BY timestamp DESC` with no tiebreak, so a fixed 00:00 leaves two same-day
backdated entries in an order SQLite may vary between reads; the screen prints the time, and 00:00
renders "12:00 AM", which nobody chose; and `resolveAnchor(On(today))` then equals `now` exactly,
so the picker and the Today chip agree. `canAnchorTo` rejects future dates.

> The Stitch mock's own sample, "Thursday, Oct 24, 2026", is a **Saturday** — verified. Derive the
> weekday, never copy that string. Test fixture `1_792_716_300_000L` is Thursday 22 Oct 2026,
> 08:45 PM in `America/New_York` — also verified.

---

## 5. Repository

`JournalRepository.write` gains five defaulted parameters; everything else is unchanged.

```kotlin
suspend fun write(
    rawText: String,
    title: String? = null,
    mood: JournalMood? = null,
    tags: List<String> = emptyList(),
    fixedText: String? = null,
    timestamp: Long? = null,
): Long
```

Normalisation happens once here: body trimmed; blank title → `null` (never `""`); tags through
`encodeTags`; `timestamp ?: now()`. The injected `now` stays — it stamps every non-backdated
entry. A backdated entry arrives as an already-resolved epoch-milli from `resolveAnchor`; the
repository does not know what a time zone is and must not learn.

`fixedText` on `write` is what lets Refine work **before** the row exists (§6).

`AppContainer.kt:59` (`saveInsight`) is unchanged — every default is right for a chat insight.
`AppContainer.now` needs to go from `private val` to `val` so the composer can stamp with the same
clock.

Recommended: add `, id DESC` to `observeAll`/`getAll` and `, id ASC` to `getUnprocessed`.
Backdating makes timestamp collisions plausible for the first time. Queries are not in the
exported schema, so no version bump — but `FakeJournalDao` must match or a test asserts an order
Room does not produce.

---

## 6. The AI polish pass

### Lifetime: Chat owns the engine, the composer borrows

`navigate(CreateJournal)` disposes the Chat destination, so Chat's `DisposableEffect` already
fires `unloadEngine()`. The composer therefore always loads for itself and **never unloads** — a
composer that unloaded on exit would be the thing that rips the model out from under Chat. Cost,
stated: Chat → pencil → Refine pays the load twice, and polishing once leaves ~770 MB resident
until the process dies.

**Load lazily, on the first Refine tap.** Chat loads eagerly because the model *is* the screen. A
composer is for writing; most entries will never be polished, and 770 MB plus seconds of disk I/O
on every FAB tap competes with exactly the responsiveness a text editor needs. Asking for polish
is an explicit act, so two honest labels — "Loading the model…" then "Refining…" — beat a spinner
nobody asked for.

`save()` and `confirmDiscard()` both cancel the polish job: a polish nobody will read is thirty
seconds of CPU.

**Residual races, plainly:** a Refine that loses ordering to a transition's unload fails with "The
model is not loaded." — clean and honest, the button reads "Try refining again". `LlamaBridge` is
a global `expect object`; the mutex fix closes the window this app can close, but a
Llamatik-internal one may remain and is not visible from here.

### `domain/PolishPrompt.kt` — the narrowest useful ask

Shaped exactly like `ChatPrompt.kt`: pure, Gemma turn markers, no system role, pinned by
`PolishPromptTest`. One job — spelling, grammar, punctuation. **Not** a mood colour and **not** a
follow-up question: a 1B model asked for three fields produces malformed structure often enough to
need a parser plus a failure path, and `docs/LOCAL_AI.md` already refused the chat screen's prompt
card on that exact ground.

The **title is deliberately not sent** — it is a label, not prose, and including it invites the
model to fold it into the body.

| Column | Filled by |
|---|---|
| `fixed_text` | the model |
| `feeling_color` | nobody — stays null. The user's mood lives in `mood` (§3) |
| `ai_question` | nobody — same refusal as the chat prompt card |

`saveAiResult` is **not** used by this flow; it stays for a future batch pass.

### `rawText` is never touched

It is documented "Exactly what the user wrote, untouched", so the model's output must reach
neither the field being typed in nor `raw_text`.

1. Tokens stream into `PolishState.Running(partial)`, rendered in a preview card below the body.
2. On completion, `PolishState.Ready(text, millis)` — `millis` timed across the real call, the
   same condition `docs/LOCAL_AI.md` already set for chat's tokens/sec.
3. The card shows the prose in italic Newsreader (the design system's pull-quote treatment) with
   **"Keep this version"** / **"Discard"**.
4. "Keep this version" sets `acceptedPolish` and `polishedFrom = body`. **It does not touch
   `body`** — nothing is rewritten under the cursor.
5. `save()` writes `rawText = body` and `fixedText = fixedTextToSave` in one insert. No id is
   needed, so **Refine works before save**, which is the only sequence that makes sense here.
6. Editing after accepting makes `fixedTextToSave` return null (`polishedFrom != body`) and takes
   the card down. A correction of text since rewritten is a correction of nothing. Pure derived
   property, so `commonTest` pins it.

### The truncation risk, not smoothed over

`MAX_TOKENS = 512` and `LlmEngine.generate` has no per-call cap, so a long entry's polished
version **will** be truncated with no signal that it stopped at the cap rather than the end.
Ship it with a plain caption — *"The model stops after a fixed length — check the end before you
keep it."* A length heuristic would be a guess dressed as a check. The real fix is a per-call
`maxTokens` on the `core` seam, and that is its own piece of work.

---

## 7. Navigation

`Destinations.kt`: `@Serializable data object CreateJournal`, a **flat sibling** of the tabs in
`mainGraph`. `selectedTab()` walks `[CreateJournal, MainGraph]` against the three tab routes,
matches nothing, returns `null`, and `MainNavBar`'s `AnimatedVisibility` fades the bar out.
Nested under `Journal` the hierarchy walk would find `Journal` and keep the bar up — hence flat.

FAB: `navController.navigate(CreateJournal) { launchSingleTop = true }`. A push, not `switchTab`
(which pops to the graph root with `saveState`). `launchSingleTop` is load-bearing: a double-tap
must not stack two composers sharing one graph-scoped view model.

**This removes the bar's only use of the journal view model**, so `MainNavBar(navController,
container, modifier)` → `MainNavBar(navController, modifier)`, `MainNavBarContent` loses
`container` and `entry`, `AppNavHost` drops the argument, and `journalViewModel()` goes back to
`private`.

**Closing itself**, given screens never see a `NavController` and there is no `SharedFlow`
precedent: a `finished: Boolean` on the state. The graph does
`LaunchedEffect(state.finished) { if (it) { popBackStack(); viewModel.acknowledgeClose() } }`, and
`acknowledgeClose()` clears only that flag so the 320 ms exit animation still renders the entry
rather than a composer visibly emptying itself.

A plain Boolean, not a counter like `composeRequest` — that was a counter because sender and
receiver sat in different compositions and the request could repeat; neither is true here.

**Draft reset is keyed on the back-stack entry id**, via `startComposing(backStackEntry.id)`, not
an `onDispose`. `DisposableEffect`'s `onDispose` fires on **rotation** too — for Chat's engine
that is merely wasteful (a pre-existing wart: rotating Chat unloads and reloads 770 MB), but for a
draft it would be silent data loss. `startComposing` is idempotent on the same id.

### Dead code this deletes

`JournalUiState.composeRequest` / `requestingCompose()` / `draft` / `saving` / `canSave`;
`JournalViewModel.requestCompose()` / `onDraftChange()` / `saveDraft()`; the `FocusRequester`, its
`LaunchedEffect` and the composer `SectionCard` in `JournalScreen` (plus its `onDraftChange` /
`onSave` params); and in `JournalUiStateTest` the two compose-request tests, with the four
`canSave` tests **moved** to `CreateJournalUiStateTest`. `errorMessage`/`dismissError` survive —
deletion still fails.

---

## 8. The screen

```
feature/journal/ui/CreateJournalViewModel.kt   state + view model
feature/journal/ui/CreateJournalScreen.kt      screen + previews
feature/journal/ui/components/Chips.kt         MoodChip, TagChip, PresetChip
feature/journal/ui/components/DateAnchorCard.kt
feature/journal/ui/components/TagRow.kt
feature/journal/ui/components/PolishCard.kt
```

All under the feature, none in `core` — each pattern-matches a journal domain type, the
`DiagnosticRow` precedent.

`CreateJournalUiState` in house style: one immutable data class, derived `val ... get()` so
`commonTest` pins them as pure logic; one `MutableStateFlow` + `asStateFlow()`;
`viewModelScope.launch` with `catch (cancellation: CancellationException) { throw cancellation }`
before the generic catch; `_uiState.update { it.copy(...) }` only.

**Top bar: text buttons, zero new icons.** `MindfulIcons` has no close, back, save or calendar
glyph, and `EntryCard` already set the precedent in this very file ("A text button, not an icon:
MindfulIcons has no trash glyph"). Each new glyph is Material Symbols path data split at fixed
columns, which `CLAUDE.md` flags as the standard way to silently corrupt a shape. A fixed
`Row(SpaceBetween)` holds `TextButton("Cancel")` and `Button("Save", pill)`. The centred Stitch
title is dropped — every other screen puts its title on its own line.

Existing glyphs cover the rest: `Schedule` (the date card is about *when*), `Lock`, `Add`,
`Check`, `Psychology`; remove-tag is a trailing `"×"` with a real `contentDescription`.

**Insets.** The floating bar is not drawn on this route, so `FloatingNavBarDefaults.ContentInset`
does **not** apply — needs an explicit comment, because both neighbouring screens reserve it and
the next person will copy that reflexively. No bar also means none of `ChatScreen`'s
`navBarReserve` arithmetic:

```kotlin
Column(Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime))) {
    TopBar(...)                                     // fixed; never inset by the ime
    Column(Modifier.weight(1f)
            .windowInsetsPadding(WindowInsets.ime)  // outside verticalScroll: it shrinks the viewport
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.margin, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)) { /* sections */ }
}
```

**Save lives in the fixed top bar** — above the scroll area and outside the ime inset, so the
keyboard physically cannot cover it. This also matches Stitch, which puts Save top-right.

Scroll order: brand mark + "New entry" + `Badge(Lock, "Stays on this device")` → `DateAnchorCard`
→ "How did it feel?" + `MoodChipRow` → title field → body field → word-count row + "Clear" →
`PolishCard` → `TagRow` → error row (reusing `JournalScreen`'s existing shape). Tags get their own
card rather than sharing the polish card as Stitch does — bundling them under a "local model"
heading implies the tags are AI-generated.

**Text fields:** `BasicTextField` with a hand-rolled placeholder (the `ChatComposer` precedent),
not `OutlinedTextField` — the body is the page, not a box. Title `headlineMedium` + `singleLine`
+ `ImeAction.Next`; body `headlineSmall` + `ImeAction.Default` so Enter inserts a newline. First
use of `KeyboardOptions` in the repo, and the right place for it.

Stitch asks for line-height 1.8 on the body; `headlineSmall` is 20/28. **Keep 28** — a hand-set
`lineHeight` in a screen is the typographic equivalent of a raw `dp`. If looser leading is right
for long prose it belongs in `Type.kt`, and in Stitch before that. A deliberate deviation.

**Date card:** `Schedule` icon, the formatted date and time, then pills Today / Yesterday / Pick a
date. `DatePickerDialog` is confirmed present in commonMain of `material3 1.12.0-alpha03`. Two
caveats: it is `@ExperimentalMaterial3Api` and brings its own typography opinions that will not
match Newsreader, and `DatePickerState.selectedDateMillis` is **UTC midnight** — convert via
`LocalDate`, never by treating it as a local instant.

**Back:** `BackHandler(enabled = true) { onClose() }` from `androidx.compose.ui.backhandler`
(available transitively through `compose.ui` 1.12.0; `@ExperimentalComposeUiApi`; first use here).
Without it system back pops past the confirm dialog and eats the draft.

**Discard dialog:** the app's first `AlertDialog` — chosen over a sheet because a destructive
confirmation should not be swipe-dismissible. "Discard this entry?" / "Discard" (error colour) /
"Keep writing".

Previews: mid-write, `PolishState.Ready` (otherwise only reachable by loading 770 MB — its
`millis` is a made-up fixture, which is fine in a preview and nowhere else; say so, as
`ChatScreenPreview` already does), empty, and model-not-installed.

---

## 9. Tests — `commonTest` only

New: `JournalTagsTest`, `JournalDatesTest`, `JournalMoodTest`, `PolishPromptTest`,
`CreateJournalUiStateTest`, `CreateJournalViewModelTest`. Changed: `JournalRepositoryTest`
(+ `FakeJournalDao`), `JournalUiStateTest` (down to two tests), `MainTabsTest`.

The cases that carry the design decisions:

- `savingAnAiResultLeavesTheUsersMoodAndTagsAlone` — the `mood`-vs-`feeling_color` decision (§3).
- `backdatingKeepsTheTimeOfDayYouSavedAt`, `anchoringToTodayIsExactlyNow` (§4).
- `theSameInstantIsADifferentDayInADifferentZone` — justifies zone being a parameter.
- `anchoringAcrossASpringForwardStillLandsOnTheChosenDate` (`2026-03-08`, `America/New_York`).
- `aTagContainingTheSeparatorCannotSplitIntoTwo`, `decodingWhatWasEncodedGivesTheNormalisedList`.
- `aPolishAcceptedThenEditedIsNotSaved` — the §6 step-6 rule.
- `theTitleIsNeverSent`, `nothingAsksForAColourOrAQuestion` — pin the narrow ask against a future
  well-meaning edit.
- `thereIsNoReadingTimeAnywhereInTheCopy` and `noLabelClaimsEncryptionOrALatency` — sweep every
  label branch for `"min read"`, `"encrypt"`, `"0ms"`, `"Neural"`, `"sync"`. These exist to stop
  the struck copy being helpfully added back.
- `startComposingTwiceWithTheSameSessionKeepsTheDraft` — the rotation guard.
- `createJournalIsNotATab` in `MainTabsTest` — the testable half of "the bar hides itself".

Note for whoever writes the view-model tests: `CLAUDE.md`'s warning is about Ktor's `MockEngine`.
A **fake `LlmEngine`** returning a plain `flow { }` *does* run on the test scheduler, so
`advanceUntilIdle()` is a real synchronisation point here.

Preview fixtures in `JournalScreen.kt` and `JournalUiStateTest.kt` use `timestamp = 0L`; once the
archive renders dates they read **Jan 1, 1970**. Move them to `1_792_716_300_000L`.

---

## 10. Docs to update

- `docs/LOCAL_AI.md` — a "What the Create Journal screen deliberately does not claim" table in the
  same shape as the two that exist (voice, photos, "0ms", "Neural Core", encryption, autosave,
  read time, the prompt card); the two-caller engine rule and the mutex fix under "Lifetime"; what
  polish writes (`fixed_text` only); and the `MAX_TOKENS` truncation limit.
- `docs/THEME.md` — the six-moods-on-four-accents quantisation and why the palette was not
  extended; the line-height deviation beside the no-raw-`dp` rule.
- The two stale KDocs named at the top of this file.

---

## 11. Order of work

1. The `LlamatikEngine` mutex fix (§1) — small, independent, a prerequisite.
2. `JournalTags.kt` + `JournalMood.kt` + `PolishPrompt.kt` and their tests — pure, provable now.
3. Catalog + `build.gradle.kts` for kotlinx-datetime. **Gradle sync here.**
4. `JournalDates.kt` + tests.
5. `JournalEntry` + `version = 4` + `MIGRATION_3_4` + `DeGeneralMigrations`. **Build here** to
   generate `4.json`, then the schema check below.
6. `JournalRepository.write` + the DAO tiebreak + `JournalRepositoryTest`/`FakeJournalDao`.
7. `CreateJournalViewModel` + `CreateJournalUiStateTest`.
8. `components/`, then `CreateJournalScreen` + previews.
9. Navigation wiring, then the deletions in the Journal tab.
10. `CreateJournalViewModelTest`, preview-fixture timestamps, docs.

---

## 12. Verifying it

```bash
python3 tools/check_theme_tokens.py                # should be unaffected; run it to prove that
./gradlew :shared:testAndroidHostTest              # also generates shared/schemas/.../4.json
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64   # proves the new dependency is genuinely common
```

Then check the generated schema by hand:

1. `shared/schemas/.../4.json` exists and is new. Never hand-write it.
2. Its `journal_entries.createSql` ends
   ``..., `timestamp` INTEGER NOT NULL, `title` TEXT, `mood` TEXT, `tags` TEXT)`` — three new
   columns at the end, in the same order as the three `ALTER`s.
3. The three new `fields` entries show `"affinity": "TEXT"` and **no `"notNull": true`** (Room
   omits it when false — compare `fixed_text` in `3.json`). If one shows it, the Kotlin type lost
   its `?`.
4. `chat_messages` is byte-identical between `3.json` and `4.json`.

On device, in this order:

5. **Install over a build that already has entries** — the only run that exercises
   `MIGRATION_3_4`. Old entries open, with null title/mood/tags.
6. Pencil FAB → the composer opens full-screen and the floating bar is gone.
7. Write, pick a mood, add two tags, Save → it appears at the top of the archive; going back does
   not resurrect the draft.
8. Backdate to Yesterday, save, reopen → the date reads yesterday and the time reads **when you
   pressed Save**, not 12:00 AM.
9. Tap Refine → "Loading the model…" then a suggestion; keep it, save, and confirm in the archive
   that the body is still **your** words.
10. Rotate mid-draft — nothing is lost. System back with text → the confirm dialog, not a pop.
11. FAB from the Chat tab with the engine loaded, twice in a row — the case §1 and §6 exist for.

The three things most likely to be subtly wrong on a real device: the focused body field landing
above the keyboard, `BackHandler` firing on this route, and a Refine tapped immediately after a
Chat → composer transition.

**Nothing in this plan has been compiled, run or rendered.** There is no AVD and no Mac on this
machine, so the iOS half would be written blind and the screen has never been drawn.
