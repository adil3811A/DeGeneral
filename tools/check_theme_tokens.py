#!/usr/bin/env python3
"""Check that Color.kt still matches the Mindful Scribe palette from Stitch.

Color.kt is a hand transcription of 47 hexes out of the Stitch design system
(project 13240507270798585972, design system "Mindful Scribe"). That is exactly the
kind of thing that rots silently, so this fails loudly on:

  * a token in the design system that Color.kt does not define
  * a hex that no longer matches
  * a token that is defined but never wired into lightColorScheme(...)

The expected values below are the `theme.namedColors` map. When the design system
changes upstream, re-pull it and update this map in the same commit as Color.kt:

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

COLOR_KT = (
    Path(__file__).resolve().parent.parent
    / "shared/src/commonMain/kotlin/com/example/de_general/ui/theme/Color.kt"
)

# Color.kt names the error role ErrorColor so it does not clash with the Color type.
KOTLIN_ALIASES = {"error": "ErrorColor"}


def pascal(snake: str) -> str:
    return "".join(part.capitalize() for part in snake.split("_"))


def main() -> int:
    if not COLOR_KT.exists():
        print(f"FAIL  Color.kt not found at {COLOR_KT}")
        return 1

    source = COLOR_KT.read_text()
    declared = {
        name: "#" + value.lower()
        for name, value in re.findall(
            r"private val (\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)", source
        )
    }

    missing, mismatched = [], []
    for token, expected in STITCH_NAMED_COLORS.items():
        name = KOTLIN_ALIASES.get(token, pascal(token))
        if name not in declared:
            missing.append(f"{token} (expected Kotlin val `{name}`)")
        elif declared[name] != expected:
            mismatched.append(f"{token}: Stitch {expected} vs Color.kt {declared[name]}")

    unwired = [
        name
        for name in declared
        if not re.search(rf"=\s*{re.escape(name)},\s*$", source, re.M)
    ]

    print(f"Stitch tokens: {len(STITCH_NAMED_COLORS)}    Kotlin vals: {len(declared)}")

    ok = True
    for label, problems in (
        ("missing from Color.kt", missing),
        ("hex mismatches", mismatched),
        ("declared but not wired into lightColorScheme()", unwired),
    ):
        if problems:
            ok = False
            print(f"\nFAIL  {label}:")
            for problem in problems:
                print(f"        {problem}")

    if ok:
        print("OK    palette matches the Stitch design system")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
