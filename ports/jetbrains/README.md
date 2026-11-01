# JetBrains — Catppuccin Iced Americano

A resource-only theme plugin based on New UI Light with Light Header, using the
[shared Iced Americano palette](../../palette/iced-americano.json).

## Build and preview

Run from the repository root with JDK 25:

```sh
./gradlew :ports:jetbrains:buildPlugin :ports:jetbrains:verifyPluginStructure
./gradlew :ports:jetbrains:runIde
```

Targets IntelliJ IDEA 2026.2.3, minimum platform build `262`. The first build downloads
the IDE SDK. In the sandbox, select **Settings -> Appearance & Behavior -> Appearance ->
Theme -> Catppuccin Iced Americano**.

## Install

Use **Settings -> Plugins -> gear -> Install Plugin from Disk…** and select
`ports/jetbrains/build/distributions/jetbrains-0.1.0.zip`. Restart if prompted,
then select the theme under Appearance.

## Maintenance

- `src/main/resources/META-INF/plugin.xml`: plugin metadata and New UI registration.
- `src/main/resources/themes/IcedAmericano.theme.json`: UI colors and parent theme.
- `src/main/resources/themes/IcedAmericano.xml`: editor colors and syntax styles.
- `build.gradle.kts`: plugin version, packaging, signing and publishing.
- `../../gradle/libs.versions.toml`: target IDE, Gradle plugin and JDK versions.

Colors are hand-maintained; the Ghostty generator does not generate this port.
Keep UI aliases, editor colors and Base-derived highlight tints in sync with the
shared palette. Preserve Light's font/effect styles when changing color mappings.
See the root README for [palette policy](../../README.md#identity-baseline-and-palette-policy)
and [accessibility limits](../../README.md#usage-limits). Preview changes in the IDE;
structural validation does not establish visual correctness.

## Verify and publish

```sh
./gradlew :ports:jetbrains:verifyPlugin
./gradlew :ports:jetbrains:signPlugin
./gradlew :ports:jetbrains:publishPlugin
```

Signing requires `CERTIFICATE_CHAIN`, `PRIVATE_KEY` and `PRIVATE_KEY_PASSWORD`.
Publishing requires `PUBLISH_TOKEN`. Supply credentials through environment variables,
not committed files. `verifyPlugin` downloads additional IDEs for compatibility checks.

References: [New UI Light Header](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-resources/src/themes/expUI/expUI_light_with_light_header.theme.json)
· [Light editor scheme](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-resources/src/themes/Light.xml)
