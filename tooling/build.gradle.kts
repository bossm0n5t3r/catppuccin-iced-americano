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
}

val palettePath = providers.gradleProperty("palette").orElse("palette/iced-americano.json")
val ghosttyOutputPath = providers.gradleProperty("output").orElse("ports/ghostty/iced-americano")
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
    args("generate-ghostty", "--palette", palettePath.get(), "--output", ghosttyOutputPath.get())
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
            "--check",
        )
    }

tasks.named<JavaExec>("run") { workingDir(repositoryDirectory) }

tasks.named("check") { dependsOn(validatePalette, checkGhostty) }

tasks.named<Test>("test") { useJUnitPlatform() }

ktfmt { kotlinLangStyle() }
