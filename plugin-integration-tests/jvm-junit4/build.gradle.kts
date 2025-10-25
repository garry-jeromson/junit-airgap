plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Test explicit @NoNetworkTest annotation
    debug = true // Enable debug logging
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit4)
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
}

// NOTE: NOT using useJUnitPlatform() - this runs pure JUnit 4 tests
// This configuration tests the bytecode enhancement path for JUnit 4 @Rule injection
