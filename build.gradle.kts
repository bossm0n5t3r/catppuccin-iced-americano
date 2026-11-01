import com.ncorti.ktfmt.gradle.tasks.KtfmtCheckTask
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.intellij.platform) apply false
}

repositories { mavenCentral() }

ktfmt { kotlinLangStyle() }

// Restrict the file tree before traversal, not after visiting generated IDE caches.
val rootScripts = fileTree(projectDir) { include("*.kts") }

tasks.named<KtfmtCheckTask>("ktfmtCheckScripts") { setSource(rootScripts) }

tasks.named<KtfmtFormatTask>("ktfmtFormatScripts") { setSource(rootScripts) }
