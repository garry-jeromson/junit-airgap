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
    applyToAllTests = false // Test explicit @BlockNetworkRequests annotation
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
        allWarningsAsErrors = true
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
    // Note: junit-no-network and junit-no-network-jvm are added automatically by the plugin

    // HTTP clients for network testing
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.okhttp)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.converter.scalars)
    testImplementation(libs.reactor.netty.http)
    testImplementation(libs.kotlinx.coroutines.test)
}

// NOTE: Uses Robolectric for Android framework testing with JUnit 4
// This configuration tests the bytecode enhancement path for JUnit 4 @Rule injection
// Tests run on JVM but can access Android APIs via Robolectric
