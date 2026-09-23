#!/usr/bin/env python3
"""Check that Color.kt and ColorDark.kt still match their palettes in Stitch.

Each file is a hand transcription of 47 hexes out of a Stitch design system in
project 13240507270798585972 -- "Mindful Scribe" (light) into Color.kt and
"Nocturnal Sanctuary" (dark) into ColorDark.kt. That is exactly the kind of thing
that rots silently, so this fails loudly, per palette, on:

  * a token in the design system that the Kotlin file does not define
  * a hex that no longer matches
  * a token that is defined but never wired into its colour scheme builder

The expected values below are each system's `theme.namedColors` map. Only the
snake_case keys: Nocturnal Sanctuary also carries hyphenated duplicates from its
prose, deliberately not ported (see ColorDark.kt). The mood palettes in
MoodColors.kt are not checked here. When a design system changes upstream, re-pull
it and update its map in the same commit as the Kotlin file:

    mcp__stitch__list_design_systems(projectId: "13240507270798585972")

Run from anywhere:  python3 tools/check_theme_tokens.py
"""

import re
import sys
from pathlib import Path

# theme.namedColors, verbatim from the Stitch design system.
STITCH_NAMED_COLORS = {
    "background": "#fdf9f5",
    "error": "#ba1a1a",
    "error_container": "#ffdad6",
    "inverse_on_surface": "#f4f0ec",
    "inverse_primary": "#aecdc3",
    "inverse_surface": "#31302e",
    "on_background": "#1c1c19",
    "on_error": "#ffffff",
    "on_error_container": "#93000a",
    "on_primary": "#ffffff",
    "on_primary_container": "#c4e4da",
    "on_primary_fixed": "#02201a",
    "on_primary_fixed_variant": "#304c45",
    "on_secondary": "#ffffff",
    "on_secondary_container": "#706050",
    "on_secondary_fixed": "#241a0d",
    "on_secondary_fixed_variant": "#524436",
    "on_surface": "#1c1c19",
    "on_surface_variant": "#414846",
    "on_tertiary": "#ffffff",
    "on_tertiary_container": "#e6d8f2",
    "on_tertiary_fixed": "#20182a",
    "on_tertiary_fixed_variant": "#4c4357",
    "outline": "#727976",
    "outline_variant": "#c1c8c4",
    "primary": "#324f47",
    "primary_container": "#4a675f",
    "primary_fixed": "#c9e9df",
    "primary_fixed_dim": "#aecdc3",
    "secondary": "#6b5c4c",
    "secondary_container": "#f1dcc8",
    "secondary_fixed": "#f4dfcb",
    "secondary_fixed_dim": "#d7c3b0",
    "surface": "#fdf9f5",
    "surface_bright": "#fdf9f5",
    "surface_container": "#f1ede9",
    "surface_container_high": "#ebe7e4",
    "surface_container_highest": "#e6e2de",
    "surface_container_low": "#f7f3ef",
    "surface_container_lowest": "#ffffff",
    "surface_dim": "#ddd9d6",
    "surface_tint": "#47645c",
    "surface_variant": "#e6e2de",
    "tertiary": "#4f465a",
    "tertiary_container": "#685d73",
    "tertiary_fixed": "#ecddf7",
    "tertiary_fixed_dim": "#cfc1da",
}

