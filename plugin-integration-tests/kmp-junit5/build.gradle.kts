plugins {
    id("junit-extensions.kotlin-multiplatform-convention")
    id("junit-extensions.plugin-integration-test-convention")
    alias(libs.plugins.android.library)
    alias(libs.plugins.junit.no.network)
}

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Test explicit @BlockNetworkRequests annotation
    debug = false
}

kotlin {
    jvm()
    androidTarget()

    sourceSets {
        val commonTest by getting {
            dependencies {
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
                implementation(libs.kotlin.test)
                implementation(libs.junit.jupiter.engine)
                // Ktor HTTP client for JVM (CIO engine)
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
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
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

android {
    namespace = "io.github.garryjeromson.junit.nonetwork.test.kmpjunit5"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Configure JUnit Platform for test tasks
tasks.withType<Test> {
    useJUnitPlatform()
}
