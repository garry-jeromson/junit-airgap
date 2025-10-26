plugins {
    kotlin("multiplatform")
    id("com.android.library")
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
                // Depend on the published junit-no-network library from Maven Local
                // Use root artifact for proper platform variant matching
                implementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")

                // JUnit 5
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)

                implementation(kotlin("test"))
            }
        }

        // Android source sets
        val androidUnitTest by getting {
            dependencies {
                // Depend on the published junit-no-network library from Maven Local
                // Use root artifact for proper Android variant matching
                implementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")

                // JUnit 4 for Android
                implementation(libs.junit4)

                // Robolectric for Android unit tests
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)

                implementation(kotlin("test"))

                // Note: junit-no-network-jvm would be added automatically by the plugin,
                // but benchmark doesn't use the plugin, so we add it manually
                implementation("io.github.garryjeromson:junit-no-network-jvm:0.1.0-SNAPSHOT")
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

    // Ensure library is published to Maven Local before running benchmarks
    dependsOn(":junit-no-network:publishToMavenLocal")
}

// Create combined benchmark task
tasks.register("benchmark") {
    description = "Run all performance benchmarks (JVM + Android)"
    group = "verification"

    dependsOn("jvmTest")
    dependsOn("testDebugUnitTest")
}
