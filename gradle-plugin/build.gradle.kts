plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("com.gradle.plugin-publish") version "1.3.0"
    id("junit-extensions.kotlin-common-convention")
    alias(libs.plugins.kover)
}

gradlePlugin {
    // Plugin Portal configuration
    website.set("https://github.com/garry-jeromson/junit-request-blocker")
    vcsUrl.set("https://github.com/garry-jeromson/junit-request-blocker.git")

    plugins {
        create("junitNoNetworkPlugin") {
            id = "io.github.garryjeromson.junit-no-network"
            implementationClass = "io.github.garryjeromson.junit.nonetwork.gradle.JunitNoNetworkPlugin"
            displayName = "JUnit No-Network Plugin"
            description = "Automatically configure JUnit tests to block network requests"
            tags.set(listOf("testing", "junit", "junit5", "junit4", "network", "isolation", "test-isolation"))
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlin.stdlib)

    // ByteBuddy for JUnit 4 rule injection via bytecode enhancement
    implementation(libs.byte.buddy)

    // JUnit 4 API for detecting test annotations
    implementation(libs.junit4)

    testImplementation(gradleTestKit())
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter.engine)
}

// Additional compiler flags specific to gradle-plugin
// (allWarningsAsErrors is already configured by junit-extensions.kotlin-common)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        // Suppress Beta warning for expect/actual classes (KMP standard pattern from junit-no-network dependency)
        // Suppress experimental language version warning (Gradle 9.1.0 uses Kotlin 2.2.0)
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xsuppress-version-warnings"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

// ============================================================================
// Native Agent Packaging
// ============================================================================

// Task to copy the native agent from the junit-no-network build to plugin resources
tasks.register<Copy>("packageNativeAgent") {
    description = "Copy native JVMTI agent into plugin resources for packaging"
    group = "native"

    // Determine platform-specific paths at configuration time
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()

    val os =
        when {
            osName.contains("mac") || osName.contains("darwin") -> "darwin"
            osName.contains("linux") -> "linux"
            osName.contains("windows") -> "windows"
            else -> {
                logger.warn("Unsupported OS: $osName - native agent will not be packaged")
                return@register
            }
        }

    val arch =
        when {
            osArch.contains("aarch64") || osArch.contains("arm64") -> "aarch64"
            osArch.contains("x86_64") || osArch.contains("amd64") -> "x86-64"
            else -> {
                logger.warn("Unsupported architecture: $osArch - native agent will not be packaged")
                return@register
            }
        }

    val agentFileName =
        when (os) {
            "darwin" -> "libjunit-no-network-agent.dylib"
            "linux" -> "libjunit-no-network-agent.so"
            "windows" -> "junit-no-network-agent.dll"
            else -> {
                logger.warn("Unknown OS: $os")
                return@register
            }
        }

    // Source: built agent from junit-no-network module
    // Use layout.projectDirectory navigation for configuration cache compatibility
    // gradle-plugin is a sibling of junit-no-network, so go up to parent, then into native/build
    from(layout.projectDirectory.dir("../native/build")) {
        include(agentFileName)
    }

    // Destination: plugin resources by platform
    into("src/main/resources/native/$os-$arch")

    // Depend on the native build task from junit-no-network
    dependsOn(":junit-no-network:buildNativeAgent")

    // Capture agent file path at configuration time for configuration cache
    val agentFilePath = layout.projectDirectory.file("../native/build/$agentFileName").asFile

    // Capture build type at configuration time for configuration cache
    val isReleaseBuild = gradle.startParameter.taskNames.any { it.contains("publish") || it.contains("release") }
    val buildType = if (isReleaseBuild) "Release" else "Debug"

    doFirst {
        logger.lifecycle("Packaging native agent (BUILD_TYPE=$buildType) for $os-$arch")

        // Verify the agent file exists (using captured path)
        if (!agentFilePath.exists()) {
            throw GradleException(
                "Native agent not found at ${agentFilePath.absolutePath}. " +
                    "Run ':junit-no-network:buildNativeAgent' first.",
            )
        }
    }

    doLast {
        logger.lifecycle("Packaged native agent for $os-$arch into plugin resources")
    }
}

// Make processResources depend on packaging the native agent
tasks.named("processResources") {
    dependsOn("packageNativeAgent")
}

// Make all source JAR tasks depend on packageNativeAgent to avoid task ordering issues
tasks.withType<Jar>().configureEach {
    if (name.contains("sources", ignoreCase = true)) {
        dependsOn("packageNativeAgent")
    }
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            // groupId and version are inherited from project
            artifactId = "junit-no-network-gradle-plugin"

            // POM metadata for Maven Central
            pom {
                name.set("JUnit No-Network Gradle Plugin")
                description.set("Gradle plugin for automatically configuring JUnit tests to block network requests")
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
}

// Signing configuration for Maven Central
signing {
    // Only require signing if publishing to Maven Central (not for Maven Local)
    // This ensures publishToMavenLocal doesn't require signing credentials
    setRequired {
        gradle.taskGraph.allTasks.any { task ->
            task.name.contains("publish", ignoreCase = true) &&
                !task.name.contains("ToMavenLocal", ignoreCase = true)
        }
    }

    // Use in-memory key from environment variables or gradle.properties
    val signingKeyId: String? = findProperty("signingKeyId") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingKeyId")
    val signingKey: String? = findProperty("signingKey") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingKey")
    val signingPassword: String? = findProperty("signingPassword") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingPassword")

    if (signingKeyId != null && signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    }

    // Only configure signing tasks if we have credentials
    if (signingKeyId != null && signingKey != null && signingPassword != null) {
        sign(publishing.publications)
    }
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
        }
    }
}
