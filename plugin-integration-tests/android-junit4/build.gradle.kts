plugins {
    alias(libs.plugins.android.library)
    kotlin("android")
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false  // Test explicit @NoNetworkTest annotation
    debug = false
}

android {
    namespace = "io.github.garryjeromson.junit.nonetwork.test.androidjunit4"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit4)
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
}

// NOTE: NOT using useJUnitPlatform() - this runs pure JUnit 4 tests
// This configuration tests the bytecode enhancement path for JUnit 4 @Rule injection
