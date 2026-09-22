# The Mindful Scribe theme

This is the guide to `shared/src/commonMain/kotlin/com/example/de_general/core/ui/theme/`. Read it
before you build a screen, change a colour, or touch a font.

## Where the theme comes from

The theme is not designed in this repo. It is **ported** from two sibling design systems in
Google Stitch — **"Mindful Scribe"** for light and **"Nocturnal Sanctuary"** for dark:

| | |
|---|---|
| Stitch project | `projects/13240507270798585972` — "AI Daily Journal UI" |
| Light | `Mindful Scribe` (asset `c21ce6f0be904afab06b485b9641cbd7`), `colorMode: LIGHT` |
| Dark | `Nocturnal Sanctuary` (asset `3d19f1af088440eaa8bba592336c39d2`), `colorMode: DARK` |
| Fonts | Newsreader (headings), Plus Jakarta Sans (body & labels) — shared by both |

That matters for how you change things. **Stitch is upstream; this directory is downstream.** If a
colour looks wrong, the fix is usually in Stitch, followed by a re-port here — not a hand-edit of
`Color.kt`. See [Re-syncing from Stitch](#re-syncing-from-stitch).

### Source-of-truth rule

The Stitch design system carries the same information twice: a machine-readable token map
(`namedColors`, `typography`, `spacing`, `rounded`) and a prose style guide. **They disagree in
places.** The prose says the canvas is `#FBF9F5` and surface-container-low is `#F3EFE9`; the token
map says `#fdf9f5` and `#f7f3ef`.

**The token map wins.** It is what the generated screens actually render, so it is what this theme
transcribes. The prose is used only for things the token map has no field for — the mood accents,
the elevation tiers and the component anatomy.

## What is where

```
shared/src/commonMain/
├── composeResources/font/          4 variable fonts + their OFL licenses
└── kotlin/com/example/de_general/core/ui/theme/
    ├── Color.kt          47 light colour tokens → MindfulScribeLightColors
    ├── ColorDark.kt      47 dark colour tokens → MindfulScribeDarkColors
    ├── Type.kt           11 type slots → Material's 15
    ├── Shape.kt          roundness scale + the pill shape
    ├── Spacing.kt        8pt scale, breakpoints, reading width
    ├── Elevation.kt      the five depth tiers
    ├── MoodColors.kt     mood accents + the feelingColor bridge
    ├── Theme.kt          MindfulScribeTheme(darkTheme) { } and MindfulTheme
    ├── ThemeMode.kt      System / Light / Dark — the Settings choice, and isDark()
    └── ThemePreview.kt   MindfulScribeSpecimen() — the visual reference
```

## Using the theme

Everything Material has a home for is on `MaterialTheme`. Everything else is on `MindfulTheme`,
which is shaped deliberately like it so the two read the same at a call site:

```kotlin
@Composable
fun EntryCard(entry: JournalEntry) {
    val tier = MindfulTheme.elevation.card
    val mood = MindfulTheme.moods.nearestTo(entry.feelingColor)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(tier.container, MaterialTheme.shapes.medium)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = tier.borderAlpha),
                MaterialTheme.shapes.medium,
            )
            .padding(MindfulTheme.spacing.lg),   // the design system is strict about lg here
    ) {
        Text(entry.rawText, style = MaterialTheme.typography.bodyLarge)
    }
}
```

Rules of thumb:

- **Never write a hex in a screen.** If you need a colour that isn't in the theme, it belongs in
  the theme (or in Stitch) first.
- **Never write a raw `dp` for padding.** Use `MindfulTheme.spacing`. The design system runs on an
  8pt rhythm and ad-hoc values break it quietly.
- **Never hand-set a `lineHeight` in a screen either** — it is the typographic equivalent of a raw
  `dp`. Stitch asks for line-height 1.8 on the Create Journal body field; `headlineSmall` is 20/28
  and 28 is what ships. If looser leading is right for long prose it belongs in `Type.kt`, and in
  Stitch before that. A deliberate deviation, recorded here rather than buried in a screen.
- Cards use `spacing.cardPadding`, never a literal. That token is **16dp, not the design system's
  24dp**: the screen gutter and the card padding stack, and at the design's numbers a 360dp phone
  spent 88dp on horizontal padding before drawing anything. `spacing.margin` was tightened from
  20dp to 16dp for the same reason. Both are deliberate deviations — change them together.
- Buttons, chips, filters and badges are **pills** — `MindfulShapes.full`, not
  `MaterialTheme.shapes.large`.

### Type slots

Material has 15 slots; the design system names 11. Four are deliberately doubled up:

| Design slot | Font · size/line · weight | Material slot(s) |
|---|---|---|
| `display-lg` | Newsreader 40/48 w400, -0.02em | `displayLarge` |
| `display-lg-mobile` | Newsreader 32/40 w400, -0.01em | `displayMedium` |
| `headline-lg` | Newsreader 28/36 w400, -0.01em | `displaySmall`, `headlineLarge` |
| `headline-md` | Newsreader 24/32 w500 | `headlineMedium` |
| `headline-sm` | Newsreader 20/28 w500 | `headlineSmall`, `titleLarge` |
| `body-lg` | Plus Jakarta Sans 18/28 w400, -0.01em | `bodyLarge` |
| `body-md` | Plus Jakarta Sans 16/24 w400 | `bodyMedium` |
| `body-sm` | Plus Jakarta Sans 14/20 w400 | `bodySmall` |
| `label-lg` | Plus Jakarta Sans 14/20 w600, 0.01em | `labelLarge`, `titleMedium` |
| `label-md` | Plus Jakarta Sans 12/16 w600, 0.02em | `labelMedium`, `titleSmall` |
| `label-sm` | Plus Jakarta Sans 11/14 w500, 0.03em | `labelSmall` |

The doubling is intentional — the design system defines no title scale, and its labels are what its
screens use in those positions. `display-lg-mobile` has no Material equivalent worth the name;
reach it as `mindfulScribeTypeTokens().displayLgMobile`.

Italic Newsreader is not a slot but the design system calls for it on prompts and pull-quotes:
`style.copy(fontStyle = FontStyle.Italic)`.

### Mood accents and `feelingColor`

`JournalEntry.feelingColor` stores whatever hex the model picked for an entry's mood. Letting that
straight onto the canvas would break the palette, so quantise it:

```kotlin
val mood = MindfulTheme.moods.nearestTo(entry.feelingColor)   // → Calm | Happy | Reflective | Energetic
mood.accent       // the chip dot, waveform tint, selected bead
mood.container    // selected-chip background
mood.onContainer  // text on that background
mood.glow         // the design's 4% "Mood Glow Bleed" wash
```

`nearestTo` parses `#RRGGBB` / `#AARRGGBB` (with or without the `#`) and snaps to the closest of
the four accents using redmean distance. Null, blank or unparseable input returns Calm.

The **mood** is always chosen against the *light* accents, whichever palette is active; only the
returned colours come from the active one. Snapping against the dark accents instead would let one
entry read as Calm in light mode and Reflective in dark. `MoodPaletteTest` pins this.

### Six moods on four accents

`feature/journal/domain/JournalMood.kt` offers the design's six mood chips — Calm, Joyful,
Reflective, Grateful, Energized, Pensive — against this palette's **four** accents. Grateful borrows
Happy's, Pensive borrows Reflective's, so two pairs share a selected colour.

That quantisation is the accepted cost of staying faithful to the design, and the palette was
deliberately **not** extended to six. Stitch is upstream; adding two accents here would fork this
directory away from its source, and `tools/check_theme_tokens.py` only reads `Color.kt`, so the
drift would not even be caught. Only one chip is ever selected and the label disambiguates, so the
shared colour costs nothing legible.

If six real accents are wanted: generate them in Stitch, re-port `Color.kt` and `MoodColors.kt`,
and `JournalMood` collapses to a one-to-one `accent: Mood`. Nothing else moves. `JournalMoodTest`
pins the current mapping, including that exactly four accents are in use.

The person's mood and `feelingColor` are **different columns**. `mood` is what they picked;
`feelingColor` is what the model read, and `saveAiResult` overwrites it. Do not fold them together.

## Known gaps

These are real limits, not oversights. Each is one constant in one file, so fixing one later is
cheap — please don't work around them in a screen.

1. **Two-layer shadows collapse to one.** The design specifies
   `0px 4px 16px -2px …, 0px 1px 4px 0px …`; `Modifier.shadow` renders a single layer.
   `MindfulTier.shadow` is an approximation of the pair.
2. **No frosted glass.** Depth tier 3 (nav bars, floating control bars) wants
   `backdrop-filter: blur(16px) saturate(140%)`. Compose Multiplatform has no portable backdrop
   blur, so the tier falls back to an opaque tonal surface. Marked `TODO` in `Elevation.kt`.
3. **Font variations need API 26.** `minSdk` is 24, but Android honours font variation settings
   only from 26. On API 24–25 the variable fonts render at their default instance (wght 400) with
   synthetic bolding, so headings lose the 500 weight and Newsreader loses its optical sizing.
   Layout is unaffected.
4. **The Android window flashes light before the first frame.** The manifest's window theme is
   `Theme.Material.Light`, which draws before Compose does. It cannot know the in-app choice (that
   lives in a file Kotlin reads), so a person on Dark can see a white window for a moment at cold
   start. Fixing it means a DayNight window theme plus a splash screen — its own change.

## Dark mode

Dark is Nocturnal Sanctuary, ported into `ColorDark.kt` the same way Mindful Scribe went into
`Color.kt` — same private names, same order. `MindfulScribeTheme(darkTheme)` picks the scheme, the
mood palette and the shadow tint; the depth tiers follow the scheme through `mindfulElevation`.

The person chooses **System**, **Light** or **Dark** in Settings. The choice is `ThemeMode`, stored
as one word by `core/data/ThemePreferences.kt` and read synchronously at startup so the first
frame is already right. `App` resolves it (`mode.isDark(isSystemInDarkTheme())`) and reports the
answer to `MainActivity`, which sets the system-bar icons from it — never from
`SystemBarStyle.auto`, which follows the *system* and is wrong whenever the two differ.

Things to know before touching it:

- **Only the snake_case keys are ported.** Nocturnal Sanctuary's `namedColors` also carries
  hyphenated duplicates taken from its prose, and some disagree:
  `surface_container #1d201f` vs `surface-container #212624`, `on_surface #e1e3e1` vs
  `on-surface #f0eee9`, `outline_variant #414845` vs `outline-variant rgba(255,255,255,0.08)`.
  The snake_case set is the Material scheme and lines up with the light map, so it wins — the
  token-map rule above, applied again. If the hyphenated values are what looks right, fix the
  snake_case ones in Stitch and re-port.
- **The dark mood accents come from the token map** (`mood-*-bg`, `mood-*-text`, which have no
  conflicting twin). The dot and the text share the `-text` token, because the design gives no
  separate accent. Energetic is **rose** in dark where it is coral in light — same slot.
- **Not a hue-for-hue inversion.** Dark secondary is a second sage and dark tertiary a cool grey,
  where light has warm sand and lavender. Upstream's decision.
- **Shadow tint is per scheme** — warm charcoal in light, black in dark (`LightShadowTint`,
  `DarkShadowTint` in `Elevation.kt`).
- Dark tier 3 asks for 80% translucency and `blur(20px)`: Known gap 2 again.

## Re-syncing from Stitch

The design system is reachable over the Stitch MCP server. It is configured at **local** scope in
`~/.claude.json` for this project — if you are a new contributor you need to add it yourself with
your own key:

```bash
claude mcp add --transport http stitch https://stitch.googleapis.com/mcp \
  --header "X-Goog-Api-Key: <your key>"
```

> Note the argument order. `--header` is variadic, so a URL placed *after* it gets swallowed and
> the command fails with `missing required argument 'commandOrUrl'`. URL first, header last.

Then, to pull the current tokens:

```
mcp__stitch__list_design_systems(projectId: "13240507270798585972")
```

The response holds both design systems. Mindful Scribe's `theme.namedColors` is what `Color.kt`
mirrors; Nocturnal Sanctuary's snake_case keys are what `ColorDark.kt` mirrors, and its
`mood-*` keys are what `MindfulMoodPaletteDark` mirrors. `theme.typography`,
`theme.spacing` and the `rounded` block in `theme.designMd` are what `Type.kt`, `Spacing.kt` and
`Shape.kt` mirror.

### Checking the port didn't drift

`Color.kt` and `ColorDark.kt` are hand transcriptions of 47 hexes each, which is exactly the kind
of thing that rots silently. After any change to either palette on either side, diff them:

```bash
python3 tools/check_theme_tokens.py
```

It checks both files and fails loudly on a missing token, a mismatched hex, or a token that exists
but was never wired into `lightColorScheme(...)` / `darkColorScheme(...)`. The mood palettes are
not checked.

## Fonts

Four variable TTFs in `composeResources/font/`, ~1.3 MB total, both families
[SIL Open Font License 1.1](https://openfontlicense.org) — their `OFL.txt` files sit beside them
and must stay there.

| File | Source |
|---|---|
| `newsreader_variable.ttf` | `google/fonts` → `ofl/newsreader/Newsreader[opsz,wght].ttf` |
| `newsreader_italic_variable.ttf` | `ofl/newsreader/Newsreader-Italic[opsz,wght].ttf` |
| `plus_jakarta_sans_variable.ttf` | `ofl/plusjakartasans/PlusJakartaSans[wght].ttf` |
| `plus_jakarta_sans_italic_variable.ttf` | `ofl/plusjakartasans/PlusJakartaSans-Italic[wght].ttf` |

`google/fonts` ships only the variable cuts for both families — there are no static instances to
fall back to. One file per family per slant covers every weight the design needs.

Compose resource filenames become Kotlin identifiers (`Res.font.newsreader_variable`), so they must
be lowercase with underscores. The upstream names contain brackets and hyphens and **will** break
the generated accessors if copied verbatim.

## Verifying a change

```bash
./gradlew :androidApp:assembleDebug              # Android; also proves Res.font.* accessors resolve
./gradlew :shared:compileKotlinIosSimulatorArm64 # proves the theme is genuinely common code
./gradlew :shared:testAndroidHostTest            # existing tests
python3 tools/check_theme_tokens.py              # palette matches Stitch
```

Then look at it. Open `ThemePreview.kt` in Android Studio's preview pane, or run the app — the
specimen has a light and a dark preview, and renders every type slot at size, every surface and role colour, the five depth tiers, the
mood chips and the pill components. **If something in the specimen looks wrong, the theme is wrong,
not your screen.** A compiling build proves nothing about whether a font actually loaded.
