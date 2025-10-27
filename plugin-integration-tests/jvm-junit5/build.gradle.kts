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

// Configure JUnit Platform for test tasks
tasks.withType<Test> {
    useJUnitPlatform()
}
