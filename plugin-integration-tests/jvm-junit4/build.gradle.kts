plugins {
    id("junit-extensions.kotlin-jvm-convention")
    id("junit-extensions.plugin-integration-test-convention")
    alias(libs.plugins.junit.airgap)
}

// Configure the plugin
junitAirgap {
    enabled = true
    applyToAllTests = false // Test explicit @BlockNetworkRequests annotation
    debug = false
    injectJUnit4Rule = true // Enable automatic @Rule injection for JUnit 4
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit4)
    testImplementation(libs.junit.airgap)

    // Test contracts for shared test behaviors
    testImplementation(projects.pluginIntegrationTests.testContracts)

    // HTTP clients for network testing
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.converter.scalars)
    testImplementation(libs.async.http.client)
    testImplementation(libs.slf4j.simple) // SLF4J implementation for async-http-client
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

// NOTE: NOT using useJUnitPlatform() - this runs pure JUnit 4 tests
// This configuration tests the bytecode enhancement path for JUnit 4 @Rule injection
