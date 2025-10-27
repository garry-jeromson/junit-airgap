plugins {
    id("junit-extensions.kotlin-jvm-convention")
    id("junit-extensions.plugin-integration-test-convention")
    alias(libs.plugins.junit.airgap)
}

// Configure the plugin with allowed hosts
junitAirgap {
    enabled = true
    allowedHosts = listOf("localhost", "127.0.0.1", "*.local")
    debug = false
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.airgap)

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
