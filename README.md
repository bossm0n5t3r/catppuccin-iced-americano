# Catppuccin Iced Americano

Unofficial Catppuccin Latte derivative, **v0.1.0 / experimental**.

> A developer theme built on Catppuccin Latte's soft, cool light palette, with
> blue-black typography, vivid blue accents and a clearer text hierarchy, designed
> with protanopia and deuteranopia color distinctions in mind.

*The quiet confidence of blue-black typography.*

Iced Americano is a reinterpretation of Catppuccin Latte, not a generic white IDE
theme. Accessibility means useful distinctions, readable roles and non-color cues;
it does not mean maximizing every contrast ratio or simulated color distance.

The source of truth is [`palette/iced-americano.json`](palette/iced-americano.json).

## Identity baseline and palette policy

| Token    | Catppuccin Latte | Iced Americano |
|----------|------------------|----------------|
| Text     | `#4C4F69`        | `#25334D`      |
| Blue     | `#1E66F5`        | `#1D62ED`      |
| Mauve    | `#8839EF`        | `#7E0B97`      |
| Green    | `#40A02B`        | `#006F80`      |
| Subtext1 | `#5C5F77`        | `#454D65`      |
| Subtext0 | `#6C6F85`        | `#686B81`      |
| Base     | `#EFF1F5`        | `#EFF1F5`      |
| Mantle   | `#E6E9EF`        | `#E6E9EF`      |
| Crust    | `#DCE0E8`        | `#DCE0E8`      |

Base, Mantle and Crust retain the Latte surfaces. Other palette colors are unchanged.
Exact values and the original Latte colors are recorded in `colors` and
`changes_from_latte` in the [shared palette](palette/iced-americano.json).

**Pause discretionary palette tuning at this baseline.** The next development focus
is expressing this fixed palette consistently and readably in Ghostty, JetBrains and
the future Zed port, not optimizing the colors themselves.

The central palette defines the project's design identity. Adapting controls, syntax
roles and derived highlights to an application is a port's responsibility. **Do not change the central palette to
resemble IntelliJ's built-in Light theme.**

Any future palette revision requires an explicit design decision against all three:

1. **Identity:** retain Catppuccin Latte's calm, slightly cool surfaces and Iced
   Americano's blue-black typography, vivid Blue and stronger text hierarchy.
2. **Accessibility:** improve meaningful readability or semantic distinctions;
   review regressions in other important pairs, simulations and non-color cues.
   Neither a single contrast result nor a single simulated distance is sufficient.
3. **Consistency:** keep the palette usable across ports, update all implemented
   consumers together and assess implications for future ports.

## Ports

- **Ghostty:** [installation, mapping and limitations](ports/ghostty/README.md).
- **JetBrains:** [theme plugin structure, build and installation](ports/jetbrains/README.md).
- **Zed:** not implemented.

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
  Mauve–Green and Red–Green, in each application. Review tradeoffs across pairs and
  retain explicit labels/icons for status information; simulations are not user validation.

## Maintenance

Kotlin tooling uses the JDK configured in `gradle/libs.versions.toml` and the
Gradle Wrapper. Ghostty installation uses Bash (macOS/Linux) and standard system
utilities, without launching Gradle or a JVM. Ghostty itself needs only the generated theme file.

### Kotlin tooling

The Gradle tooling module reads the shared palette and provides Kotlin contrast
validation, CVD reports and Ghostty theme generation. JSON parsing uses kotlinx.serialization.
JDK toolchain, Kotlin plugin, JSON library and ktfmt plugin versions are managed in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml). The JDK entry controls
the tooling compilation toolchain, not the JVM that launches Gradle.
The Foojay settings plugin version remains in `settings.gradle.kts`.

ktfmt uses KotlinLang style (four-space indentation) for Kotlin sources and
Gradle Kotlin DSL scripts in the root project, tooling and JetBrains modules.
Root script formatting matches only top-level `.kts` files before traversal, excluding
generated IDE sandbox files. Module source checks still run normally.

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

Licensed under the [MIT License](LICENSE).
See [third-party notices](docs/THIRD_PARTY_NOTICES.md) for Catppuccin attribution.
