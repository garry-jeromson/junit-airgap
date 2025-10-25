plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.junit.no.network)
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Test explicit @BlockNetworkRequests annotation
    debug = true // Enable debug logging
    injectJUnit4Rule = true // Enable automatic @Rule injection for JUnit 4
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit4)
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")

    // HTTP clients for network testing
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.converter.scalars)
    testImplementation(libs.async.http.client)
    testImplementation(libs.reactor.netty.http)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Enable strict compilation - treat all warnings as errors
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        allWarningsAsErrors = true
    }
}

// NOTE: NOT using useJUnitPlatform() - this runs pure JUnit 4 tests
// This configuration tests the bytecode enhancement path for JUnit 4 @Rule injection
