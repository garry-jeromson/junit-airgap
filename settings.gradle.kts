rootProject.name = "junit-airgap-workspace"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // Include build-logic for convention plugins
    includeBuild("build-logic")

    repositories {
        mavenLocal() // For included builds to consume locally published plugin
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal() // For integration-test-app to consume published artifacts
        google()
        mavenCentral()
    }
}

// Core library and plugin
include(":junit-airgap")
include(":gradle-plugin")

// Benchmark projects and plugin integration tests are included builds
// This avoids circular dependency during configuration phase:
// - They consume the plugin from Maven Local (realistic testing)
// - They're isolated workspaces with their own configuration
// - Main build doesn't evaluate their build files during configuration
//
// Bootstrap logic:
// 1. Skip included builds when running publish tasks (they need the published plugin)
// 2. Check if plugin exists in Maven Local before including builds
// 3. Provide helpful error message when plugin is missing

// Only skip included builds when DIRECTLY publishing (not when test depends on publish)
val isPublishing = gradle.startParameter.taskNames.any {
    it == "publishToMavenLocal" ||
    it == ":gradle-plugin:publishToMavenLocal" ||
    it == "publish" ||
    it.startsWith("publish") && !it.contains("test", ignoreCase = true)
}

val pluginMarkerPath = file("${System.getProperty("user.home")}/.m2/repository/io/github/garry-jeromson/junit-airgap-gradle-plugin")
val pluginExists = pluginMarkerPath.exists()

if (isPublishing) {
    // Skip included builds during publishing to avoid circular dependency
    logger.quiet("Skipping included builds during publish operation")
} else if (!pluginExists) {
    // Plugin not published yet - provide helpful message
    logger.warn("")
    logger.warn("═══════════════════════════════════════════════════════════════")
    logger.warn("  Plugin not found in Maven Local")
    logger.warn("═══════════════════════════════════════════════════════════════")
    logger.warn("The Gradle plugin must be published to Maven Local before")
    logger.warn("included builds (benchmarks, plugin-integration-tests) can be used.")
    logger.warn("")
    logger.warn("Run one of:")
    logger.warn("  make test              (auto-bootstraps and runs all tests)")
    logger.warn("  make bootstrap         (just bootstrap the plugin)")
    logger.warn("  ./gradlew publishToMavenLocal")
    logger.warn("")
    logger.warn("Skipping included builds for now...")
    logger.warn("═══════════════════════════════════════════════════════════════")
    logger.warn("")
} else {
    // Plugin exists - include all builds
    includeBuild("benchmarks")
    includeBuild("plugin-integration-tests")
}
