rootProject.name = "benchmarks"

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

// Benchmark projects
include(":benchmark-common")
include(":benchmark-control")
include(":benchmark-treatment")
