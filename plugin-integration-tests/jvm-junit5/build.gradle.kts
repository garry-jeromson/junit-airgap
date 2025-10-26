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
    debug = false
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.no.network)

    // Test contracts for shared test behaviors
    testImplementation(projects.pluginIntegrationTests.testContracts)

    // HTTP clients for network testing
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.converter.scalars)
    testImplementation(libs.async.http.client)
    testImplementation(libs.slf4j.simple)  // SLF4J implementation for async-http-client
    testImplementation(libs.reactor.netty.http)
    testImplementation("io.netty:netty-resolver-dns-native-macos:${libs.versions.netty.get()}:osx-aarch_64")
    testImplementation(libs.spring.webflux)
    testImplementation(libs.spring.context)
    testImplementation(libs.openfeign.core)
    testImplementation(libs.openfeign.okhttp)
    testImplementation(libs.fuel)
    testImplementation(libs.fuel.coroutines)
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

    // Show test output for debugging
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
    }
}
