plugins {
    id("junit-extensions.kotlin-multiplatform")
    id("com.android.library")
}

kotlin {
    // jvmToolchain(21) is configured by junit-extensions.kotlin-multiplatform

    // JVM target
    jvm()

    // Android target
    androidTarget()

    sourceSets {
        // Common source sets
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(projects.benchmarks.benchmarkCommon)
            }
        }

        // JVM source sets
        val jvmTest by getting {
            dependencies {
                // Depend on the published junit-no-network library from Maven Local
                // Use root artifact for proper platform variant matching
                implementation(libs.junit.no.network)

                // JUnit 5
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)

                implementation(libs.kotlin.test)
            }
        }

        // Android source sets
        val androidUnitTest by getting {
            dependencies {
                // Depend on the published junit-no-network library from Maven Local
                // Use root artifact for proper Android variant matching
                implementation(libs.junit.no.network)

                // JUnit 4 for Android
                implementation(libs.junit4)

                // Robolectric for Android unit tests
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)

                implementation(libs.kotlin.test)

                // Note: junit-no-network-jvm would be added automatically by the plugin,
                // but benchmark doesn't use the plugin, so we need the JVM variant explicitly
                // Using the catalog reference which resolves to the multiplatform artifact
                // that includes the JVM variant
                implementation(libs.junit.no.network)
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

    // Note: Library is provided by composite build automatically
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
