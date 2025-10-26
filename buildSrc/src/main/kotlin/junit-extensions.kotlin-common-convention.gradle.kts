/**
 * Common Kotlin configuration applied to all Kotlin projects.
 * This replaces the allprojects{} block and provides:
 * - Group and version configuration
 * - ktlint code formatting
 * - Compiler warnings as errors
 */

plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

// Project coordinates
group = "io.github.garryjeromson"
version = "0.1.0-beta.1"

// Configure ktlint for consistent code formatting
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.3.1")
    android.set(true)
    verbose.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)

    filter {
        exclude("**/build/**")
    }
}

// Treat all compiler warnings as errors for strict compilation
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}
