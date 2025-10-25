plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Test explicit @NoNetworkTest annotation
    debug = false
    injectJUnit4Rule = false // This project uses JUnit 5 only
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.engine)
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
}

// Configure JUnit Platform for test tasks
tasks.withType<Test> {
    useJUnitPlatform()
}
