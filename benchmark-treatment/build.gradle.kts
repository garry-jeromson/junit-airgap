plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.junit.no.network)
}

kotlin {
    jvmToolchain(21)

    // JVM target
    jvm()

    // Android target
    androidTarget()

    sourceSets {
        // Common source sets
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(project(":benchmark-common"))
            }
        }

        // JVM source sets
        val jvmTest by getting {
            dependencies {
                // Plugin will add junit-no-network dependency automatically

                // JUnit 5
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)

                implementation(kotlin("test"))
            }
        }

        // Android source sets
        val androidUnitTest by getting {
            dependencies {
                // Plugin will add junit-no-network dependency automatically

                // JUnit 4 for Android
                implementation(libs.junit4)

                // Robolectric for Android unit tests
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)

                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "io.github.garryjeromson.junit.nonetwork.benchmark"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// Configure the junit-no-network plugin
junitNoNetwork {
    enabled = true
    debug = false
}

// Configure JVM test task
tasks.named<Test>("jvmTest") {
    description = "Run JVM performance benchmarks"
    group = "verification"

    // Note: Dependencies are managed by compareBenchmarks task to ensure
    // publishing happens before timing measurements begin

    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Configure Android unit test task
tasks.matching { it.name.contains("UnitTest") }.configureEach {
    description = "Run Android performance benchmarks"
    group = "verification"

    // Ensure library and plugin are published to Maven Local before running benchmarks
    dependsOn(":junit-no-network:publishToMavenLocal")
    dependsOn(":gradle-plugin:publishToMavenLocal")
}

// Create combined benchmark task
tasks.register("benchmark") {
    description = "Run all performance benchmarks (JVM + Android)"
    group = "verification"

    dependsOn("jvmTest")
    dependsOn("testDebugUnitTest")
}

// Enable strict compilation - treat all warnings as errors
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}
