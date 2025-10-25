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
    applyToAllTests = false // Test explicit @NoNetworkTest annotation
    debug = false
}

kotlin {
    jvmToolchain(21)

    jvm()
    androidTarget()

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit.jupiter.engine)
                // JUnit Vintage engine to run JUnit 4 tests under JUnit Platform
                implementation(libs.junit.vintage.engine)
                implementation(libs.junit4)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit.jupiter.engine)
                // JUnit Vintage engine to run JUnit 4 tests under JUnit Platform
                implementation(libs.junit.vintage.engine)
                implementation(libs.junit4)
            }
        }
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
}

// Configure JUnit Platform for test tasks (runs both JUnit 5 and JUnit 4 via vintage engine)
tasks.withType<Test> {
    useJUnitPlatform()
}
