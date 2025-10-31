import org.gradle.plugins.signing.Sign

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

// Project coordinates (group and version)
group = "io.github.garry-jeromson"
version = "0.1.0-beta.1"

// Configure signing to be optional (only required when keys are available)
// This allows publishToMavenLocal without GPG setup
tasks.withType<Sign>().configureEach {
    isRequired =
        providers
            .environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey")
            .orElse(providers.gradleProperty("signingInMemoryKey"))
            .map { true }
            .getOrElse(false)
}

// Configure ktlint for consistent code formatting
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.3.1")
    android.set(true)
    verbose.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)

    filter {
        exclude("**/build/**")
    }
}

kotlin {
    // Use Java 17 for broader compatibility (other projects use 21)
    jvmToolchain(17)

    // JVM target
    jvm()

    // Android target
    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        // Common source sets
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Note: JUnit dependencies moved to JVM/Android source sets
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        // JVM source sets
        val jvmMain by getting {
            dependencies {
                // JUnit dependencies for JVM
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit4)
                // JUnit Platform Launcher for LauncherSessionListener
                implementation(libs.junit.platform.launcher)
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
                // Android unit tests (Robolectric) run on JVM, so they need jvmMain classes
                // This gives access to NetworkBlockerContext for configuration
                implementation(projects.junitAirgap) {
                    attributes {
                        attribute(
                            org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute,
                            org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm,
                        )
                    }
                }

                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)

                // HTTP clients for Android integration testing
                implementation(libs.okhttp)
                implementation(libs.retrofit)
                implementation(libs.retrofit.converter.scalars)
                implementation(libs.volley)
                implementation(libs.nanohttpd)

                // Robolectric shadows for Apache HTTP (required by Volley)
                implementation(libs.robolectric.shadows.httpclient)
            }
        }
    }
}

// Enable strict compilation - treat all warnings as errors
// Apply to all Kotlin compilation tasks (JVM, Android)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(true)
        // Suppress Beta warning for expect/actual classes (KMP standard pattern)
        // Suppress experimental language version warning (Gradle 9.1.0 uses Kotlin 2.2.0)
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xsuppress-version-warnings",
        )
    }
}

android {
    namespace = "io.github.garryjeromson.junit.airgap"
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
            implementation(libs.kotlin.test)
            implementation(libs.junit.jupiter.engine)
            implementation(libs.junit.jupiter.params)

            // HTTP clients for integration testing
            implementation(libs.okhttp)
            implementation(libs.apache.httpclient5)
            implementation(libs.retrofit)
            implementation(libs.retrofit.converter.scalars)
            implementation(libs.reactor.netty.http)
            implementation("io.netty:netty-resolver-dns-native-macos:${libs.versions.netty.get()}:osx-aarch_64")
            implementation(libs.spring.webflux)
            implementation(libs.spring.context)
            implementation(libs.openfeign.core)
            implementation(libs.openfeign.okhttp)
            implementation(libs.fuel)
            implementation(libs.fuel.coroutines)

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

    // Use Java 21 toolchain (native agent built with Java 21)
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        },
    )

    // Load JVMTI agent for network blocking
    // Capture file references at configuration time for configuration cache compatibility
    val agentFile = project.file("../native/build/libjunit-airgap-agent.dylib")
    val agentPath = agentFile.absolutePath

    doFirst {
        if (!agentFile.exists()) {
            logger.warn("JVMTI agent not found at: $agentPath")
            logger.warn("Run 'make build-native' to build the native agent.")
            logger.warn("Tests will fail without the agent.")
        }
    }

    jvmArgs("-agentpath:$agentPath")

    // Pass junit.airgap system properties to test JVM
    // Capture system property at configuration time for configuration cache compatibility
    val debugProperty = System.getProperty("junit.airgap.debug") ?: "false"
    systemProperty("junit.airgap.debug", debugProperty)
}

// Make jvmTest depend on native build
tasks.named("jvmTest") {
    dependsOn("buildNativeAgent")
}

// Configure Android test tasks
// Note: Android tests use Robolectric which runs on JVM, so we can load the JVMTI agent
// Robolectric tests need the JVMTI agent to block network requests
tasks.withType<Test>().configureEach {
    if (name.contains("UnitTest")) {
        // Use Java 21 toolchain (native agent built with Java 21)
        javaLauncher.set(
            javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
            },
        )

        // Load JVMTI agent for network blocking
        // Capture file references at configuration time for configuration cache compatibility
        val agentFile = project.file("../native/build/libjunit-airgap-agent.dylib")
        val agentPath = agentFile.absolutePath

        doFirst {
            if (!agentFile.exists()) {
                logger.warn("JVMTI agent not found at: $agentPath")
                logger.warn("Run 'make build-native' to build the native agent.")
                logger.warn("Android tests will fail without the agent.")
            }
        }

        jvmArgs("-agentpath:$agentPath")

        // Pass junit.airgap system properties to test JVM
        // Capture system property at configuration time for configuration cache compatibility
        val debugProperty = System.getProperty("junit.airgap.debug") ?: "false"
        systemProperty("junit.airgap.debug", debugProperty)
    }
}

