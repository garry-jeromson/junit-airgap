plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `maven-publish`
    signing
}

kotlin {
    jvmToolchain(17)

    // JVM target
    jvm()

    // Android target
    androidTarget {
        publishLibraryVariants("release", "debug")
    }

    // iOS target (Apple Silicon simulator)
    iosSimulatorArm64()

    sourceSets {
        // Common source sets
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                // Note: JUnit dependencies moved to JVM/Android source sets
                // as they are not compatible with native (iOS) targets
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // JVM source sets
        val jvmMain by getting {
            dependencies {
                // JUnit dependencies for JVM
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit4)

                // Byte Buddy for modern network blocking (no SecurityManager deprecation)
                implementation(libs.byte.buddy)
                implementation(libs.byte.buddy.agent)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)
                implementation(libs.mockk)
            }
        }

        // Android source sets
        val androidMain by getting {
            dependencies {
                // JUnit dependencies for Android
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit4)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)

                // HTTP clients for Android integration testing
                implementation(libs.okhttp)
                implementation(libs.retrofit)
                implementation(libs.retrofit.converter.scalars)
                implementation(libs.nanohttpd)
            }
        }

        // iOS source sets
        val iosSimulatorArm64Main by getting {
            dependencies {
                // iOS-specific dependencies if needed
            }
        }

        val iosSimulatorArm64Test by getting {
            dependencies {
                // iOS test dependencies
            }
        }
    }
}

// Enable strict compilation - treat all warnings as errors
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        allWarningsAsErrors = true
        // Suppress Beta warning for expect/actual classes (KMP standard pattern)
        freeCompilerArgs = freeCompilerArgs + "-Xexpect-actual-classes"
    }
}

android {
    namespace = "io.github.garryjeromson.junit.nonetwork"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// Configure JVM integration test source set using Kotlin DSL
// Note: Integration tests are JVM-specific and test the compiled extension end-to-end
// Source set name: jvmIntegrationTest (follows <target><CompilationName> convention)
kotlin.jvm().compilations.create("integrationTest") {
    associateWith(kotlin.jvm().compilations.getByName("main"))
    associateWith(kotlin.jvm().compilations.getByName("test"))

    defaultSourceSet {
        kotlin.srcDir("src/jvmIntegrationTest/kotlin")

        dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit.jupiter.engine)
            implementation(libs.junit.jupiter.params)

            // HTTP clients for integration testing
            implementation(libs.okhttp)
            implementation(libs.apache.httpclient5)
            implementation(libs.retrofit)
            implementation(libs.retrofit.converter.scalars)
            implementation(libs.reactor.netty.http)

            // Ktor client for integration testing
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.java)
            implementation(libs.kotlinx.coroutines.test)

            // Simple HTTP server for testing
            implementation(libs.nanohttpd)
        }
    }
}

// Configure JVM test task
tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

// Configure Android test tasks
// Note: Android tests use Robolectric which primarily supports JUnit 4
// JUnit 5 support on Android requires junit-vintage-engine to run both frameworks together
// For now, we keep Android tests using JUnit 4 runner (default behavior)
tasks.withType<Test>().configureEach {
    if (name.contains("UnitTest")) {
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }
}

// Create integration test task (JVM)
tasks.register<Test>("integrationTest") {
    description = "Runs JVM integration tests using the compiled extension"
    group = "verification"

    val integrationCompilation = kotlin.jvm().compilations.getByName("integrationTest")
    testClassesDirs = integrationCompilation.output.classesDirs
    // Include both the compiled output and runtime dependencies in classpath
    classpath = integrationCompilation.output.classesDirs + integrationCompilation.runtimeDependencyFiles

    shouldRunAfter(tasks.named("jvmTest"))
    useJUnitPlatform()

    // Exclude Kotlin companion objects from test discovery
    exclude("**/*\$Companion.class")

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// ============================================================================
// Maven Central Publishing Configuration
// ============================================================================

publishing {
    publications.withType<MavenPublication> {
        // POM metadata for Maven Central
        pom {
            name.set("JUnit No-Network Extension")
            description.set("A JUnit extension that automatically fails tests attempting to make outgoing network requests")
            url.set("https://github.com/garry-jeromson/junit-request-blocker")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }

            developers {
                developer {
                    id.set("garry-jeromson")
                    name.set("Garry Jeromson")
                    email.set("garry.jeromson@gmail.com")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/garry-jeromson/junit-request-blocker.git")
                developerConnection.set("scm:git:ssh://github.com:garry-jeromson/junit-request-blocker.git")
                url.set("https://github.com/garry-jeromson/junit-request-blocker")
            }
        }
    }
}

// Signing configuration for Maven Central
signing {
    // Only require signing if publishing to Maven Central (not for local builds)
    setRequired { gradle.taskGraph.allTasks.any { it.name.contains("publish") } }

    // Use in-memory key from environment variables or gradle.properties
    val signingKeyId: String? = findProperty("signingKeyId") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingKeyId")
    val signingKey: String? = findProperty("signingKey") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingKey")
    val signingPassword: String? = findProperty("signingPassword") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingPassword")

    if (signingKeyId != null && signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
}
