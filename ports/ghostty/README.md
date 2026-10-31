# Iced Americano for Ghostty

Unofficial Catppuccin Latte derivative, v0.1.0 / experimental.
Generated from `palette/iced-americano.json`; no CVD accessibility certification.

## Install

Add an absolute path to your existing Ghostty configuration:

```ini
theme = /ABSOLUTE/REPO/ports/ghostty/iced-americano
```

Alternatively, copy the theme into Ghostty's themes directory:

```sh
mkdir -p "${XDG_CONFIG_HOME:-$HOME/.config}/ghostty/themes"
cp ports/ghostty/iced-americano "${XDG_CONFIG_HOME:-$HOME/.config}/ghostty/themes/iced-americano"
```

Then set `theme = iced-americano`. Recopy after regenerating.
Configuration is normally `${XDG_CONFIG_HOME:-$HOME/.config}/ghostty/config.ghostty`;
macOS also supports `~/Library/Application Support/com.mitchellh.ghostty/config.ghostty`.
Merge the theme line; do not replace existing configuration. Explicit user color
settings override the theme. Nothing is installed or configured automatically.

Reload: **Cmd+Shift+,** on macOS or **Ctrl+Shift+,** on Linux (default bindings).
Uninstall by removing your theme line and any installed copy.

## Generate and verify

From the repository root:

```sh
uv run --locked python -m scripts.generate_ghostty
uv run --locked python -m scripts.generate_ghostty --check
ghostty +validate-config --config-file="$PWD/ports/ghostty/iced-americano"
```

On macOS the executable may be `/Applications/Ghostty.app/Contents/MacOS/ghostty`.
Do not add `--config-default-files=false` to `+validate-config`.
The generator accepts `--palette PATH`, `--output PATH`, and `--check`.
Check exits: 0 synchronized, 1 missing/stale, 2 invalid input or I/O error.

## Mapping and limits

Background/foreground use base/text. Cursor uses blue/base; selection uses
surface1/text. ANSI slots:

| Slots  | Token    |
|--------|----------|
| 0 / 15 | text     |
| 1 / 9  | red      |
| 2 / 10 | green    |
| 3 / 11 | yellow   |
| 4 / 12 | blue     |
| 5 / 13 | pink     |
| 6 / 14 | teal     |
| 7      | subtext1 |
| 8      | subtext0 |

Bright pairs deliberately reuse colors. Yellow, pink and teal do not meet AA
normal-text contrast on Base. Use explicit status labels and legible default text;
color alone must not carry meaning. The theme cannot control arbitrary application
styles, truecolor output or underline. Contrast does not establish CVD usability.

Mauve is not mapped by this port; ANSI magenta uses pink, not mauve.
Applications choose syntax roles and may emit truecolor independently.
See the [usage limits](../../README.md#usage-limits) for palette validation scope.

[Official theme documentation](https://ghostty.org/docs/features/theme) ·
[Configuration reference](https://ghostty.org/docs/config/reference)
