plugins {
    alias(libs.plugins.android.library)
    kotlin("android")
    alias(libs.plugins.junit.no.network)
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Test explicit @NoNetworkTest annotation
    debug = true
    injectJUnit4Rule = true // Enable automatic @Rule injection for JUnit 4
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
