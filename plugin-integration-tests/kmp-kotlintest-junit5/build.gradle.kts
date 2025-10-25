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
    debug = false
    // NO injectJUnit4Rule - this project uses JUnit 5 runtime with kotlin.test
    // Network blocking is provided by JUnit 5 extension (no bytecode injection)
}

kotlin {
    jvmToolchain(21)

    jvm()
    androidTarget()

    sourceSets {
        val commonTest by getting {
            dependencies {
                // kotlin.test - will run on JUnit 5 via useJUnitPlatform()
                implementation(kotlin("test"))
                implementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
                // Ktor HTTP client core (platform-independent)
                implementation(libs.ktor.client.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                // JUnit 5 (Jupiter) engine - enables kotlin.test to run on JUnit 5
                implementation(libs.junit.jupiter.engine)
                // Ktor HTTP client for JVM (CIO engine)
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                // JUnit 5 (Jupiter) engine - enables kotlin.test to run on JUnit 5
                implementation(libs.junit.jupiter.engine)
                // Ktor HTTP client for Android (OkHttp engine)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.test)
                // Note: junit-no-network-jvm is added automatically by the plugin for Robolectric support
            }
        }
    }
}

// Enable strict compilation - treat all warnings as errors
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        allWarningsAsErrors = true
    }
}

android {
    namespace = "io.github.garryjeromson.junit.nonetwork.test.kmpkotlintestjunit5"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Configure JUnit Platform - this makes kotlin.test run on JUnit 5 instead of JUnit 4
// Network blocking works via JUnit 5 extension (automatic discovery)
tasks.withType<Test> {
    useJUnitPlatform()
}
