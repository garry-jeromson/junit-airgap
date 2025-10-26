plugins {
    id("junit-extensions.kotlin-jvm-convention")
    id("junit-extensions.plugin-integration-test-convention")
    alias(libs.plugins.junit.no.network)
}

// Configure the plugin with blocked hosts
// Note: When using blockedHosts, you typically also want to set allowedHosts to ["*"]
// to allow all hosts EXCEPT those in blockedHosts
junitNoNetwork {
    enabled = true
    allowedHosts = listOf("*")  // Allow all hosts by default
    blockedHosts = listOf("*.example.com", "badhost.io")  // Except these
    debug = false
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
    testImplementation(libs.kotlinx.coroutines.test)
}

// Configure JUnit Platform for test tasks
tasks.withType<Test> {
    useJUnitPlatform()
}
