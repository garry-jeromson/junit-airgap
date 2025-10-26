plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.junit.no.network)
}

group = "io.github.garryjeromson.test"
version = "1.0-SNAPSHOT"

// Configure the plugin to apply network blocking to ALL tests by default (JUnit 4)
junitNoNetwork {
    enabled = true
    injectJUnit4Rule = true // Enable bytecode injection for JUnit 4
    applyToAllTests = true // Block network by default via bytecode-injected @Rule
    debug = true
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
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

// Configure test tasks to use JUnit 4
tasks.withType<Test> {
    useJUnit()
}
