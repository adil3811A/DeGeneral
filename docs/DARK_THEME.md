# Dark theme and the System / Light / Dark setting

This is the plan for giving the app a dark theme and letting the person pick **System**, **Light**
or **Dark** from the Settings tab.

> **Status (2026-09-23): implemented, not yet built or run.** `THEME.md § Dark mode` now describes
> the result. Where the code departs from this plan:
>
> - **§5, directory:** `AppContainer` takes a `PreferencesStorage` (an `expect class` in
>   `core/data`, like `ModelStorage`), not a raw `okio.Path`. `androidApp` doesn't depend on
>   okio, so it couldn't name the type.
> - **§5, system bars:** `MainActivity` doesn't collect the flow itself. `App(container,
>   onDarkThemeChange)` resolves the theme in common code and reports a Boolean, so the choice
>   is resolved in one place and `androidApp` never touches a coroutine type.
> - **§2, tests:** the dark accents do **not** snap back to their own mood through the light
>   reference (pale sage `#A8D3C5` → Reflective, pale amber `#F0CF9E` → Energetic). That's fine,
>   because the requirement is only that both palettes agree, and it's why the reference has to
>   be one fixed palette. `MoodPaletteTest` pins what actually holds.
> - **New known gap:** the Android window theme (`Theme.Material.Light`) can flash white before
>   Compose's first frame for someone on Dark. Recorded in `THEME.md`, not fixed.
> - `ThemePreview.kt` swatch labels now read the hex off the drawn colour. They used to hard-code
>   the light hexes, which would have been wrong in the dark preview.

Read `THEME.md` first. This doc carries out that doc's "Adding dark mode" section and changes
several of its rules.

## Where the dark colours come from

`Color.kt` has **no** dark colours today. It is light-only on purpose, and both `Theme.kt` and
`THEME.md` say not to hand-pick dark colours at the Kotlin layer. Stitch is upstream.

The upstream step is already done, though. The Stitch project now holds a second design system:

| | |
|---|---|
| Stitch project | `projects/13240507270798585972`, "AI Daily Journal UI" |
| Design system | `Nocturnal Sanctuary` (asset `3d19f1af088440eaa8bba592336c39d2`) |
| Colour mode | `DARK` |
| Fonts / roundness | Newsreader + Plus Jakarta Sans, `ROUND_FULL`, same as Mindful Scribe |

So this is a **port**, done the same way the light theme was ported. `Type.kt`, `Shape.kt` and
`Spacing.kt` don't change.

### Source-of-truth rule, and a conflict inside the dark map

The dark `namedColors` map contains **two overlapping sets of keys**:

- **47 snake_case keys** (`surface_container`, `on_surface`, …). These match the light map one to
  one, and they are the Material scheme that Stitch generated.
- **Hyphenated keys** (`surface-container`, `on-surface`, `mood-sage-bg`, `privacy-badge-text`, …).
  These are extras taken from the prose's component specs.

Some of them disagree:

| Role | snake_case | hyphenated |
|---|---|---|
| surface container | `surface_container #1d201f` | `surface-container #212624` |
| on surface | `on_surface #e1e3e1` | `on-surface #f0eee9` |
| surface bright | `surface_bright #373a38` | `surface-bright #212624` |
| outline variant | `outline_variant #414845` | `outline-variant rgba(255,255,255,0.08)` |

**Decision: the `ColorScheme` is built from the snake_case set only.** It is the machine-readable
Material scheme, it lines up key for key with the light port, and the drift checker can hold it
still. The hyphenated duplicates are **not** ported. The hyphenated **mood** and **privacy** keys
have no snake_case twin and no conflict, so they *are* used, for the mood palette (below).

Don't merge the two sets by hand. If the hyphenated values are the ones that look right on a
device, fix the snake_case values in Stitch and re-port.

One more thing to know before you look at it: the dark system is **not** a hue-for-hue inversion of
the light one. Light secondary is warm sand and light tertiary is twilight lavender. In the dark
system, secondary is another sage and tertiary is a cool grey. That's the upstream design, so port
it as it is. If it reads wrong, fix it in Stitch.

