plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.junit.no.network)
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Test explicit @BlockNetworkRequests annotation
    debug = false // Enable debug logging
    injectJUnit4Rule = true // Enable automatic @Rule injection for JUnit 4
}

kotlin {
    jvmToolchain(21)

    jvm()
    androidTarget()

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit.no.network)
                // Test contracts for shared test behaviors
                implementation(projects.pluginIntegrationTests.testContracts)
                // Ktor HTTP client core (platform-independent)
                implementation(libs.ktor.client.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit.jupiter.engine)
                // JUnit Vintage engine to run JUnit 4 tests under JUnit Platform
                implementation(libs.junit.vintage.engine)
                implementation(libs.junit4)
                // Ktor HTTP client for JVM (CIO engine)
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit.jupiter.engine)
                // JUnit Vintage engine to run JUnit 4 tests under JUnit Platform
                implementation(libs.junit.vintage.engine)
                implementation(libs.junit4)
                // Robolectric for Android framework testing
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)
                // Ktor HTTP client for Android (OkHttp engine for Robolectric compatibility)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.test)
                // Note: junit-no-network-jvm is added automatically by the plugin for Robolectric support
            }
        }
    }
}

// Enable strict compilation - treat all warnings as errors
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

android {
    namespace = "io.github.garryjeromson.junit.nonetwork.test.kmpjunit4"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Configure JUnit Platform for test tasks (runs both JUnit 5 and JUnit 4 via vintage engine)
tasks.withType<Test> {
    useJUnitPlatform()
}
