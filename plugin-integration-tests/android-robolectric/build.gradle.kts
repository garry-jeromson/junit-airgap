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
    namespace = "io.github.garryjeromson.junit.nonetwork.test.androidrobolectric"
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
}

// NOTE: Uses Robolectric for Android framework testing with JUnit 4
// This configuration tests the bytecode enhancement path for JUnit 4 @Rule injection
// Tests run on JVM but can access Android APIs via Robolectric
