rootProject.name = "junit-no-network"

pluginManagement {
    repositories {
        mavenLocal() // For plugin-integration-test to consume locally published plugin
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
include(":benchmark")

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
