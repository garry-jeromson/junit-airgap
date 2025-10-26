plugins {
    alias(libs.plugins.android.library)
    kotlin("android")
    id("junit-extensions.kotlin-common-convention")
    alias(libs.plugins.junit.no.network)
}

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Test explicit @BlockNetworkRequests annotation
    debug = false
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            allWarningsAsErrors.set(true)
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // Note: junit-no-network and junit-no-network-jvm are added automatically by the plugin

    // Test contracts for shared test behaviors
    testImplementation(projects.pluginIntegrationTests.testContracts)

    // HTTP clients for network testing
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.okhttp)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.converter.scalars)
    testImplementation(libs.reactor.netty.http)
    testImplementation("io.netty:netty-resolver-dns-native-macos:${libs.versions.netty.get()}:osx-aarch_64")
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.volley)
    testImplementation(libs.robolectric.shadows.httpclient)
}

// NOTE: Uses Robolectric for Android framework testing with JUnit 4
// This configuration tests the bytecode enhancement path for JUnit 4 @Rule injection
// Tests run on JVM but can access Android APIs via Robolectric
