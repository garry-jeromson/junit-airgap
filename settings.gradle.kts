rootProject.name = "junit-extensions-workspace"

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
// Only include these builds when not publishing (they require plugin to be published first)
val skipIncludedBuilds = gradle.startParameter.taskNames.any {
    it.contains("publish") || it.contains("MavenLocal")
}
if (!skipIncludedBuilds) {
    includeBuild("benchmarks")
    includeBuild("plugin-integration-tests")
}
