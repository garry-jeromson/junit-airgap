plugins {
    id("junit-extensions.kotlin-multiplatform-convention")
    id("junit-extensions.plugin-integration-test-convention")
    alias(libs.plugins.android.library)
    alias(libs.plugins.junit.airgap)
}

// Configure the plugin
junitAirgap {
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
                implementation(libs.junit.airgap)
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
                // JUnit 4 required for Robolectric @RunWith annotation
                implementation(libs.junit4)
                // Note: junit-airgap-jvm is added automatically by the plugin for Robolectric support
            }
        }
    }
}

android {
    namespace = "io.github.garryjeromson.junit.airgap.test.kmpkotlintest"
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
