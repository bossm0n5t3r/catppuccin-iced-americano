plugins {
    java
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.ktfmt)
}

group = "io.github.bossm0n5t3r.icedamericano"

version = "0.1.0"

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

ktfmt { kotlinLangStyle() }
