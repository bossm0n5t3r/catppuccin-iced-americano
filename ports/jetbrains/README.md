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
runs the tests, validates the plugin and uploads the ZIP as a build artifact.

## Maintenance

The theme files are edited by hand. Every color must be a palette color or a derived color
registered in [`DerivedColors.kt`](../../tooling/src/main/kotlin/icedamericano/tooling/jetbrains/DerivedColors.kt),
which records the Catppuccin for JetBrains formula (Blue accent). After changing the palette or a
theme color, run `./gradlew :tooling:checkJetBrains` and update any reported values.

## Install

1. Open **Settings -> Plugins -> gear -> Install Plugin from Disk…**.
2. Select the ZIP from `ports/jetbrains/build/distributions/` and restart if prompted.
3. Under **Appearance & Behavior -> Appearance**, choose **Iced Americano** or **Islands Iced Americano**, matching your
   IDE's UI.
4. Under **Editor -> Color Scheme**, choose **Iced Americano**.

CVD modes use the same palette and styles; legacy built-in Light overrides are omitted.
See the shared [accessibility limits](../../README.md#usage-limits).
