plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktfmt)
}

repositories { mavenCentral() }

ktfmt { kotlinLangStyle() }
