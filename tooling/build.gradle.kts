plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktfmt)
    application
}

repositories { mavenCentral() }

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java { toolchain { languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get().toInt()) } }

application { mainClass = "icedamericano.tooling.cli.MainKt" }

sourceSets.test { resources.srcDir(rootProject.layout.projectDirectory.dir("palette")) }

tasks.processTestResources {
    from(rootProject.layout.projectDirectory.file("ports/ghostty/install.sh"))
    from(rootProject.layout.projectDirectory.dir("ports/jetbrains/src/main/resources/themes")) {
        into("jetbrains")
    }
}

val palettePath = providers.gradleProperty("palette").orElse("palette/iced-americano.json")
val ghosttyOutputPath = providers.gradleProperty("output").orElse("ports/ghostty/iced-americano")
val releaseVersion = providers.gradleProperty("version")
val toolingMainClass = application.mainClass
val toolingClasspath = sourceSets.main.get().runtimeClasspath
val repositoryDirectory = rootProject.layout.projectDirectory

val validatePalette =
    tasks.register<JavaExec>("validatePalette") {
        group = "verification"
        description = "Validate palette tokens, HEX values and required WCAG contrast pairs."
        classpath = toolingClasspath
        mainClass = toolingMainClass
        workingDir(repositoryDirectory)
        args("validate", palettePath.get())
    }

tasks.register<JavaExec>("reportCvd") {
    group = "verification"
    description = "Report Machado CVD simulations and OKLab distances (not certification)."
    classpath = toolingClasspath
    mainClass = toolingMainClass
    workingDir(repositoryDirectory)
    args("cvd", palettePath.get())
}

tasks.register<JavaExec>("generateGhostty") {
    group = "generation"
    description = "Generate the Ghostty theme from the shared palette."
    classpath = toolingClasspath
    mainClass = toolingMainClass
    workingDir(repositoryDirectory)
    args(
        "generate-ghostty",
        "--palette",
        palettePath.get(),
        "--output",
        ghosttyOutputPath.get(),
        "--release",
        releaseVersion.get(),
    )
}

val checkGhostty =
    tasks.register<JavaExec>("checkGhostty") {
        group = "verification"
        description = "Check the Ghostty theme is current without writing files."
        classpath = toolingClasspath
        mainClass = toolingMainClass
        workingDir(repositoryDirectory)
        args(
            "generate-ghostty",
            "--palette",
            palettePath.get(),
            "--output",
            ghosttyOutputPath.get(),
            "--release",
            releaseVersion.get(),
            "--check",
        )
    }

val checkJetBrains =
    tasks.register<JavaExec>("checkJetBrains") {
        group = "verification"
        description = "Check JetBrains theme colors and derived colors against the shared palette."
        classpath = toolingClasspath
        mainClass = toolingMainClass
        workingDir(repositoryDirectory)
        args("check-jetbrains", "--palette", palettePath.get())
    }

tasks.register<JavaExec>("generateSite") {
    group = "generation"
    description = "Generate the static palette website from the shared palette."
    classpath = toolingClasspath
    mainClass = "icedamericano.tooling.site.SiteGeneratorKt"
    workingDir(repositoryDirectory)
    args(palettePath.get(), "docs/index.html")
}

val checkSite =
    tasks.register<JavaExec>("checkSite") {
        group = "verification"
        description = "Check the palette website is current without writing files."
        classpath = toolingClasspath
        mainClass = "icedamericano.tooling.site.SiteGeneratorKt"
        workingDir(repositoryDirectory)
        args(palettePath.get(), "docs/index.html", "--check")
    }

tasks.named<JavaExec>("run") { workingDir(repositoryDirectory) }

tasks.named("check") { dependsOn(validatePalette, checkGhostty, checkJetBrains, checkSite) }

tasks.named<Test>("test") { useJUnitPlatform() }

ktfmt { kotlinLangStyle() }
