# Iced Americano for Ghostty

Unofficial Catppuccin Latte derivative, v0.1.0 / experimental.
Generated from `palette/iced-americano.json`; no CVD accessibility certification.

## Install

From the repository root, install the theme and select it (Bash, macOS/Linux;
no JDK, Gradle or Python required):

```sh
./ports/ghostty/install.sh
```

The installer creates a symlink at
`${XDG_CONFIG_HOME:-$HOME/.config}/ghostty/themes/iced-americano` pointing to this
checkout, then sets `theme = iced-americano` without replacing other settings.
Regenerated colors are available through the link; keep the checkout in place.
Rerunning replaces an existing theme file or symlink, including broken links,
and collapses active `theme` settings into one. It refuses to replace a directory.

Config discovery prefers existing XDG `config.ghostty`, then legacy `config`,
then their macOS equivalents under
`~/Library/Application Support/com.mitchellh.ghostty/`.
If none exists, it creates XDG `config.ghostty`. To choose a specific file:

```sh
./ports/ghostty/install.sh \
  --config ~/code/dotfiles/ghostty/.config/ghostty/config
```

Config symlinks are preserved; their target file is updated.
Explicit user color settings still override the theme.
When a config update is needed, CRLF/CR line endings are normalized to LF,
matching the previous Python installer. An already-correct config is not rewritten.
Normal Gradle build/check tasks never install into user settings; installer tests
use isolated temporary directories.

Reload: **Cmd+Shift+,** on macOS or **Ctrl+Shift+,** on Linux (default bindings),
or your configured reload shortcut.
Uninstall by removing the `theme` line and the installed symlink.

## Generate and verify

From the repository root:

```sh
./gradlew :tooling:generateGhostty
./gradlew :tooling:checkGhostty
ghostty +validate-config --config-file="$PWD/ports/ghostty/iced-americano"
```

On macOS the executable may be `/Applications/Ghostty.app/Contents/MacOS/ghostty`.
Do not add `--config-default-files=false` to `+validate-config`.
Override generation paths with `-Ppalette=PATH` and `-Poutput=PATH`.
The Kotlin CLI accepts `generate-ghostty --palette PATH --output PATH --check`
through `./gradlew :tooling:run --args="generate-ghostty ..."` as well.
Freshness checks never write files or create directories; missing/stale output fails.
The legacy Python attribution comment in the generated file is temporarily retained
to prove byte-for-byte migration parity; Python itself is not used for generation.
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