## The dark tokens to port

Verbatim from `mcp__stitch__list_design_systems(projectId: "13240507270798585972")` →
Nocturnal Sanctuary → `theme.namedColors`, snake_case keys only. Re-pull before porting in case
upstream moved.

| Token | Hex |
|---|---|
| `background` | `#111413` |
| `error` | `#ffb4ab` |
| `error_container` | `#93000a` |
| `inverse_on_surface` | `#2e3130` |
| `inverse_primary` | `#3d665b` |
| `inverse_surface` | `#e1e3e1` |
| `on_background` | `#e1e3e1` |
| `on_error` | `#690005` |
| `on_error_container` | `#ffdad6` |
| `on_primary` | `#0a372e` |
| `on_primary_container` | `#1d473d` |
| `on_primary_fixed` | `#00201a` |
| `on_primary_fixed_variant` | `#254e44` |
| `on_secondary` | `#06372e` |
| `on_secondary_container` | `#94c2b5` |
| `on_secondary_fixed` | `#00201a` |
| `on_secondary_fixed_variant` | `#234e44` |
| `on_surface` | `#e1e3e1` |
| `on_surface_variant` | `#c0c8c4` |
| `on_tertiary` | `#2b322f` |
| `on_tertiary_container` | `#3b423f` |
| `on_tertiary_fixed` | `#161d1a` |
| `on_tertiary_fixed_variant` | `#414845` |
| `outline` | `#4a534f` |
| `outline_variant` | `#414845` |
| `primary` | `#a5d1c3` |
| `primary_container` | `#8ab5a8` |
| `primary_fixed` | `#bfecde` |
| `primary_fixed_dim` | `#a4d0c2` |
| `secondary` | `#a2d0c3` |
| `secondary_container` | `#255047` |
| `secondary_fixed` | `#beecde` |
| `secondary_fixed_dim` | `#a2d0c3` |
| `surface` | `#111413` |
| `surface_bright` | `#373a38` |
| `surface_container` | `#1d201f` |
| `surface_container_high` | `#282b29` |
| `surface_container_highest` | `#323534` |
| `surface_container_low` | `#191c1b` |
| `surface_container_lowest` | `#0c0f0e` |
| `surface_dim` | `#111413` |
| `surface_tint` | `#a4d0c2` |
| `surface_variant` | `#323534` |
| `tertiary` | `#c2c9c5` |
| `tertiary_container` | `#a7aeaa` |
| `tertiary_fixed` | `#dde4e0` |
| `tertiary_fixed_dim` | `#c1c8c4` |

## The plan, file by file

Paths are under `shared/src/commonMain/kotlin/com/example/de_general/` unless stated.

### 1. `core/ui/theme/ColorDark.kt` (new)

Mirror `Color.kt` exactly. Use the same `private val Primary = Color(0xFF…)` names and the same
comment groups, then end with:

```kotlin
val MindfulScribeDarkColors: ColorScheme = darkColorScheme(
    primary = Primary,
    // … every argument, in the same order as lightColorScheme(...) in Color.kt
)
```

The names are file-private, so they don't clash with `Color.kt`. Keeping the two files shaped the
same way means one drift-checker function can parse both (§7). `scrim` stays at the Material
default, as it does in light.

Rewrite the "Light only" KDoc in `Color.kt` so it points at its dark twin.

### 2. `core/ui/theme/MoodColors.kt`: dark mood palette, and a `nearestTo` fix

Add `MindfulMoodPaletteDark`, built from the hyphenated mood keys:

| Mood | `container` (`mood-*-bg`) | `accent` and `onContainer` (`mood-*-text`) |
|---|---|---|
| Calm, sage | `#243530` | `#a8d3c5` |
| Happy, amber | `#362e24` | `#f0cf9e` |
| Reflective, lavender | `#2c2738` | `#d2c9e3` |
| Energetic, **rose** | `#38252a` | `#e8b4be` |

`privacyShield` = `privacy-badge-text` `#8ab5a8`.

Two decisions to record in the KDoc:

