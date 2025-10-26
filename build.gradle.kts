plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.nexus.publish)
}

allprojects {
    group = "io.github.garryjeromson"
    version = "0.1.0-SNAPSHOT"

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.3.1")
        android.set(true)
        verbose.set(true)
        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(false)

        filter {
            exclude("**/build/**")
        }
    }
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

// Benchmark comparison task
tasks.register("compareBenchmarks") {
    description = "Compare benchmark results from control and treatment projects"
    group = "verification"

    // Note: Composite build provides libraries automatically, no publishing needed!
    // benchmark-control depends on published library (via deps), so publish that first
    dependsOn(":benchmarks:benchmark-control:jvmTest")
    dependsOn(":benchmarks:benchmark-treatment:jvmTest")

    doLast {
        val controlDir = project(":benchmarks:benchmark-control").layout.buildDirectory.get().asFile
        val treatmentDir = project(":benchmarks:benchmark-treatment").layout.buildDirectory.get().asFile

        // Load and compare results using buildSrc utility
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
        val reportFile = File(project.layout.buildDirectory.get().asFile, "benchmark-comparison.md")
        reportFile.parentFile.mkdirs()
        reportFile.writeText(report)

        println()
        println(report)
        println()
        println("Full report written to: ${reportFile.absolutePath}")
    }
}