STITCH_DARK_NAMED_COLORS = {
    "background": "#111413",
    "error": "#ffb4ab",
    "error_container": "#93000a",
    "inverse_on_surface": "#2e3130",
    "inverse_primary": "#3d665b",
    "inverse_surface": "#e1e3e1",
    "on_background": "#e1e3e1",
    "on_error": "#690005",
    "on_error_container": "#ffdad6",
    "on_primary": "#0a372e",
    "on_primary_container": "#1d473d",
    "on_primary_fixed": "#00201a",
    "on_primary_fixed_variant": "#254e44",
    "on_secondary": "#06372e",
    "on_secondary_container": "#94c2b5",
    "on_secondary_fixed": "#00201a",
    "on_secondary_fixed_variant": "#234e44",
    "on_surface": "#e1e3e1",
    "on_surface_variant": "#c0c8c4",
    "on_tertiary": "#2b322f",
    "on_tertiary_container": "#3b423f",
    "on_tertiary_fixed": "#161d1a",
    "on_tertiary_fixed_variant": "#414845",
    "outline": "#4a534f",
    "outline_variant": "#414845",
    "primary": "#a5d1c3",
    "primary_container": "#8ab5a8",
    "primary_fixed": "#bfecde",
    "primary_fixed_dim": "#a4d0c2",
    "secondary": "#a2d0c3",
    "secondary_container": "#255047",
    "secondary_fixed": "#beecde",
    "secondary_fixed_dim": "#a2d0c3",
    "surface": "#111413",
    "surface_bright": "#373a38",
    "surface_container": "#1d201f",
    "surface_container_high": "#282b29",
    "surface_container_highest": "#323534",
    "surface_container_low": "#191c1b",
    "surface_container_lowest": "#0c0f0e",
    "surface_dim": "#111413",
    "surface_tint": "#a4d0c2",
    "surface_variant": "#323534",
    "tertiary": "#c2c9c5",
    "tertiary_container": "#a7aeaa",
    "tertiary_fixed": "#dde4e0",
    "tertiary_fixed_dim": "#c1c8c4",
}

THEME_DIR = (
    Path(__file__).resolve().parent.parent
    / "shared/src/commonMain/kotlin/com/adll/de_general/core/ui/theme"
)

# (label, expected tokens, Kotlin file, scheme builder the vals must be wired into)
PALETTES = [
    ("light", STITCH_NAMED_COLORS, THEME_DIR / "Color.kt", "lightColorScheme"),
    ("dark", STITCH_DARK_NAMED_COLORS, THEME_DIR / "ColorDark.kt", "darkColorScheme"),
]

# Both files name the error role ErrorColor so it does not clash with the Color type.
KOTLIN_ALIASES = {"error": "ErrorColor"}


def pascal(snake: str) -> str:
    return "".join(part.capitalize() for part in snake.split("_"))


def check(label: str, expected_tokens: dict, kotlin_file: Path, builder: str) -> bool:
    print(f"== {label}: {kotlin_file.name}")
    if not kotlin_file.exists():
        print(f"FAIL  {kotlin_file.name} not found at {kotlin_file}")
        return False

    source = kotlin_file.read_text()
    declared = {
        name: "#" + value.lower()
        for name, value in re.findall(
            r"private val (\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)", source
        )
    }

    missing, mismatched = [], []
    for token, expected in expected_tokens.items():
        name = KOTLIN_ALIASES.get(token, pascal(token))
        if name not in declared:
            missing.append(f"{token} (expected Kotlin val `{name}`)")
        elif declared[name] != expected:
            mismatched.append(
                f"{token}: Stitch {expected} vs {kotlin_file.name} {declared[name]}"
            )

    # Wiring is only looked for inside the builder call, so a val "wired" into some other
    # expression does not pass by accident.
    call = re.search(rf"{builder}\((.*?)^\)", source, re.S | re.M)
    wiring = call.group(1) if call else ""
    unwired = [
        name
        for name in declared
        if not re.search(rf"=\s*{re.escape(name)},\s*$", wiring, re.M)
    ]

    print(f"Stitch tokens: {len(expected_tokens)}    Kotlin vals: {len(declared)}")

    ok = True
    for problem_label, problems in (
        (f"missing from {kotlin_file.name}", missing),
        ("hex mismatches", mismatched),
        (f"declared but not wired into {builder}()", unwired),
    ):
        if problems:
            ok = False
            print(f"\nFAIL  {problem_label}:")
            for problem in problems:
                print(f"        {problem}")

    if ok:
        print(f"OK    {label} palette matches the Stitch design system")
    return ok


def main() -> int:
    results = [check(*palette) for palette in PALETTES]
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main())