- **accent == onContainer in dark.** The dark prose says the active chip's text *and* its 6px dot
  take "the dedicated mood foreground tint". The dark map has no separate accent token, so it
  isn't invented. (The prose's Mood Glow example says `#8ab5a8` for Calm, not `#a8d3c5`. That's
  the prose disagreeing with the token map again, and the map wins.)
- **Energetic → rose.** The light palette's fourth accent is coral; the dark system's is rose.
  They hold the same slot. `JournalMood`'s six-onto-four mapping doesn't change.

**The trap: `nearestTo(feelingColor)` is palette-relative.** It snaps a stored hex to the
nearest accent *in the current palette*. If it ran against the dark accents, one entry's
`feelingColor` could snap to Calm in light mode and to Reflective in dark mode. The entry's mood
would change when the theme changes. Fix it so the `Mood` is always chosen against a fixed
reference, the light accents, and only the returned colours come from the active palette:

```kotlin
fun nearestTo(feelingColor: String?): MoodAccent {
    val target = parseHexColor(feelingColor) ?: return calm
    val mood = MindfulMoodPalette.all.minBy { colorDistance(it.accent, target) }.mood
    return this[mood]
}
```

This stays a pure function, so it can be tested in `commonTest` (see Tests).

### 3. `core/ui/theme/Elevation.kt` and `Theme.kt`

`mindfulElevation(colors)` already derives every tier from the scheme, so the tiers follow on
their own. The only hard-coded piece is `MindfulElevation.shadowTint = #3C342A`, the warm charcoal
the light prose asks for. The dark prose's shadows are `rgba(0,0,0,…)`. Make it a parameter:

```kotlin
fun mindfulElevation(colors: ColorScheme, shadowTint: Color): MindfulElevation
```

Light passes `#3C342A` and dark passes `Color.Black`. Both values live in the theme, not in a
screen.

`MindfulScribeTheme` gets the branch:

```kotlin
@Composable
fun MindfulScribeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) MindfulScribeDarkColors else MindfulScribeLightColors
    val moods = if (darkTheme) MindfulMoodPaletteDark else MindfulMoodPalette
    // elevation from (colors, shadowTint); provide moods through LocalMoodPalette
}
```

Rewrite the "Light only, on purpose" KDoc so it describes the two upstream systems.

Not ported, and it should say so: dark tier 3 asks for `#212624` at 80% opacity with
`blur(20px)`. That's the same missing portable backdrop blur as light's tier 3 (Known gap 2 in
`THEME.md`), so it stays an opaque tonal surface.

### 4. The preference: `core/ui/theme/ThemeMode.kt` and `core/data/ThemePreferences.kt` (new)

```kotlin
enum class ThemeMode(val storageKey: String) {
    System("system"), Light("light"), Dark("dark");

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    companion object {
        /** Unknown, blank or null → System. A bad file never locks someone into a theme. */
        fun fromStorageKey(key: String?): ThemeMode = entries.firstOrNull { it.storageKey == key?.trim() } ?: System
    }
}
```

`ThemePreferences(fileSystem: FileSystem, file: Path)`:

- It stores **one word in one file** in app-private storage, using okio. Okio is already a
  dependency, and the same pattern keeps `ModelFiles` honest.
- It **reads synchronously in the constructor** into a `MutableStateFlow<ThemeMode>`. That's the
  same trick `AppNavHost` uses with `installState`: the first frame is already in the right theme,
  so there's no light-to-dark flash on launch. The file is a few bytes.
- `val mode: StateFlow<ThemeMode>`.
- `suspend fun setMode(mode: ThemeMode)` updates the flow **first**, so the UI responds on the
  tap, then writes `file.tmp` and `atomicMove`s it over `file`. A crash mid-write leaves the old
  value, never a half-written one.

**Why not Room.** The database is lazy on purpose (`AppContainer`: someone who never finishes
setup never opens it). The theme is needed on frame one, so storing it in Room would open SQLite on
every launch and cost a v5 migration, all for one enum.

**Why not DataStore.** It's a new dependency to store one word, and okio already does the job.

