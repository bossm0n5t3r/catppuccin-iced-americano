"""Experimental palette checks (WCAG 2.2 text contrast, not CVD certification).

Run: uv run python scripts/validate.py palette/iced-americano.json
"""

import json
import re
import sys
from pathlib import Path

EXPECTED = (
    "rosewater",
    "flamingo",
    "pink",
    "mauve",
    "red",
    "maroon",
    "peach",
    "yellow",
    "green",
    "teal",
    "sky",
    "sapphire",
    "blue",
    "lavender",
    "text",
    "subtext1",
    "subtext0",
    "overlay2",
    "overlay1",
    "overlay0",
    "surface2",
    "surface1",
    "surface0",
    "base",
    "mantle",
    "crust",
)

# Existing Base normal-text contract, plus text-bearing Ghostty roles.
# ANSI yellow/pink/teal are reported, not certified as normal text.
REQUIRED_PAIRS = (
    ("text", "base"),
    ("subtext1", "base"),
    ("subtext0", "base"),
    ("blue", "base"),
    ("red", "base"),
    ("green", "base"),
    ("mauve", "base"),
    ("text", "surface1"),  # Ghostty selection
    ("base", "blue"),  # Ghostty cursor text
)
DIAGNOSTIC_TEXT = (
    "text",
    "subtext1",
    "subtext0",
    "blue",
    "red",
    "green",
    "mauve",
    "yellow",
    "pink",
    "teal",
)


def luminance(color: str) -> float:
    channels = (int(color[i : i + 2], 16) / 255 for i in (1, 3, 5))
    linear = [
        c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in channels
    ]
    return sum(v * w for v, w in zip(linear, (0.2126, 0.7152, 0.0722)))


def contrast(a: str, b: str) -> float:
    light, dark = sorted((luminance(a), luminance(b)), reverse=True)
    return (light + 0.05) / (dark + 0.05)


def main(path: Path) -> int:
    data = json.loads(path.read_text(encoding="utf-8"))
    colors = data["colors"]
    if set(colors) != set(EXPECTED):
        print("Token mismatch:", sorted(set(EXPECTED) ^ set(colors)))
        return 1
    for key, value in colors.items():
        if (
            not isinstance(value, str)
            or re.fullmatch(r"#[0-9a-fA-F]{6}", value) is None
        ):
            print(f"Invalid HEX: {key}={value!r}")
            return 1

    print(f"{data['name']} {data['version']} | Base {colors['base']}")
    print("Required pairs: AA normal text, minimum 4.5:1")
    bad = []
    for foreground, background in REQUIRED_PAIRS:
        ratio = contrast(colors[foreground], colors[background])
        state = "PASS" if ratio >= 4.5 else "FAIL"
        print(f"{foreground:12} on {background:8} {ratio:5.2f}:1  {state}")
        if ratio < 4.5:
            bad.append((foreground, background))

    print(
        "Additional diagnostics: normal-text 4.5:1 reference, not additional failures"
    )
    for token in DIAGNOSTIC_TEXT:
        for background in ("base", "mantle", "crust"):
            if (token, background) in REQUIRED_PAIRS:
                continue
            ratio = contrast(colors[token], colors[background])
            state = "MEETS" if ratio >= 4.5 else "BELOW"
            print(f"{token:12} on {background:8} {ratio:5.2f}:1  {state}")

    warning = data["semantics"]["warning"]
    print(
        f"Warning: foreground={warning['foreground']}, accent={warning['accent']}; "
        "the accent is not suitable for small normal text."
    )
    print(
        "Reminder: contrast tests do not establish color-vision-deficiency accessibility."
    )
    return 1 if bad else 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python validate.py PATH_TO_PALETTE_JSON")
        sys.exit(2)
    sys.exit(main(Path(sys.argv[1])))
