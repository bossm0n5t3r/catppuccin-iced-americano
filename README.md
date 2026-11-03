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

**[Explore the palette website](https://bossm0n5t3r.github.io/catppuccin-iced-americano/)**
CSS color chips for all 26 tokens, a side-by-side Catppuccin Latte comparison
with labeled, highlighted modified rows, direct Ghostty and JetBrains installation
guides, and usage limits. No images, JavaScript, external fonts or runtime dependencies.

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

## Commands

| Command                                                                    | Description                                                                                                                              |
|----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `./gradlew ktfmtFormat`                                                    | Format Kotlin and Gradle Kotlin DSL using KotlinLang style. Run before checks.                                                           |
| `./gradlew ktfmtCheck :tooling:build`                                      | Check formatting, build tooling, run tests, validate the palette, and check generated Ghostty/site output without modifying it.          |
| `./gradlew :tooling:validatePalette`                                       | Validate tokens, HEX values, and required contrast pairs against the unrounded 4.5:1 threshold. Diagnostic pairs do not fail validation. |
| `./gradlew :tooling:reportCvd`                                             | Print CVD simulations and color-distance diagnostics; not accessibility certification.                                                   |
| `./gradlew :tooling:generateGhostty`                                       | Regenerate the Ghostty theme from the shared palette.                                                                                    |
| `./gradlew :tooling:checkGhostty`                                          | Check the generated Ghostty theme without modifying it.                                                                                  |
| `./gradlew :tooling:generateSite`                                          | Update the palette website from the shared palette.                                                                                      |
| `./gradlew :tooling:checkSite`                                             | Check the palette website without modifying it.                                                                                          |
| `./ports/ghostty/install.sh`                                               | Install the Ghostty theme on macOS/Linux using Bash; no Gradle or JVM required.                                                          |
| `./gradlew :tooling:validatePalette -Ppalette=palette/iced-americano.json` | Override the palette path; relative paths resolve from the repository root.                                                              |
| `./gradlew :tooling:run --args="validate palette/iced-americano.json"`     | Validate a palette through the CLI.                                                                                                      |
| `./gradlew :tooling:run --args="cvd"`                                      | Print the CVD report through the CLI.                                                                                                    |

## License

Licensed under the [MIT License](LICENSE).
See [third-party notices](docs/THIRD_PARTY_NOTICES.md) for Catppuccin attribution.
