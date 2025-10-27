rootProject.name = "plugin-integration-tests"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // Include build-logic for convention plugins
    includeBuild("../build-logic")

    repositories {
        mavenLocal() // Consume locally published plugin
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal() // Consume locally published junit-airgap
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

// Test contracts shared module
include(":test-contracts")

// KMP integration tests
include(":kmp-junit5")
include(":kmp-junit4")
include(":kmp-kotlintest")
include(":kmp-kotlintest-junit5")

// Android integration tests
include(":android-robolectric")

// JVM integration tests
include(":jvm-junit5")
include(":jvm-junit4")

// Plugin configuration option tests
include(":jvm-junit5-apply-all")
include(":jvm-junit5-allowed-hosts")
include(":jvm-junit5-blocked-hosts")
include(":jvm-junit4-apply-all")
