plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    jvmToolchain(17)

    // JVM target
    jvm()

    // Android target
    androidTarget()

    // iOS target (Apple Silicon simulator)
    iosSimulatorArm64()

    sourceSets {
        // Common source sets
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                // Ktor client core for shared networking code
                implementation(libs.ktor.client.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // JVM source sets
        val jvmMain by getting {
            dependencies {
                // Ktor CIO engine for JVM
                implementation(libs.ktor.client.cio)
            }
        }

        val jvmTest by getting {
            dependencies {
                // Depend on the published junit-no-network library from Maven Local
                // Using root artifact - Gradle automatically resolves to -jvm variant
                implementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")

                // JUnit 5
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)

                // HTTP clients for integration testing
                implementation(libs.okhttp)
                implementation(libs.apache.httpclient5)

                // Ktor client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.java)
                implementation(libs.kotlinx.coroutines.test)

                // Simple HTTP server for testing
                implementation(libs.nanohttpd)
            }
        }

        // Android source sets
        val androidMain by getting {
            dependencies {
                // Ktor OkHttp engine for Android
                implementation(libs.ktor.client.okhttp)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                // Depend on the published junit-no-network library from Maven Local
                // Using root artifact - Gradle automatically resolves to -android variant
                implementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")

                // JUnit 5
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)

                // Robolectric for Android unit tests
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)

                // HTTP clients for integration testing
                implementation(libs.okhttp)
                implementation(libs.nanohttpd)

                // Coroutines test for runTest
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.core)
            }
        }

        // iOS source sets
        val iosSimulatorArm64Main by getting {
            dependencies {
                // Ktor Darwin engine for iOS
                implementation(libs.ktor.client.darwin)
            }
        }

        val iosSimulatorArm64Test by getting {
            dependencies {
                // iOS network blocking is API structure only (no active blocking)
                // But we still test the structure
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "io.github.garryjeromson.junit.nonetwork.integrationtest"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// Configure JVM test task
tasks.named<Test>("jvmTest") {
    useJUnitPlatform()

    // Ensure library is published to Maven Local before running tests
    dependsOn(":junit-no-network:publishToMavenLocal")

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Configure Android unit test task (if it exists)
tasks.matching { it.name.contains("UnitTest") }.configureEach {
    // Ensure library is published to Maven Local before running tests
    dependsOn(":junit-no-network:publishToMavenLocal")
}
