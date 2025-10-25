plugins {
    kotlin("multiplatform")
    id("com.android.library")
    // Plugin under test - this is what we're validating!
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

group = "io.github.garryjeromson.plugin.test"
version = "1.0-SNAPSHOT"

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = true  // Test that default blocking works via plugin
    debug = false
}

kotlin {
    jvmToolchain(17)

    // JVM target
    jvm()

    // Android target
    androidTarget()

    sourceSets {
        val commonMain by getting {
            // No main code, this is test-only
        }

        val jvmTest by getting {
            dependencies {
                // IMPORTANT: NO manual junit-no-network dependency!
                // The plugin should add it automatically.

                // Only test framework dependencies
                implementation(kotlin("test"))
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                // IMPORTANT: NO manual junit-no-network dependency!
                // The plugin should add it automatically.

                // Only test framework dependencies
                implementation(kotlin("test"))
                implementation(libs.junit.jupiter.engine)
            }
        }
    }
}

android {
    namespace = "io.github.garryjeromson.junit.nonetwork.plugin.test"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Configure JUnit 5 for both JVM and Android
tasks.withType<Test> {
    useJUnitPlatform()
}
