plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.maven.publish) apply false
}

// Publishing is now configured per-module using the vanniktech maven-publish plugin
// See junit-airgap/build.gradle.kts and gradle-plugin/build.gradle.kts
// This provides direct Central Portal API support via the Publisher API

// Configure root test task to run all tests across the project
// This ensures that running `./gradlew test` runs comprehensive test coverage:
// - Core library tests (JVM + Android)
// - Gradle plugin tests
// - Core integration tests
// - Plugin integration tests (all 11 integration test projects)
// Note: Benchmarks are excluded and run separately via compareBenchmarks
tasks.register("test") {
    description = "Run all tests: core library, plugin, and plugin integration tests"
    group = "verification"

    // Depend on tests from core library and plugin (main workspace)
    dependsOn(
        ":junit-airgap:test",
        ":junit-airgap:integrationTest",
        ":gradle-plugin:test",
    )

    // Publish plugin to Maven Local before running integration tests
    // Integration tests consume the plugin from Maven Local (realistic testing)
    dependsOn(":gradle-plugin:publishToMavenLocal")

    // Include plugin integration tests from the included build
    // Must run after publishToMavenLocal
    dependsOn(gradle.includedBuild("plugin-integration-tests").task(":test"))
    mustRunAfter(":gradle-plugin:publishToMavenLocal")
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
