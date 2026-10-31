# Catppuccin Iced Americano

Unofficial Catppuccin Latte derivative, **v0.1.0 / experimental**.
Light Latte surfaces, blue-black typography and bright blue accents.

The source of truth is [`palette/iced-americano.json`](palette/iced-americano.json).
Blue is `#1D62ED`; Mauve is `#7E0B97`; secondary text is `#454D65`.

## Ports

- **Ghostty:** [installation, mapping and limitations](ports/ghostty/README.md).
- **Zed / JetBrains:** not implemented.

## Usage limits

- Contrast validation checks required normal-text pairs; other background
  combinations are diagnostic only. Passing does not certify the whole theme.
- Blue, Subtext0 and Red fall below 4.5:1 on Mantle and Crust; Green falls below
  4.5:1 on Crust. Use Text or Subtext1 for normal text on those surfaces.
- Yellow, Pink and Teal ANSI text fall below 4.5:1 even on Base. Use readable
  text with explicit labels and icons rather than color alone.
- `semantics.cue` describes recommended cues that applications must provide;
  the palette and Ghostty theme do not implement them automatically.
- CVD simulations are model estimates, not accessibility certification or a
  substitute for user evaluation. Check actual roles and pairings, including
  Mauve–Green, in each application.

## Maintenance

Requires Python 3.15 and [uv](https://docs.astral.sh/uv/).
There are no runtime dependencies; Ruff is the only development dependency.
Ghostty needs only the generated `ports/ghostty/iced-americano` theme.

Run from the repository root:

```sh
# Regenerate from the palette; never edit the theme by hand.
uv run python -m scripts.generate_ghostty

# Check the generated theme and calculate contrast and CVD estimates.
uv run python -m scripts.generate_ghostty --check
uv run python -m scripts.validate palette/iced-americano.json
uv run python -m scripts.cvd

# Run tests and Python style checks.
uv run python -m unittest discover -s tests -v
uv run ruff check .
uv run ruff format --check .
```

Generation does not install anything or change user settings.

[Third-party notices](docs/THIRD_PARTY_NOTICES.md).
