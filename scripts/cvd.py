"""Machado dichromacy simulations and Euclidean OKLab distances.

Matrices are the authors' severity 1.0 table (dichromacy):
https://www.inf.ufrgs.br/~oliveira/pubs_files/CVD_Simulation/CVD_Simulation.html
The linear-sRGB-to-OKLab formula is Björn Ottosson's published conversion:
https://bottosson.github.io/posts/oklab/#converting-from-linear-srgb-to-oklab
Decode sRGB, apply the matrix in linear light, clip each channel to [0, 1],
then convert to OKLab. Clipping is a reproducible choice, not gamut mapping.
Distances are model estimates, not an accessibility certification.

Run the palette report: uv run python -m scripts.cvd
"""

import json
import math
from pathlib import Path

from scripts.validate import contrast

MATRICES = {
    "protanopia": (
        (0.152286, 1.052583, -0.204868),
        (0.114503, 0.786281, 0.099216),
        (-0.003882, -0.048116, 1.051998),
    ),
    "deuteranopia": (
        (0.367322, 0.860646, -0.227968),
        (0.280085, 0.672501, 0.047413),
        (-0.011820, 0.042940, 0.968881),
    ),
}


def srgb_to_linear(channel: float) -> float:
    """Decode one normalized sRGB channel to linear light."""
    return channel / 12.92 if channel <= 0.04045 else ((channel + 0.055) / 1.055) ** 2.4


def hex_to_linear(color: str) -> tuple[float, float, float]:
    """Decode a six-digit #RRGGBB color."""
    if (
        len(color) != 7
        or not color.startswith("#")
        or any(c not in "0123456789abcdefABCDEF" for c in color[1:])
    ):
        raise ValueError("Expected #RRGGBB")
    channels = tuple(int(color[i : i + 2], 16) for i in (1, 3, 5))
    return tuple(srgb_to_linear(c / 255) for c in channels)


def simulate(
    rgb: tuple[float, float, float], vision: str
) -> tuple[float, float, float]:
    """Apply a severity-1 matrix to linear sRGB and clip to the sRGB gamut."""
    if vision == "normal":
        return rgb
    matrix = MATRICES[vision]
    return tuple(
        max(0.0, min(1.0, sum(x * y for x, y in zip(row, rgb)))) for row in matrix
    )


def linear_to_oklab(rgb: tuple[float, float, float]) -> tuple[float, float, float]:
    """Convert in-gamut linear sRGB to OKLab on the L=0..1 scale."""
    r, g, b = rgb
    l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b) ** (1 / 3)
    m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b) ** (1 / 3)
    s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b) ** (1 / 3)
    return (
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
    )


def distance(first: str, second: str, vision: str = "normal") -> float:
    """Euclidean OKLab distance between two hex colors under a vision model."""
    a = linear_to_oklab(simulate(hex_to_linear(first), vision))
    b = linear_to_oklab(simulate(hex_to_linear(second), vision))
    return math.dist(a, b)


def _print_pair(label: str, first: str, second: str) -> None:
    values = (
        distance(first, second, vision)
        for vision in ("normal", "protanopia", "deuteranopia")
    )
    print(f"{label:23} {first} {second}  " + "  ".join(f"{x:.5f}" for x in values))


def main() -> None:
    path = Path(__file__).resolve().parents[1] / "palette/iced-americano.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    colors = data["colors"]
    print(f"{data['name']} {data['version']} | {data['status']}")
    print(
        "Euclidean OKLab distances (L 0-1 scale); Machado severity 1, "
        "linear-sRGB channel clipping."
    )
    print("Model estimates only; no accessibility threshold or certification.")
    print(
        f"Fixed current partners: mauve {colors['mauve']}, "
        f"green {colors['green']}, red {colors['red']}."
    )
    print(
        f"{'Pair / variant':23} {'first':7} {'second':7}  "
        f"{'normal':>7}  {'protan':>7}  {'deutan':>7}"
    )
    for label, blue in (
        ("Latte", data["changes_from_latte"]["blue"]["from"]),
        ("previous", "#193B83"),
        ("current", colors["blue"]),
    ):
        for partner in ("mauve", "green"):
            _print_pair(f"blue-{partner} / {label}", blue, colors[partner])
    _print_pair("red-green / current", colors["red"], colors["green"])

    print("\nMauve change: fixed current Blue, Green and Red")
    for label, mauve in (
        ("Latte", data["changes_from_latte"]["mauve"]["from"]),
        ("current", colors["mauve"]),
    ):
        for partner in ("blue", "green", "red"):
            _print_pair(f"mauve-{partner} / {label}", mauve, colors[partner])
        ratios = "  ".join(
            f"{bg} {contrast(mauve, colors[bg]):.2f}:1"
            for bg in ("base", "mantle", "crust")
        )
        print(f"Mauve {mauve} WCAG contrast (normal vision): {ratios}")


if __name__ == "__main__":
    main()
