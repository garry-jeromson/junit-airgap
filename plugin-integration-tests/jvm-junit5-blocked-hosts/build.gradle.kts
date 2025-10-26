plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.junit.no.network)
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

// Configure the plugin with blocked hosts
// Note: When using blockedHosts, you typically also want to set allowedHosts to ["*"]
// to allow all hosts EXCEPT those in blockedHosts
junitNoNetwork {
    enabled = true
    allowedHosts = listOf("*")  // Allow all hosts by default
    blockedHosts = listOf("*.example.com", "badhost.io")  // Except these
    debug = false
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.engine)
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-beta.1")

    // Test contracts for shared test behaviors
    testImplementation(project(":plugin-integration-tests:test-contracts"))

    // HTTP clients for network testing
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.kotlinx.coroutines.test)
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
