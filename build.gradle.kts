buildscript {
    dependencies {
        classpath("build-logic:benchmark-support")
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.nexus.publish)
}

// Configure Nexus publishing for Maven Central
nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(findProperty("ossrhUsername") as String? ?: System.getenv("ORG_GRADLE_PROJECT_ossrhUsername"))
            password.set(findProperty("ossrhPassword") as String? ?: System.getenv("ORG_GRADLE_PROJECT_ossrhPassword"))
        }
    }
}

// Exclude benchmark projects from the default test task
// Benchmarking is a separate concern from testing
// Benchmark tests should only run when explicitly requested via 'make benchmark' or 'compareBenchmarks'
gradle.taskGraph.whenReady {
    if (!gradle.startParameter.taskNames.any {
        it.contains("benchmark") || it.contains("Benchmark")
    }) {
        allTasks.filter { task ->
            task.project.path.startsWith(":benchmarks:") &&
            (task.name.endsWith("Test") || task.name == "test")
        }.forEach { task ->
            task.enabled = false
        }
    }
}

// Configure root test task to run all tests across the project
// This ensures that running `./gradlew test` runs comprehensive test coverage:
// - Core library tests (JVM + Android) via included build
// - Gradle plugin tests via included build
// - Integration tests via included build
// - Plugin integration tests (all 11 projects) in this workspace
tasks.register("test") {
    description = "Run all tests across the project (core library, gradle plugin, integration tests, and plugin integration tests)"
    group = "verification"

    // Depend on tests from the included build (junit-extensions-build)
    gradle.includedBuilds.find { it.name == "junit-extensions-build" }?.let { includedBuild ->
        dependsOn(
            includedBuild.task(":junit-no-network:test"),
            includedBuild.task(":gradle-plugin:test"),
            includedBuild.task(":junit-no-network:integrationTest")
        )
    }

    // Depend on all test tasks in this workspace (plugin-integration-tests)
    dependsOn(
        ":plugin-integration-tests:jvm-junit5:test",
        ":plugin-integration-tests:jvm-junit4:test",
        ":plugin-integration-tests:jvm-junit5-apply-all:test",
        ":plugin-integration-tests:jvm-junit5-allowed-hosts:test",
        ":plugin-integration-tests:jvm-junit5-blocked-hosts:test",
        ":plugin-integration-tests:jvm-junit4-apply-all:test",
        ":plugin-integration-tests:kmp-junit5:test",
        ":plugin-integration-tests:kmp-junit4:test",
        ":plugin-integration-tests:kmp-kotlintest:test",
        ":plugin-integration-tests:kmp-kotlintest-junit5:test",
        ":plugin-integration-tests:android-robolectric:test"
    )
}

// Benchmark comparison task
tasks.register("compareBenchmarks") {
    description = "Compare benchmark results from control and treatment projects"
    group = "verification"

    // Note: Composite build provides libraries automatically, no publishing needed!
    dependsOn(":benchmarks:benchmark-control:jvmTest")
    dependsOn(":benchmarks:benchmark-treatment:jvmTest")

    // Capture build directories at configuration time for configuration cache compatibility
    val controlBuildDir = project(":benchmarks:benchmark-control").layout.buildDirectory
    val treatmentBuildDir = project(":benchmarks:benchmark-treatment").layout.buildDirectory
    val rootBuildDir = layout.buildDirectory

    doLast {
        val controlDir = controlBuildDir.get().asFile
        val treatmentDir = treatmentBuildDir.get().asFile

        // Load and compare results using build-logic utility
        // Note: High percentage overhead is expected for very fast operations (microseconds)
        // because the JVMTI agent has a small constant overhead that becomes a large
        // percentage of tiny operation times. Set threshold high to catch regressions
        // while allowing expected overhead.
        val report = BenchmarkComparison.compare(
            controlDir = controlDir,
            treatmentDir = treatmentDir,
            maxOverheadPercent = 1000.0  // 10x overhead is acceptable for microbenchmarks
        )

        // Write report
        val reportFile = File(rootBuildDir.get().asFile, "benchmark-comparison.md")
        reportFile.parentFile.mkdirs()
        reportFile.writeText(report)

        println()
        println(report)
        println()
        println("Full report written to: ${reportFile.absolutePath}")
    }
}
