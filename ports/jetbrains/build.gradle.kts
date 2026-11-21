plugins {
    java
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.ktfmt)
}

group = "io.github.bossm0n5t3r.icedamericano"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies { intellijPlatform { intellijIdea(libs.versions.intellij.idea.get()) } }

java { toolchain { languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get().toInt()) } }

intellijPlatform {
    // Resource-only theme: no compiled classes or searchable settings to process.
    buildSearchableOptions = false
    instrumentCode = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
            untilBuild = provider { null }
        }
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing { token = providers.environmentVariable("PUBLISH_TOKEN") }
    pluginVerification { ides { recommended() } }
}

tasks.buildPlugin { archiveBaseName.set("catppuccin-iced-americano-jetbrains") }

// Plugin icon is generated from the site favicon so the two never drift apart.
val faviconSvg = rootProject.layout.projectDirectory.file("docs/favicon.svg")
val pluginIconDir = layout.buildDirectory.dir("generated/pluginIcon")

val generatePluginIcon =
    tasks.register("generatePluginIcon") {
        description = "Generates META-INF/pluginIcon.svg from docs/favicon.svg."
        val source = faviconSvg.asFile
        val output = pluginIconDir.map { it.file("META-INF/pluginIcon.svg").asFile }
        inputs.file(source)
        outputs.dir(pluginIconDir)
        doLast {
            val svg = source.readText()
            val rootTag = Regex("""<svg\b[^>]*>""").find(svg)
            require(rootTag != null && "viewBox=\"0 0 32 32\"" in rootTag.value) {
                "docs/favicon.svg must keep a 32x32 viewBox on its root <svg> tag"
            }
            // 40x40 logo size with a 2px margin around the 32x32 artwork.
            val icon =
                svg.replaceRange(
                    rootTag.range,
                    """<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="-2 -2 36 36">""",
                )
            output.get().apply {
                parentFile.mkdirs()
                writeText(icon)
            }
        }
    }

sourceSets.main { resources.srcDir(generatePluginIcon) }

ktfmt { kotlinLangStyle() }
