/**
 * Kotlin Multiplatform convention plugin.
 * Applied to KMP projects.
 * Provides common KMP configuration on top of kotlin-common.
 */

plugins {
    kotlin("multiplatform")
    id("junit-extensions.kotlin-common-convention")
}

// Configure JVM toolchain to use Java 21
kotlin {
    jvmToolchain(21)
}

// Configure test tasks to use JUnit Platform
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
