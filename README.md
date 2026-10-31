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

Kotlin tooling uses the JDK configured in `gradle/libs.versions.toml` and the
Gradle Wrapper. Ghostty installation uses Bash (macOS/Linux) and needs no JDK,
Gradle or Python. Ghostty itself needs only the generated theme file.

### Python migration reference

Python 3.15 and [uv](https://docs.astral.sh/uv/) remain temporarily for reference
comparisons and the existing tests. These tools have no runtime dependencies;
Ruff is their only development dependency.

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

### Kotlin tooling

The Gradle tooling module reads the shared palette and provides Kotlin contrast
validation, CVD reports and Ghostty theme generation. JSON parsing uses kotlinx.serialization.
JDK toolchain, Kotlin plugin, JSON library and ktfmt plugin versions are managed in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml). The JDK entry controls
the tooling compilation toolchain, not the JVM that launches Gradle.
The Foojay settings plugin version remains in `settings.gradle.kts`.

ktfmt uses KotlinLang style (four-space indentation) for Kotlin sources and
Gradle Kotlin DSL scripts in the root project and tooling module.

```sh
# Format first, then check in a separate invocation.
./gradlew ktfmtFormat
./gradlew ktfmtCheck :tooling:build

# Validate the palette and print the CVD report.
./gradlew :tooling:validatePalette
./gradlew :tooling:reportCvd

# Generate explicitly; check is read-only and also runs as part of build.
./gradlew :tooling:generateGhostty
./gradlew :tooling:checkGhostty

# Install explicitly, without launching Gradle or a JVM.
./ports/ghostty/install.sh

# Override the palette path; relative paths are resolved from the repository root.
./gradlew :tooling:validatePalette -Ppalette=palette/iced-americano.json

# Run the same commands through the CLI.
./gradlew :tooling:run --args="validate palette/iced-americano.json"
./gradlew :tooling:run --args="cvd"
```

`:tooling:check` (and therefore `:tooling:build`) runs palette validation,
Ghostty freshness checks and tests. It never regenerates stale/missing themes
or installs into user settings. Installation is performed by explicitly running
`ports/ghostty/install.sh`; see the [Ghostty instructions](ports/ghostty/README.md).
Validation exit codes are 0 for success, 1 for palette token/HEX or required-contrast
failures, and 2 for invalid usage, malformed schema/JSON or unreadable files.
Generation/check exits are 0 for success, 1 for missing/stale output in check mode,
and 2 for invalid input or I/O errors.
Contrast uses the unrounded 4.5:1 threshold; diagnostic pairs do not fail validation.
Python remains temporarily as a migration reference until the migration is complete.

Licensed under the [MIT License](LICENSE).
See [third-party notices](docs/THIRD_PARTY_NOTICES.md) for Catppuccin attribution.