**Placement.** Both files go in `core`, because the app root and `feature/settings` both need them,
and a feature can't be the owner. `core/data` importing `core/ui/theme` stays inside `core`, which
the layout rules allow.

This stores a display preference, nothing personal, and it never leaves the device. There's
nothing to claim about it in the copy.

### 5. Wiring

- **`di/AppContainer.kt`** takes a new `preferencesDirectory: Path` and exposes
  `val themePreferences = ThemePreferences(FileSystem.SYSTEM, preferencesDirectory / "theme_mode")`.
  It's eager, not lazy, because `App` reads it on the first frame. The directory is passed in
  rather than borrowed from `ModelStorage`, which belongs to `feature/onboarding`. Make sure the
  directory exists before the first write (`createDirectories`).
  - `androidApp/.../DeGeneralApplication.kt`: `preferencesDirectory = filesDir.toOkioPath()`.
  - `iosMain/.../MainViewController.kt`: Application Support, resolved the same way as
    `ModelStorage.ios.kt`. **Unverified**, like everything on iOS.
- **`App.kt`**:

  ```kotlin
  val mode by container.themePreferences.mode.collectAsState()
  MindfulScribeTheme(darkTheme = mode.isDark(isSystemInDarkTheme())) { AppNavHost(container) }
  ```

- **`androidApp/.../MainActivity.kt`: the system bars.** This is the part most likely to be
  forgotten. `onCreate` forces `SystemBarStyle.light(...)` today, which gives dark icons, and those
  disappear on a dark canvas. Inside `setContent`, resolve the same boolean and re-apply the bars
  whenever it changes:

  ```kotlin
  val mode by container.themePreferences.mode.collectAsState()
  val dark = mode.isDark(isSystemInDarkTheme())
  DisposableEffect(dark) {
      enableEdgeToEdge(
          statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                           else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
          navigationBarStyle = /* same */,
      )
      onDispose {}
  }
  ```

  Don't use `SystemBarStyle.auto`. It follows the *system* setting, which is wrong the moment
  someone picks Light on a dark phone. Rewrite the comment above the current call, because its
  "#FDF9F5 so dark icons" reasoning is now only half the story.

### 6. Settings UI: `feature/settings/ui/`

- **`SettingsScreen(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit, modifier)`.**
  It takes callbacks only and never sees a repository or `NavController`, so it stays previewable.
- Add an **"Appearance"** `SectionCard` above the existing card, with a short label and three pills:
  **System · Light · Dark**.
- **`components/ThemeModeSelector.kt` (new).** Three pills in a `Row` with
  `spacing` between them, each `MindfulShapes.full`. Selected: `primaryContainer` /
  `onPrimaryContainer`. Unselected: the card tier with an `outlineVariant` hairline. Use
  `selectable` / `Role.RadioButton` inside a `selectableGroup()` so TalkBack reads it as one
  choice of three. If `feature/journal/ui/components/Chips.kt` has the right shape, **copy** the
  pattern. Don't import it, because a feature never imports another feature. If both features
  end up needing the same pill, move it to `core/ui/components` in its own change.
- Keep the honest "Not built yet" card. Change its copy so it's clear only the rest of the tab
  is unbuilt.
- **`SettingsViewModel(themePreferences)` (new).** It exposes `themeMode: StateFlow<ThemeMode>`
  and `fun setThemeMode(mode)`, which launches `setMode` on `viewModelScope` with
  `Dispatchers.IO` (`import kotlinx.coroutines.IO` in common code).
- **`navigation/MainGraph.kt`**: wire the `Settings` destination the way `Journal` and `Chat` are
  wired: a view model factory, `collectAsStateWithLifecycle`, then pass the callbacks.
- **Previews**: `SettingsScreen` in light and in `MindfulScribeTheme(darkTheme = true)`. Add a dark
  preview of `MindfulScribeSpecimen()` in `ThemePreview.kt`. The specimen is the reference: if
  something looks wrong there, the theme is wrong, not the screen.

### 7. `tools/check_theme_tokens.py`: guard both palettes

