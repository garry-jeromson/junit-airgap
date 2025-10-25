plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `maven-publish`
}

kotlin {
    jvmToolchain(17)

    // JVM target
    jvm()

    // Android target
    androidTarget {
        publishLibraryVariants("release", "debug")
    }

    // iOS target (Apple Silicon simulator)
    iosSimulatorArm64()

    sourceSets {
        // Common source sets
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                // Note: JUnit dependencies moved to JVM/Android source sets
                // as they are not compatible with native (iOS) targets
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
                // JUnit dependencies for JVM
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit4)

                // Byte Buddy for modern network blocking (no SecurityManager deprecation)
                implementation(libs.byte.buddy)
                implementation(libs.byte.buddy.agent)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)
                implementation(libs.mockk)
            }
        }

        // Android source sets
        val androidMain by getting {
            dependencies {
                // JUnit dependencies for Android
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit4)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)

                // HTTP clients for Android integration testing
                implementation(libs.okhttp)
                implementation(libs.nanohttpd)
            }
        }

        // iOS source sets
        val iosSimulatorArm64Main by getting {
            dependencies {
                // iOS-specific dependencies if needed
            }
        }

        val iosSimulatorArm64Test by getting {
            dependencies {
                // iOS test dependencies
            }
        }
    }
}

android {
    namespace = "io.github.garryjeromson.junit.nonetwork"
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

// Configure JVM integration test source set using Kotlin DSL
// Note: Integration tests are JVM-specific and test the compiled extension end-to-end
// Source set name: jvmIntegrationTest (follows <target><CompilationName> convention)
kotlin.jvm().compilations.create("integrationTest") {
    associateWith(kotlin.jvm().compilations.getByName("main"))
    associateWith(kotlin.jvm().compilations.getByName("test"))

    defaultSourceSet {
        kotlin.srcDir("src/jvmIntegrationTest/kotlin")

        dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit.jupiter.engine)
            implementation(libs.junit.jupiter.params)

            // HTTP clients for integration testing
            implementation(libs.okhttp)
            implementation(libs.apache.httpclient5)

            // Ktor client for integration testing
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.java)
            implementation(libs.kotlinx.coroutines.test)

            // Simple HTTP server for testing
            implementation(libs.nanohttpd)
        }
    }
}

// Configure JVM test task
tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

// Create integration test task (JVM)
tasks.register<Test>("integrationTest") {
    description = "Runs JVM integration tests using the compiled extension"
    group = "verification"

    val integrationCompilation = kotlin.jvm().compilations.getByName("integrationTest")
    testClassesDirs = integrationCompilation.output.classesDirs
    // Include both the compiled output and runtime dependencies in classpath
    classpath = integrationCompilation.output.classesDirs + integrationCompilation.runtimeDependencyFiles

    shouldRunAfter(tasks.named("jvmTest"))
    useJUnitPlatform()

    // Exclude Kotlin companion objects from test discovery
    exclude("**/*\$Companion.class")

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
