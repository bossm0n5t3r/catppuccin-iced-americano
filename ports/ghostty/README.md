# Iced Americano for Ghostty

An unofficial Catppuccin Latte derivative, generated from
[`palette/iced-americano.json`](../../palette/iced-americano.json).

## Install

### From a release (no checkout)

Download the theme from the latest release into Ghostty's themes directory:

```sh
themes="${XDG_CONFIG_HOME:-$HOME/.config}/ghostty/themes"
mkdir -p "$themes"
curl -fsSL -o "$themes/iced-americano.tmp" \
  https://github.com/bossm0n5t3r/catppuccin-iced-americano/releases/latest/download/iced-americano &&
  mv -f "$themes/iced-americano.tmp" "$themes/iced-americano"
```

Then add `theme = iced-americano` to your Ghostty config and reload. Re-run the commands to
update. A failed download keeps the installed theme, and `mv` replaces a symlink left by the
checkout installer instead of writing through it.

### From a checkout

From the repository root (Bash, macOS/Linux; no JDK or Gradle required):

```sh
./ports/ghostty/install.sh
```

The installer links this checkout's theme into
`${XDG_CONFIG_HOME:-$HOME/.config}/ghostty/themes/iced-americano`
and sets `theme = iced-americano` in your Ghostty config. Keep the checkout in place.
Explicit color settings in your config override the theme.

To use a specific config file:

```sh
./ports/ghostty/install.sh --config /path/to/ghostty/config
```

### Reload and uninstall

Reload with **Cmd+Shift+,** on macOS or **Ctrl+Shift+,** on Linux (default bindings).
To uninstall, remove the `theme = iced-americano` line and
`${XDG_CONFIG_HOME:-$HOME/.config}/ghostty/themes/iced-americano` (a file for release
installs, a symlink for checkout installs).

## Regenerate

After changing the shared palette or generator:

```sh
./gradlew :tooling:generateGhostty
./gradlew :tooling:checkGhostty
```

Reload Ghostty to apply the regenerated theme.

## References

- Mapping follows the Latte branch of the official
  [Catppuccin Ghostty template](https://github.com/catppuccin/ghostty/blob/main/ghostty.tera),
  except cursor text uses `text`, and ANSI yellow, pink and teal (3/5/6 and
  11/13/14) are mixed with palette tokens for 4.5:1 contrast on `base`:
  yellow with 45% `text`, pink with 60% `mauve`, teal with 25% `mauve`.
  The shared palette keeps the Latte values. These adjustments target ANSI
  foreground text on `base`; chromatic ANSI backgrounds (for example prompt
  segments) fall below 4.5:1 with the default or ANSI black foreground and may
  need explicitly chosen foreground colors.
- [Project usage limits](../../README.md#usage-limits)
- [Ghostty theme documentation](https://ghostty.org/docs/features/theme)
