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

// Configure root test task to run all tests across the project
// This ensures that running `./gradlew test` runs comprehensive test coverage:
// - Core library tests (JVM + Android)
// - Gradle plugin tests
// - Integration tests
// Note: Plugin integration tests and benchmarks are in included builds
// and should be run separately (they require the plugin to be published to Maven Local first)
tasks.register("test") {
    description = "Run core library and gradle plugin tests"
    group = "verification"

    // Depend on tests from core library and plugin (main workspace)
    dependsOn(
        ":junit-airgap:test",
        ":gradle-plugin:test",
        ":junit-airgap:integrationTest"
    )
}

// Task to run plugin integration tests from the included build
// Requires: ./gradlew publishToMavenLocal first
tasks.register("testPluginIntegration") {
    description = "Run plugin integration tests (requires publishToMavenLocal)"
    group = "verification"

    dependsOn(gradle.includedBuild("plugin-integration-tests").task(":test"))
}

// Benchmark comparison task
// Requires: ./gradlew publishToMavenLocal first
tasks.register("compareBenchmarks") {
    description = "Compare benchmark results from control and treatment projects"
    group = "verification"

    dependsOn(gradle.includedBuild("benchmarks").task(":benchmark-control:jvmTest"))
    dependsOn(gradle.includedBuild("benchmarks").task(":benchmark-treatment:jvmTest"))

    doLast {
        logger.lifecycle("Benchmark tests completed.")
        logger.lifecycle("Note: Result comparison is not implemented yet")
    }
}
