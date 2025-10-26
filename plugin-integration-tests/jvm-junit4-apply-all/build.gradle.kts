plugins {
    id("junit-extensions.kotlin-jvm-convention")
    alias(libs.plugins.junit.no.network)
}

// Configure the plugin to apply network blocking to ALL tests by default (JUnit 4)
junitNoNetwork {
    enabled = true
    injectJUnit4Rule = true // Enable bytecode injection for JUnit 4
    applyToAllTests = true // Block network by default via bytecode-injected @Rule
    debug = false
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.junit.no.network)

    // Test contracts for shared test behaviors
    testImplementation(projects.pluginIntegrationTests.testContracts)

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

// Configure test tasks to use JUnit 4
tasks.withType<Test> {
    useJUnit()
}
