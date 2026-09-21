# The Mindful Scribe theme

This is the guide to `shared/src/commonMain/kotlin/com/example/de_general/core/ui/theme/`. Read it
before you build a screen, change a colour, or touch a font.

## Where the theme comes from

The theme is not designed in this repo. It is **ported** from a design system called
**"Mindful Scribe"**, which lives in Google Stitch:

| | |
|---|---|
| Stitch project | `projects/13240507270798585972` — "AI Daily Journal UI" |
| Design system | `Mindful Scribe` (asset `c21ce6f0be904afab06b485b9641cbd7`, version 1) |
| Colour mode | `LIGHT` only |
| Fonts | Newsreader (headings), Plus Jakarta Sans (body & labels) |

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
    ├── Color.kt          47 colour tokens → MindfulScribeLightColors
    ├── Type.kt           11 type slots → Material's 15
    ├── Shape.kt          roundness scale + the pill shape
    ├── Spacing.kt        8pt scale, breakpoints, reading width
    ├── Elevation.kt      the five depth tiers
    ├── MoodColors.kt     mood accents + the feelingColor bridge
    ├── Theme.kt          MindfulScribeTheme { } and MindfulTheme
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
4. **No dark mode.** See below.

## Adding dark mode

`MindfulScribeTheme` has no `isSystemInDarkTheme()` branch, on purpose. Stitch produced no dark
tokens, and hand-picking dark colours here would fork the theme away from its upstream.

The right order is:

1. Generate a dark variant of the design system in Stitch (`update_design_system` /
   `create_design_system_from_design_md`).
2. Port its `namedColors` into a `MindfulScribeDarkColors` in `Color.kt`, the same way the light
   scheme was ported.
3. Add the branch in `Theme.kt` and re-derive `LocalMindfulElevation` from the active scheme —
   `mindfulElevation(colors)` already takes the scheme, so it follows automatically.

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

The `theme.namedColors` map on the response is what `Color.kt` mirrors. `theme.typography`,
`theme.spacing` and the `rounded` block in `theme.designMd` are what `Type.kt`, `Spacing.kt` and
`Shape.kt` mirror.

### Checking the port didn't drift

`Color.kt` is a hand transcription of 47 hexes, which is exactly the kind of thing that rots
silently. After any change to the palette on either side, diff them:

```bash
python3 tools/check_theme_tokens.py
```

It fails loudly on a missing token, a mismatched hex, or a token that exists but was never wired
into `lightColorScheme(...)`.

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
specimen renders every type slot at size, every surface and role colour, the five depth tiers, the
mood chips and the pill components. **If something in the specimen looks wrong, the theme is wrong,
not your screen.** A compiling build proves nothing about whether a font actually loaded.
