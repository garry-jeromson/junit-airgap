plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.junit.no.network)
}

kotlin {
    jvmToolchain(17)

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

// Configure the junit-no-network plugin
junitNoNetwork {
    enabled = true
    debug = true
}

// Configure JVM test task
tasks.named<Test>("jvmTest") {
    description = "Run JVM performance benchmarks"
    group = "verification"

    // Ensure library and plugin are published to Maven Local before running benchmarks
    dependsOn(":junit-no-network:publishToMavenLocal")
    dependsOn(":gradle-plugin:publishToMavenLocal")

    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }

    // External timing: measure suite startup overhead (including JVMTI agent loading)
    var startTime: Long = 0
    doFirst {
        startTime = System.nanoTime()
        println("═══════════════════════════════════════════════════════════════")
        println("  Starting benchmark-treatment suite (with JVMTI agent)")
        println("═══════════════════════════════════════════════════════════════")
    }

    doLast {
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000.0

        println("═══════════════════════════════════════════════════════════════")
        println("  Benchmark-treatment suite completed in ${String.format("%.2f", durationMs)}ms")
        println("═══════════════════════════════════════════════════════════════")

        // Write timing to JSON file
        val outputDir = File(project.buildDir, "benchmark-results")
        outputDir.mkdirs()
        val timingFile = File(outputDir, "suite-timing.json")
        timingFile.writeText("""{"suiteDurationMs": $durationMs}""")
        println("  Suite timing written to: ${timingFile.absolutePath}")
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
