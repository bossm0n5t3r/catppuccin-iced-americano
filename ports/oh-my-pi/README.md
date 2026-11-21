# Iced Americano for oh-my-pi

An unofficial Catppuccin Latte derivative for the
[oh-my-pi](https://github.com/can1357/oh-my-pi) coding agent (`omp`), generated from
[`palette/iced-americano.json`](../../palette/iced-americano.json).

## Install

oh-my-pi loads custom themes from `~/.omp/agent/themes/<name>.json`
(`$PI_CODING_AGENT_DIR/themes` when that variable is set).

### From a release (no checkout)

```sh
themes="${PI_CODING_AGENT_DIR:-$HOME/.omp/agent}/themes"
mkdir -p "$themes"
curl -fsSL -o "$themes/iced-americano.json.tmp" \
  https://github.com/bossm0n5t3r/catppuccin-iced-americano/releases/latest/download/iced-americano.json &&
  mv -f "$themes/iced-americano.json.tmp" "$themes/iced-americano.json"
omp config set theme.light iced-americano
```

Re-run the `curl` command to update. A failed download keeps the installed theme.

### From a checkout

From the repository root (Bash, macOS/Linux; no JDK or Gradle required; `omp` must be
on `PATH`):

```sh
./ports/oh-my-pi/install.sh
```

The installer links this checkout's theme into
`${PI_CODING_AGENT_DIR:-$HOME/.omp/agent}/themes/iced-americano.json` and runs
`omp config set theme.light iced-americano`. Keep the checkout in place.

`theme.light` applies when oh-my-pi detects a light terminal background. Add `--dark` to
also set `theme.dark` and use the theme regardless of detection. To use another agent
directory:

```sh
./ports/oh-my-pi/install.sh --agent-dir /path/to/agent
```

### Uninstall

Run `omp config reset theme.light` (and `theme.dark` if set), then remove
`~/.omp/agent/themes/iced-americano.json`.

## Regenerate

After changing the shared palette or generator:

```sh
./gradlew :tooling:generateOhMyPi
./gradlew :tooling:checkOhMyPi
```

## Mapping

The theme keeps `light-catppuccin`'s role names and surfaces but expresses Iced Americano's
blue-black identity, in line with the [JetBrains port](../jetbrains/README.md):

- **Typography:** message text, user and custom messages and variables use Text (`#25334D`)
  instead of the terminal's default foreground. Secondary text uses Subtext1 or Subtext0 (comments, quotes, thinking,
  muted text) rather than the fainter Overlay tokens; tool
  output and diff context use Subtext1, which stays at 4.5:1 on the Mantle tool background.
- **Accent:** Blue replaces Mauve as the accent, for headings, links, list bullets, tool
  titles and functions. Mauve keeps keywords and labels, and the dark-roast Rosewater
  colors inline code. Borders are neutral Surface tokens instead of Lavender.
- **Syntax:** follows the JetBrains editor scheme: keywords Mauve, functions and numbers
  Blue, strings Green, comments Subtext0 and types the same darkened Yellow (`#975C02`).
- **Backgrounds:** selection and pending tools tint Base with Blue, failed tools with Red.
- **Derived colors:** chromatic tokens that fall below 4.5:1 (Yellow, Peach, Sky, Pink,
  Sapphire) keep their OKLCH hue and are darkened to 4.5:1 on Crust, the status line
  background, so they also pass on Base and Mantle. Blue is darkened slightly to reach 4.5:1
  on the selection. They are added to `vars` with an `Ink` suffix; the palette is unchanged.
- **ANSI:** `terminal.ansi` uses the same 16 colors as the [Ghostty port](../ghostty/README.md),
  including its yellow, pink and teal contrast mixes.

Tests check that every readable role reaches 4.5:1 on Base (status line roles on Crust, tool
roles also on Mantle). Borders, separators, punctuation, `dim` and the off/minimal thinking
levels are intentionally faint. See the [project usage limits](../../README.md#usage-limits).

oh-my-pi's `colorBlindMode` rotates `toolDiffAdded` by 60°, which turns this theme's
blue-teal Green into a navy close to Text. Iced Americano's Red and Green are already chosen
for protanopia and deuteranopia, so turn it off with `omp config set colorBlindMode false`.
