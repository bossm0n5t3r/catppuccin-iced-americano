# JetBrains — Catppuccin Iced Americano

A light theme plugin based on [Catppuccin for JetBrains](https://github.com/catppuccin/jetbrains),
using the [Iced Americano palette](../../palette/iced-americano.json).

## Build and preview

Run from the repository root with JDK 25:

```sh
./gradlew :tooling:test :ports:jetbrains:buildPlugin :ports:jetbrains:verifyPluginStructure
./gradlew :ports:jetbrains:runIde
```

The first build downloads the IDE SDK. [GitHub Actions](../../.github/workflows/jetbrains.yaml)
runs the tests, validates the plugin and uploads the ZIP as a build artifact;
the [release workflow](../../.github/workflows/release.yaml) publishes it for `v*` tags.

## Maintenance

The theme files are edited by hand. Every color must be a palette color or a derived color
registered in [`DerivedColors.kt`](../../tooling/src/main/kotlin/icedamericano/tooling/jetbrains/DerivedColors.kt),
which records the Catppuccin for JetBrains formula (Blue accent). After changing the palette or a
theme color, run `./gradlew :tooling:checkJetBrains` and update any reported values.

## Install

Requires a JetBrains IDE 2026.2 or later. Each `v*` tag publishes the plugin ZIP to GitHub Releases.

1. Download `catppuccin-iced-americano-jetbrains-<version>.zip` from the
   [latest release](https://github.com/bossm0n5t3r/catppuccin-iced-americano/releases/latest),
   or build it into `ports/jetbrains/build/distributions/`.
2. Open **Settings -> Plugins -> gear -> Install Plugin from Disk…**, select the ZIP and restart if prompted.

### Choose the theme

1. Under **Appearance & Behavior -> Appearance**, choose **Iced Americano** or **Islands Iced Americano**, matching your
   IDE's UI.
2. Under **Editor -> Color Scheme**, choose **Iced Americano**.

CVD modes use the same palette and styles; legacy built-in Light overrides are omitted.
See the shared [accessibility limits](../../README.md#usage-limits).
