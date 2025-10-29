plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.0"
    alias(libs.plugins.maven.publish)
    id("junit-extensions.kotlin-common-convention")
    alias(libs.plugins.kover)
}

gradlePlugin {
    // Plugin Portal configuration
    website.set("https://github.com/garry-jeromson/junit-airgap")
    vcsUrl.set("https://github.com/garry-jeromson/junit-airgap.git")

    plugins {
        create("junitAirgapPlugin") {
            id = "io.github.garry-jeromson.junit-airgap"
            implementationClass = "io.github.garryjeromson.junit.airgap.gradle.JunitAirgapPlugin"
            displayName = "JUnit Airgap Plugin"
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
        // Suppress Beta warning for expect/actual classes (KMP standard pattern from junit-airgap dependency)
        // Suppress experimental language version warning (Gradle 9.1.0 uses Kotlin 2.2.0)
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xsuppress-version-warnings",
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

// ============================================================================
// Native Agent Packaging
// ============================================================================

// Task to copy the native agent from the junit-airgap build to plugin resources
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
            "darwin" -> "libjunit-airgap-agent.dylib"
            "linux" -> "libjunit-airgap-agent.so"
            "windows" -> "junit-airgap-agent.dll"
            else -> {
                logger.warn("Unknown OS: $os")
                return@register
            }
        }

    // Source: built agent from junit-airgap module
    // Use layout.projectDirectory navigation for configuration cache compatibility
    // gradle-plugin is a sibling of junit-airgap, so go up to parent, then into native/build
    from(layout.projectDirectory.dir("../native/build")) {
        include(agentFileName)
    }

    // Destination: plugin resources by platform
    into("src/main/resources/native/$os-$arch")

    // Depend on the native build task from junit-airgap
    dependsOn(":junit-airgap:buildNativeAgent")

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
                    "Run ':junit-airgap:buildNativeAgent' first.",
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

// Maven Central Publishing Configuration (Central Portal API)
// Note: Gradle Plugin Portal publishing is handled by the plugin-publish plugin above
mavenPublishing {
    // Publish to Central Portal with automatic release after validation
    publishToMavenCentral(automaticRelease = true)

    // Sign all publications with GPG (only when signing credentials are available)
    // Skip signing for publishToMavenLocal to allow tests without GPG setup
    val hasSigningKey = providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey")
        .orElse(providers.gradleProperty("signingInMemoryKey"))
        .isPresent
    if (hasSigningKey) {
        signAllPublications()
    }

    // POM metadata for Maven Central
    pom {
        name.set("JUnit Airgap Gradle Plugin")
        description.set("Gradle plugin for automatically configuring JUnit tests to block network requests")
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

    // Coordinates
    coordinates("io.github.garry-jeromson", "junit-airgap-gradle-plugin", version.toString())
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
