plugins {
    id("junit-extensions.kotlin-multiplatform-convention")
    alias(libs.plugins.android.library)
    alias(libs.plugins.junit.no.network)
}

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Test explicit @BlockNetworkRequests annotation
    debug = false
    injectJUnit4Rule = true // Enable automatic @Rule injection (kotlin.test uses JUnit 4 on Android)
}

kotlin {
    jvm()
    androidTarget()

    sourceSets {
        val commonTest by getting {
            dependencies {
                // Only kotlin.test - no explicit JUnit dependencies
                // Let KMP choose the test framework defaults for each platform
                implementation(libs.kotlin.test)
                implementation(libs.junit.no.network)
                // Test contracts for shared test behaviors
                implementation(projects.pluginIntegrationTests.testContracts)
                // Ktor HTTP client core (platform-independent)
                implementation(libs.ktor.client.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                // Ktor HTTP client for JVM (CIO engine)
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                // Ktor HTTP client for Android (OkHttp engine)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.test)
                // Robolectric for Android framework testing
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)
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
    namespace = "io.github.garryjeromson.junit.nonetwork.test.kmpkotlintest"
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

// NOTE: No useJUnitPlatform() call - let KMP use its defaults
// This tests the most common KMP configuration where the test framework
// is automatically configured based on the platform
