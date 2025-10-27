plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.junit.airgap) apply false
}

// Root test task that runs all integration test projects
tasks.register("test") {
    description = "Run all plugin integration tests"
    group = "verification"

    // JVM integration tests
    dependsOn(":jvm-junit5:test")
    dependsOn(":jvm-junit4:test")
    dependsOn(":jvm-junit5-apply-all:test")
    dependsOn(":jvm-junit5-allowed-hosts:test")
    dependsOn(":jvm-junit5-blocked-hosts:test")
    dependsOn(":jvm-junit4-apply-all:test")

    // KMP integration tests (test both JVM and Android)
    dependsOn(":kmp-junit5:jvmTest")
    dependsOn(":kmp-junit5:testDebugUnitTest")
    dependsOn(":kmp-junit4:jvmTest")
    dependsOn(":kmp-junit4:testDebugUnitTest")
    dependsOn(":kmp-kotlintest:jvmTest")
    dependsOn(":kmp-kotlintest:testDebugUnitTest")
    dependsOn(":kmp-kotlintest-junit5:jvmTest")
    dependsOn(":kmp-kotlintest-junit5:testDebugUnitTest")

    // Android integration tests
    dependsOn(":android-robolectric:testDebugUnitTest")
}
