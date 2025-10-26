rootProject.name = "junit-extensions-workspace"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // Include build-logic for convention plugins
    includeBuild("build-logic")

    // Include the composite build for plugin resolution
    // This allows benchmark projects to use the plugin without Maven Local publishing
    includeBuild("junit-extensions-build")

    repositories {
        mavenLocal() // For plugin-integration-test to consume locally published plugin
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Include the composite build for dependency resolution
    // This allows benchmark projects to depend on junit-no-network without Maven Local
    includeBuild("junit-extensions-build")

    repositories {
        mavenLocal() // For integration-test-app to consume published artifacts
        google()
        mavenCentral()
    }
}

// Benchmark projects
include(":benchmarks:benchmark-common")
include(":benchmarks:benchmark-control")
include(":benchmarks:benchmark-treatment")

// Plugin integration tests
include(":plugin-integration-tests:test-contracts")
include(":plugin-integration-tests:kmp-junit5")
include(":plugin-integration-tests:kmp-junit4")
include(":plugin-integration-tests:kmp-kotlintest")
include(":plugin-integration-tests:kmp-kotlintest-junit5")
include(":plugin-integration-tests:android-robolectric")
include(":plugin-integration-tests:jvm-junit5")
include(":plugin-integration-tests:jvm-junit4")

// Plugin configuration option tests
include(":plugin-integration-tests:jvm-junit5-apply-all")
include(":plugin-integration-tests:jvm-junit5-allowed-hosts")
include(":plugin-integration-tests:jvm-junit5-blocked-hosts")
include(":plugin-integration-tests:jvm-junit4-apply-all")