Add `STITCH_DARK_NAMED_COLORS` (the 47 snake_case keys above) and refactor the existing checks
into one function that takes `(expected_map, kotlin_file, scheme_builder_name)`. Run it twice:

- `STITCH_NAMED_COLORS`, `Color.kt`, `lightColorScheme(`
- `STITCH_DARK_NAMED_COLORS`, `ColorDark.kt`, `darkColorScheme(`

It still fails loudly on a missing token, a mismatched hex, or a token that's defined but never
wired into its scheme. Update the docstring to name both design systems.

The mood palettes stay outside the checker, as they are today. Note that in `THEME.md`.

### 8. `docs/THEME.md`

- Add Nocturnal Sanctuary to the "Where the theme comes from" table. The colour mode becomes
  "LIGHT (Mindful Scribe) + DARK (Nocturnal Sanctuary)".
- Add `ColorDark.kt` and `ThemeMode.kt` to "What is where".
- Remove Known gap 4 ("No dark mode") and replace "Adding dark mode" with a short "Dark mode"
  section covering the snake_case-vs-hyphen rule, accent == onContainer, rose → Energetic, and the
  reference-palette `nearestTo`.
- Add the dark re-sync to "Re-syncing from Stitch".

## Tests (`commonTest`, Adil runs them)

- **`core/ui/theme/ThemeModeTest`**: the `isDark` truth table (3 modes × system dark on/off);
  `fromStorageKey(storageKey)` round trip for every mode; `null`, `""`, `"  "` and `"purple"` all
  map to `System`.
- **`core/data/ThemePreferencesTest`** with okio `FakeFileSystem`:
  - missing file → `System`
  - `setMode(Dark)`, then a **new** instance on the same file → `Dark`
  - a corrupt file → `System`
  - `mode.value` is already `Dark` right after `setMode(Dark)` returns
  - no `.tmp` left behind after a write
- **`core/ui/theme/MoodPaletteTest`**: for a spread of hexes (each accent's own hex from both
  palettes, plus a few in-betweens), `MindfulMoodPalette.nearestTo(h).mood ==
  MindfulMoodPaletteDark.nearestTo(h).mood`. Also null, blank and garbage → Calm in both.
  `JournalMoodTest` should pass unchanged.

The `ThemePreferences` tests don't touch `MockEngine` or virtual time, so the known hang doesn't
apply.

## Risky parts, stated plainly

1. **The hyphen/snake_case conflict.** If the snake_case surfaces look too flat on a real
   screen, the hyphenated ones may be what the designer meant. The fix goes in Stitch, not
   `ColorDark.kt`.
2. **System bars.** Forget `MainActivity` and dark mode ships with invisible status-bar icons.
   That doesn't show up in a Compose preview; only a device shows it.
3. **Hard-coded colours outside the theme.** A grep today finds none in screens. The only
   `Color(0x…)` outside `core/ui/theme` is `Color.Black` as the icon path fill in
   `MindfulIcons.kt`, which is tinted at the call site. Grep again before shipping in case
   something landed since.
4. **iOS.** It compiles and has never run. There, the status bar and the preferences path are
   both unverified.

## Verifying

```bash
python3 tools/check_theme_tokens.py              # both palettes match Stitch
./gradlew :shared:testAndroidHostTest            # ThemeMode, ThemePreferences, MoodPalette tests
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64 # the theme is still common code
```

Then on a device:

1. Settings → **Dark**. The whole app switches at once, including the nav bar, cards and chips.
   Status-bar and nav-bar icons turn light.
2. **Light** on a phone set to dark mode: the app stays light and the bar icons stay dark.
3. **System**: toggle the phone's dark mode from quick settings. The app follows without a
   restart.
4. Pick Dark, kill the app, relaunch. It should open dark on the **first frame**, with no light
   flash, including on the Welcome screen for a fresh install.
5. Open a journal entry with a mood in both themes. It should be the same mood, recoloured.
6. Look at the `ThemePreview.kt` specimen in both schemes.

None of this has been rendered yet, because there's no AVD on this machine. A green build doesn't
prove the screen looks right.
