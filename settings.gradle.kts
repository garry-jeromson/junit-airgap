rootProject.name = "junit-no-network"

pluginManagement {
    repositories {
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

include(":junit-no-network")
include(":gradle-plugin")
include(":integration-test-app")
include(":benchmark")
