/**
 * Kotlin JVM convention plugin.
 * Applied to pure JVM Kotlin projects.
 * Provides common JVM configuration on top of kotlin-common.
 */

plugins {
    kotlin("jvm")
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
