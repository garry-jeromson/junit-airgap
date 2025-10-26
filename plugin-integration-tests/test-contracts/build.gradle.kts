plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)

    jvm()
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit.no.network)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("test"))
                // JUnit 5 for JVM tests
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.engine)
                // JUnit 4 support via Vintage engine
                implementation(libs.junit.vintage.engine)
                implementation(libs.junit4)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(kotlin("test"))
                // JUnit 4 for Android tests
                implementation(libs.junit4)
                // Robolectric for Android framework testing
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)
            }
        }
    }
}

android {
    namespace = "io.github.garryjeromson.junit.nonetwork.test.contracts"
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

// Enable strict compilation - treat all warnings as errors
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

// This is a library providing shared test utilities
// Configure Android test tasks to not fail when no tests are found
tasks.withType<Test>().configureEach {
    if (name.contains("AndroidUnitTest") || name.contains("UnitTest")) {
        // Don't fail Android unit test tasks when no tests are found
        // (this project has JVM and common tests, but no Android-specific tests)
        failOnNoDiscoveredTests.set(false)
    }
}
