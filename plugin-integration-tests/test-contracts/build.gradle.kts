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
                implementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
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
