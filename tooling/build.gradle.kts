plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktfmt)
    application
}

repositories { mavenCentral() }

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java { toolchain { languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get().toInt()) } }

application { mainClass = "icedamericano.tooling.AppKt" }

tasks.named<Test>("test") { useJUnitPlatform() }

ktfmt { kotlinLangStyle() }