// Make Android unit tests depend on native build
tasks.withType<Test>().configureEach {
    if (name.contains("UnitTest")) {
        dependsOn("buildNativeAgent")
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

    // Use Java 21 toolchain (native agent built with Java 21)
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        },
    )

    // Load JVMTI agent for network blocking
    // Capture file references at configuration time for configuration cache compatibility
    val agentFile = project.file("../native/build/libjunit-airgap-agent.dylib")
    val agentPath = agentFile.absolutePath

    doFirst {
        if (!agentFile.exists()) {
            logger.warn("JVMTI agent not found at: $agentPath")
            logger.warn("Run 'make build-native' to build the native agent.")
            logger.warn("Integration tests will fail without the agent.")
        }
    }

    jvmArgs("-agentpath:$agentPath")

    // Pass junit.airgap system properties to test JVM
    // Capture system property at configuration time for configuration cache compatibility
    val debugProperty = System.getProperty("junit.airgap.debug") ?: "false"
    systemProperty("junit.airgap.debug", debugProperty)

    testLogging {
        events("failed")
        showStandardStreams = false
    }
}

// ============================================================================
// Native Agent Build Integration (JVMTI)
// ============================================================================

// Task to configure CMake (only runs if CMakeLists.txt changes)
tasks.register<Exec>("cmakeConfigureNativeAgent") {
    description = "Configure CMake for JVMTI native agent"
    group = "native"

    workingDir = project.file("../native")

    // Capture build directory at configuration time for configuration cache compatibility
    val buildDir = project.file("../native/build")

    // Create build directory if it doesn't exist
    doFirst {
        if (!buildDir.exists()) {
            buildDir.mkdirs()
        }
    }

    // Determine build type: Release for publishing, Debug for local development
    val buildType =
        if (project.gradle.startParameter.taskNames
                .any { it.contains("publish") || it.contains("release") }
        ) {
            "Release"
        } else {
            "Debug"
        }

    commandLine("cmake", "-S", ".", "-B", "build", "-DCMAKE_BUILD_TYPE=$buildType")

    // Only re-run if CMakeLists.txt or source files change
    inputs.files(
        "../native/CMakeLists.txt",
        "../native/include/agent.h",
        "../native/src/agent.cpp",
        "../native/src/socket_interceptor.cpp",
        "../native/src/dns_interceptor.cpp",
    )
    outputs.dir("../native/build")
}

// Task to build the native agent using CMake
tasks.register<Exec>("buildNativeAgent") {
    description = "Build JVMTI native agent (.dylib/.so/.dll)"
    group = "native"

    dependsOn("cmakeConfigureNativeAgent")

    workingDir = project.file("../native/build")
    commandLine("cmake", "--build", ".")

    // Input: all source files
    inputs.files(
        "../native/include/agent.h",
        "../native/src/agent.cpp",
        "../native/src/socket_interceptor.cpp",
    )

    // Output: the built native library (platform-specific)
    val libraryName =
        when {
            org.gradle.internal.os.OperatingSystem
                .current()
                .isMacOsX -> "libjunit-airgap-agent.dylib"
            org.gradle.internal.os.OperatingSystem
                .current()
                .isLinux -> "libjunit-airgap-agent.so"
            org.gradle.internal.os.OperatingSystem
                .current()
                .isWindows -> "junit-airgap-agent.dll"
            else -> throw GradleException("Unsupported platform: ${System.getProperty("os.name")}")
        }
    outputs.file("../native/build/$libraryName")
}

// Task to clean native build artifacts
tasks.register<Delete>("cleanNativeAgent") {
    description = "Clean JVMTI native agent build artifacts"
    group = "native"

    delete("../native/build")
}

// Add native clean to main clean task
tasks.named("clean") {
    dependsOn("cleanNativeAgent")
}

// ============================================================================
// Maven Central Publishing Configuration (Central Portal API)
// ============================================================================

mavenPublishing {
    // Publish to Central Portal with automatic release after validation
    publishToMavenCentral(automaticRelease = true)

    // Sign all publications with GPG (signing is optional via task configuration above)
    signAllPublications()

    // POM metadata for Maven Central
    pom {
        name.set("JUnit Airgap Extension")
        description.set("A JUnit extension that automatically fails tests attempting to make outgoing network requests")
        url.set("https://github.com/garry-jeromson/junit-airgap")

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
            connection.set("scm:git:git://github.com/garry-jeromson/junit-airgap.git")
            developerConnection.set("scm:git:ssh://github.com:garry-jeromson/junit-airgap.git")
            url.set("https://github.com/garry-jeromson/junit-airgap")
        }
    }

    // Coordinates (group already set above, artifactId inferred from project name)
    coordinates("io.github.garry-jeromson", "junit-airgap", version.toString())
}

// ============================================================================
// Code Coverage Configuration (Kover)
// ============================================================================

kover {
    reports {
        // Generate both XML (for CI) and HTML (for local viewing) reports
        total {
            xml {
                onCheck = true
            }
            html {
                onCheck = true
            }

            // Exclude integration tests from coverage
            filters {
                excludes {
                    packages("io.github.garryjeromson.junit.airgap.integration")
                }
            }

            // Verify minimum coverage thresholds
            verify {
                onCheck = true
                rule {
                    minBound(95)
                }
            }
        }
    }
}
