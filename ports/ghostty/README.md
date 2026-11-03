# Iced Americano for Ghostty

An unofficial Catppuccin Latte derivative, generated from
[`palette/iced-americano.json`](../../palette/iced-americano.json).

## Install

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

Reload with **Cmd+Shift+,** on macOS or **Ctrl+Shift+,** on Linux (default bindings).
To uninstall, remove the `theme = iced-americano` line and the theme symlink.

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
  except cursor text uses `text` for contrast.
- [Project usage limits](../../README.md#usage-limits)
- [Ghostty theme documentation](https://ghostty.org/docs/features/theme)
