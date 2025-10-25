plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Test explicit @NoNetworkTest annotation
    debug = false
    injectJUnit4Rule = false // This project uses kotlin.test
}

kotlin {
    jvmToolchain(21)

    jvm()
    androidTarget()

    sourceSets {
        val commonTest by getting {
            dependencies {
                // Only kotlin.test - no explicit JUnit dependencies
                // Let KMP choose the test framework defaults for each platform
                implementation(kotlin("test"))
                implementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
            }
        }
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
}

// NOTE: No useJUnitPlatform() call - let KMP use its defaults
// This tests the most common KMP configuration where the test framework
// is automatically configured based on the platform
