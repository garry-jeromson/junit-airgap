rootProject.name = "junit-no-network"

pluginManagement {
    repositories {
        mavenLocal()  // For plugin-integration-test to consume locally published plugin
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
include(":plugin-integration-test")
include(":benchmark")
