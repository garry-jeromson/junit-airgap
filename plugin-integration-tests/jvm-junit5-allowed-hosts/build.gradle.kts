plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.junit.no.network)
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

// Configure the plugin with allowed hosts
junitNoNetwork {
    enabled = true
    allowedHosts = listOf("localhost", "127.0.0.1", "*.local")
    debug = true
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.engine)
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")

    // Test contracts for shared test behaviors
    testImplementation(project(":plugin-integration-tests:test-contracts"))
}

// Enable strict compilation - treat all warnings as errors
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

// Configure JUnit Platform for test tasks
tasks.withType<Test> {
    useJUnitPlatform()
}
