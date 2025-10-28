package io.github.garryjeromson.junit.airgap.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import java.io.File

/**
 * Gradle plugin that automatically configures JUnit tests to block network requests.
 *
 * This plugin:
 * - Adds the junit-airgap library dependency to test configurations
 * - Creates junit-platform.properties for JUnit 5 automatic extension discovery
 * - Configures Test tasks with appropriate system properties
 * - Supports JVM, Android, and Kotlin Multiplatform projects
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     id("") version "0.1.0"
 * }
 *
 * junitAirgap {
 *     enabled = true
 *     applyToAllTests = true
 * }
 * ```
 */
class JunitAirgapPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create extension
        val extension = project.extensions.create<JunitAirgapExtension>("junitAirgap")

        // Configure Test tasks immediately (uses lazy .configureEach, so safe to call at apply time)
        // This must happen at apply time, NOT in afterEvaluate, for configuration cache compatibility
        configureTestTasks(project, extension)

        // Configure after project evaluation
        // Note: Only use afterEvaluate for operations that truly need project type detection
        // Task wiring must NOT happen here for configuration cache compatibility
        project.afterEvaluate {
            if (!extension.enabled.get()) {
                project.logger.info("JUnit Airgap plugin is disabled")
                return@afterEvaluate
            }

            configureProject(project, extension)
        }
    }

    private fun configureProject(
        project: Project,
        extension: JunitAirgapExtension,
    ) {
        if (extension.debug.get()) {
            project.logger.debug("Configuring JUnit Airgap plugin")
        }

        // 1. Add library dependency
        addDependencies(project, extension)

        // 2. Configure JUnit 4 rule injection (auto-detect or use explicit setting)
        if (shouldInjectJUnit4Rule(project, extension)) {
            configureJUnit4RuleInjection(project, extension)
        }

        // 4. Handle Kotlin Multiplatform projects
        if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            configureKmpProject(project, extension)
        } else {
            // 5. For non-KMP projects, configure JUnit Platform properties
            configureJunitPlatform(project, extension)
        }
    }

    /**
     * Determines whether to inject JUnit 4 @Rule fields into test classes.
     *
     * Uses hybrid detection strategy:
     * 1. If explicitly configured, use that value (override)
     * 2. Auto-detect by checking:
     *    - If test tasks use useJUnitPlatform() (indicates JUnit 5)
     *    - If junit:junit:4.x dependency is present (indicates JUnit 4)
     * 3. Decision logic:
     *    - Pure JUnit 5 → Don't inject
     *    - Pure JUnit 4 → Inject (auto-detected)
     *    - Mixed (JUnit Vintage) → Inject for JUnit 4 tests
     *    - Unknown → Don't inject (safe default)
     */
    private fun shouldInjectJUnit4Rule(
        project: Project,
        extension: JunitAirgapExtension,
    ): Boolean {
        // 1. Explicit configuration takes precedence
        if (extension.injectJUnit4Rule.isPresent) {
            val explicitValue = extension.injectJUnit4Rule.get()
            if (extension.debug.get()) {
                project.logger.debug("JUnit 4 injection explicitly set to: $explicitValue")
            }
            return explicitValue
        }

        // 2. Auto-detect: Check if any Test task uses JUnit Platform
        val usesJUnitPlatform =
            project.tasks.withType<Test>().any { testTask ->
                try {
                    // JUnit Platform is used if the test task has JUnitPlatformOptions
                    testTask.options is org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
                } catch (e: Exception) {
                    false
                }
            }

        // 3. Check for JUnit 4 and JUnit 5 dependencies
        val hasJUnit4 = hasJUnit4Dependency(project)
        val hasJUnit5 = hasJUnit5Dependency(project)

        // 4. Decision logic
        return when {
            // Pure JUnit 5 (with JUnit Platform) - no injection needed
            usesJUnitPlatform && hasJUnit5 && !hasJUnit4 -> {
                if (extension.debug.get()) {
                    project.logger.debug("Auto-detected pure JUnit 5 project - skipping @Rule injection")
                }
                false
            }

            // Pure JUnit 4 (no JUnit Platform) - needs injection
            !usesJUnitPlatform && hasJUnit4 -> {
                project.logger.info("Auto-detected JUnit 4 project - enabling @Rule injection")
                true
            }

            // Mixed (JUnit Vintage) - inject for JUnit 4 tests
            usesJUnitPlatform && hasJUnit4 -> {
                project.logger.info("Auto-detected mixed JUnit 4 + JUnit 5 project - enabling @Rule injection for JUnit 4 tests")
                true
            }

            // Unknown or no JUnit - don't inject (safe default)
            else -> {
                if (extension.debug.get()) {
                    project.logger.debug(
                        "Could not auto-detect JUnit version " +
                            "(usesJUnitPlatform=$usesJUnitPlatform, hasJUnit4=$hasJUnit4, hasJUnit5=$hasJUnit5) " +
                            "- skipping @Rule injection",
                    )
                }
                false
            }
        }
    }

    /**
     * Checks if the project has a JUnit 4 dependency.
     * Looks for org.junit:junit:4.x in test dependencies (without resolving configurations).
     *
     * This uses incoming.dependencies instead of resolving the configuration to avoid
     * configuration-time resolution issues.
     */
    private fun hasJUnit4Dependency(project: Project): Boolean {
        // Try different configuration names based on project type
        val configNames =
            listOf(
                "testImplementation", // Standard JVM
                "testRuntimeClasspath", // Standard JVM (fallback)
                "jvmTestImplementation", // KMP JVM target
                "testDebugImplementation", // Android
            )

        return configNames.any { configName ->
            try {
                val config = project.configurations.findByName(configName)
                // Check declared dependencies without resolving the configuration
                config?.allDependencies?.any { dep ->
                    dep.group == "junit" && dep.name == "junit"
                } ?: false
            } catch (e: Exception) {
                // Configuration might not exist
                false
            }
        }
    }

    /**
     * Checks if the project has a JUnit 5 (Jupiter) dependency.
     * Looks for org.junit.jupiter:junit-jupiter* in test dependencies (without resolving configurations).
     *
     * This uses incoming.dependencies instead of resolving the configuration to avoid
     * configuration-time resolution issues.
     */
    private fun hasJUnit5Dependency(project: Project): Boolean {
        // Try different configuration names based on project type
        val configNames =
            listOf(
                "testImplementation", // Standard JVM
                "testRuntimeClasspath", // Standard JVM (fallback)
                "jvmTestImplementation", // KMP JVM target
                "testDebugImplementation", // Android
            )

        return configNames.any { configName ->
            try {
                val config = project.configurations.findByName(configName)
                // Check declared dependencies without resolving the configuration
                config?.allDependencies?.any { dep ->
                    dep.group == "org.junit.jupiter" && dep.name?.startsWith("junit-jupiter") == true
                } ?: false
            } catch (e: Exception) {
                // Configuration might not exist
                false
            }
        }
    }

    private fun addDependencies(
        project: Project,
        extension: JunitAirgapExtension,
    ) {
        val version = extension.libraryVersion.get()

        // Determine which configurations to add the dependency to
        val configurations =
            when {
                project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
                    // KMP project - dependencies are handled per source set
                    project.logger.info("Detected KMP project, dependencies will be added per source set")
                    emptyList()
                }

                project.plugins.hasPlugin("com.android.library") ||
                    project.plugins.hasPlugin("com.android.application") -> {
                    // Android project
                    listOf("testImplementation")
                }

                else -> {
                    // Regular JVM project
                    listOf("testImplementation")
                }
            }

        configurations.forEach { configName ->
            project.configurations.findByName(configName)?.let { _ ->
                project.dependencies.add(
                    configName,
                    "io.github.garryjeromson:junit-airgap:$version",
                )
                project.logger.info("Added junit-airgap:$version to $configName")
            }
        }

        // For Android projects (non-KMP), also add the JVM variant for Robolectric support
        if (project.plugins.hasPlugin("com.android.library") || project.plugins.hasPlugin("com.android.application")) {
            project.configurations.findByName("testImplementation")?.let { _ ->
                try {
                    project.dependencies.add(
                        "testImplementation",
                        "io.github.garryjeromson:junit-airgap-jvm:$version",
                    )
                    project.logger.info("Added junit-airgap-jvm:$version to testImplementation for Robolectric support")
                } catch (e: Exception) {
                    project.logger.debug("Failed to add JVM dependency to testImplementation: ${e.message}")
                }
            }
        }
    }

    private fun configureJunitPlatform(
        project: Project,
        extension: JunitAirgapExtension,
    ) {
        // Generate junit-platform.properties in build directory (not src/)
        val generatedResourcesDir =
            project.layout.buildDirectory
                .dir(
                    "generated/junit-platform/test/resources",
                ).get()
                .asFile
        val propsFile = File(generatedResourcesDir, "junit-platform.properties")

        // Create directory if it doesn't exist
        generatedResourcesDir.mkdirs()

        // Generate properties content
        val properties =
            buildString {
                appendLine("# Generated by JUnit Airgap Gradle Plugin")
                appendLine("# Enable JUnit 5 automatic extension discovery")
                appendLine("junit.jupiter.extensions.autodetection.enabled=true")
                appendLine()
                appendLine("# Network blocking configuration")
                appendLine("junit.airgap.applyToAllTests=${extension.applyToAllTests.get()}")

                // Add allowedHosts if configured
                if (extension.allowedHosts.isPresent && extension.allowedHosts.get().isNotEmpty()) {
                    val hosts = extension.allowedHosts.get().joinToString(",")
                    appendLine("junit.airgap.allowedHosts=$hosts")
                }

                // Add blockedHosts if configured
                if (extension.blockedHosts.isPresent && extension.blockedHosts.get().isNotEmpty()) {
                    val hosts = extension.blockedHosts.get().joinToString(",")
                    appendLine("junit.airgap.blockedHosts=$hosts")
                }

                if (extension.debug.get()) {
                    appendLine("junit.airgap.debug=true")
                }
            }

        // Write properties file
        propsFile.writeText(properties)
        if (extension.debug.get()) {
            project.logger.debug("Generated junit-platform.properties at: ${propsFile.absolutePath}")
        }

        // Add generated resources to test source set
        addGeneratedResourcesToSourceSet(project, generatedResourcesDir, "test")
    }

    private fun addGeneratedResourcesToSourceSet(
        project: Project,
        resourcesDir: File,
        sourceSetName: String,
    ) {
        try {
            // Access source sets using reflection to avoid compile-time dependency
            val sourceSets = project.extensions.findByName("sourceSets")
            if (sourceSets != null) {
                val getByName = sourceSets.javaClass.getMethod("getByName", String::class.java)
                val sourceSet = getByName.invoke(sourceSets, sourceSetName)
                val resources = sourceSet.javaClass.getMethod("getResources").invoke(sourceSet)
                val srcDir = resources.javaClass.getMethod("srcDir", Any::class.java)
                srcDir.invoke(resources, resourcesDir)
                project.logger.info("Added generated resources to $sourceSetName source set")
            }
        } catch (e: Exception) {
            // Source sets might not exist in all project types - that's okay, system properties will still work
            project.logger.debug("Could not add generated resources to source set: ${e.message}")
        }
    }

    private fun configureTestTasks(
        project: Project,
        extension: JunitAirgapExtension,
    ) {
        project.tasks.withType<Test>().configureEach {
            // Enable JUnit Platform automatic extension detection
            systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")

            // Set system properties at configuration time (resolved lazily via Provider.get())
            // Note: These are also set again in doFirst to handle special cases, but setting them here
            // allows tests to inspect systemProperties at configuration time
            systemProperty("junit.airgap.applyToAllTests", extension.applyToAllTests.get().toString())
            systemProperty("junit.airgap.debug", extension.debug.get().toString())

            // Enable SecurityManager on Java 21+ (required for SECURITY_MANAGER and SECURITY_POLICY implementations)
            // On Java 21+, SecurityManager is deprecated and disabled by default
            // This JVM argument allows setting it: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/SecurityManager.html
            jvmArgs("-Djava.security.manager=allow")

            // Capture test task name and build directory for logging (configuration cache compatible)
            val testTaskName = name
            val buildDirectory = project.layout.buildDirectory.get().asFile

            // Extract native agent and set system properties at execution time (doFirst)
            // This ensures everything is resolved when the test JVM starts
            // Note: We use task's logger and captured build directory to avoid capturing project reference
            doFirst {
                // Set system properties with resolved values (must be done in doFirst for config cache)
                systemProperty("junit.airgap.applyToAllTests", extension.applyToAllTests.get().toString())
                systemProperty("junit.airgap.debug", extension.debug.get().toString())

                // Only set host lists if they have values
                val allowedHosts = extension.allowedHosts.get().joinToString(",")
                if (allowedHosts.isNotEmpty()) {
                    systemProperty("junit.airgap.allowedHosts", allowedHosts)
                }

                val blockedHosts = extension.blockedHosts.get().joinToString(",")
                if (blockedHosts.isNotEmpty()) {
                    systemProperty("junit.airgap.blockedHosts", blockedHosts)
                }

                val agentPath = NativeAgentExtractor.getAgentPath(
                    buildDirectory, // Use captured build directory
                    logger, // Use task's logger
                    extension.debug.get()
                )

                if (agentPath != null) {
                    // Add debug option if debug mode is enabled
                    val agentArg =
                        if (extension.debug.get()) {
                            "-agentpath:$agentPath=debug"
                        } else {
                            "-agentpath:$agentPath"
                        }
                    jvmArgs(agentArg)
                    if (extension.debug.get()) {
                        logger.debug("Loading JVMTI agent from: $agentPath")
                    }
                } else {
                    logger.warn(
                        "JVMTI agent not available for test task '$testTaskName'. " +
                            "Network blocking may not work on this platform.",
                    )
                }
            }
        }
    }

    private fun configureKmpProject(
        project: Project,
        extension: JunitAirgapExtension,
    ) {
        val version = extension.libraryVersion.get()

        project.logger.info("Configuring KMP project for JUnit Airgap")

        // For KMP projects, add dependencies using platform-specific configuration names
        // These configurations are created by the Kotlin Multiplatform plugin
        val kmpTestConfigurations =
            listOf(
                "jvmTestImplementation",
                "androidUnitTestImplementation",
            )

        kmpTestConfigurations.forEach { configName ->
            project.configurations.findByName(configName)?.let { _ ->
                try {
                    project.dependencies.add(
                        configName,
                        "io.github.garryjeromson:junit-airgap:$version",
                    )
                    project.logger.info("Added junit-airgap:$version to $configName")
                } catch (e: Exception) {
                    project.logger.debug("Failed to add dependency to $configName: ${e.message}")
                }
            } ?: project.logger.debug("Configuration $configName not found")
        }

        // For Android unit tests (Robolectric), also add the JVM variant
        // Robolectric runs tests on the JVM, so it needs access to JVM-specific classes like NetworkBlockerContext
        project.configurations.findByName("androidUnitTestImplementation")?.let { _ ->
            try {
                project.dependencies.add(
                    "androidUnitTestImplementation",
                    "io.github.garryjeromson:junit-airgap-jvm:$version",
                )
                project.logger.info("Added junit-airgap-jvm:$version to androidUnitTestImplementation for Robolectric support")
            } catch (e: Exception) {
                project.logger.debug("Failed to add JVM dependency to androidUnitTestImplementation: ${e.message}")
            }
        }

        // Note: We don't generate junit-platform.properties files for KMP projects
        // because system properties configured on Test tasks are sufficient and
        // avoid duplicate resource issues
        project.logger.info("Configuration complete - using system properties for KMP test configuration")
    }

    private fun createJunitPlatformProperties(
        project: Project,
        sourceSetName: String,
        extension: JunitAirgapExtension,
    ) {
        // Generate in build directory, not src/
        val generatedResourcesDir =
            project.layout.buildDirectory
                .dir(
                    "generated/junit-platform/$sourceSetName/resources",
                ).get()
                .asFile
        val propsFile = File(generatedResourcesDir, "junit-platform.properties")

        generatedResourcesDir.mkdirs()

        val properties =
            buildString {
                appendLine("# Generated by JUnit Airgap Gradle Plugin")
                appendLine("junit.jupiter.extensions.autodetection.enabled=true")
                appendLine("junit.airgap.applyToAllTests=${extension.applyToAllTests.get()}")

                // Add allowedHosts if configured
                if (extension.allowedHosts.isPresent && extension.allowedHosts.get().isNotEmpty()) {
                    val hosts = extension.allowedHosts.get().joinToString(",")
                    appendLine("junit.airgap.allowedHosts=$hosts")
                }

                // Add blockedHosts if configured
                if (extension.blockedHosts.isPresent && extension.blockedHosts.get().isNotEmpty()) {
                    val hosts = extension.blockedHosts.get().joinToString(",")
                    appendLine("junit.airgap.blockedHosts=$hosts")
                }
            }

        propsFile.writeText(properties)
        // Note: extension is not available in this method, so we skip debug logging here
        project.logger.debug("Generated junit-platform.properties for $sourceSetName at: ${propsFile.absolutePath}")

        // Add generated resources to the KMP source set
        addGeneratedResourcesToKmpSourceSet(project, generatedResourcesDir, sourceSetName)
    }

    private fun addGeneratedResourcesToKmpSourceSet(
        project: Project,
        resourcesDir: File,
        sourceSetName: String,
    ) {
        try {
            val kotlin = project.extensions.findByName("kotlin") ?: return
            val sourceSets = kotlin.javaClass.getMethod("getSourceSets").invoke(kotlin)
            val getByName = sourceSets.javaClass.getMethod("getByName", String::class.java)
            val sourceSet = getByName.invoke(sourceSets, sourceSetName)
            val resources = sourceSet.javaClass.getMethod("getResources").invoke(sourceSet)
            val srcDir = resources.javaClass.getMethod("srcDir", Any::class.java)
            srcDir.invoke(resources, resourcesDir)
            project.logger.info("Added generated resources to $sourceSetName KMP source set")
        } catch (e: Exception) {
            project.logger.debug("Could not add generated resources to KMP source set $sourceSetName: ${e.message}")
        }
    }

    private fun configureJUnit4RuleInjection(
        project: Project,
        extension: JunitAirgapExtension,
    ) {
        if (extension.debug.get()) {
            project.logger.debug("Configuring JUnit 4 @Rule injection via bytecode enhancement")
        }

        // Detect project type and configure accordingly
        val hasKmpPlugin = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
        val hasAndroidLibrary = project.plugins.hasPlugin("com.android.library")
        val hasAndroidApp = project.plugins.hasPlugin("com.android.application")

        if (extension.debug.get()) {
            project.logger.debug(
                "Plugin detection: KMP=$hasKmpPlugin, AndroidLib=$hasAndroidLibrary, AndroidApp=$hasAndroidApp",
            )
        }

        when {
            hasKmpPlugin -> {
                if (extension.debug.get()) {
                    project.logger.debug("Detected KMP project - configuring KMP injection")
                }
                configureKmpJUnit4Injection(project, extension)
            }
            hasAndroidLibrary || hasAndroidApp -> {
                if (extension.debug.get()) {
                    project.logger.debug("Detected Android project - configuring Android injection")
                }
                configureAndroidJUnit4Injection(project, extension)
            }
            else -> {
                if (extension.debug.get()) {
                    project.logger.debug("Detected JVM project - configuring JVM injection")
                }
                configureJvmJUnit4Injection(project, extension)
            }
        }
    }

    private fun configureJvmJUnit4Injection(
        project: Project,
        extension: JunitAirgapExtension,
    ) {
        // Register injection task for JVM project
        val injectionTask = project.tasks.register("injectJUnit4NetworkRule", JUnit4RuleInjectionTask::class.java) {
            testClassesDir.set(project.layout.buildDirectory.dir("classes/kotlin/test"))
            debug.set(extension.debug)
        }

        // Wire test classpath and configure task dependencies in a nested afterEvaluate
        // to ensure all tasks exist before we reference them
        project.afterEvaluate {
            // Wire test classpath using named() directly instead of .configure() to avoid task realization issues
            project.tasks.named("injectJUnit4NetworkRule", JUnit4RuleInjectionTask::class.java) {
                testClasspath.setFrom(
                    project.tasks.named("test", org.gradle.api.tasks.testing.Test::class.java)
                        .flatMap { it.classpath.elements }
                )
            }

            // Configure task wiring using lazy approach
            configureTaskWiring(project, "compileTestKotlin", "injectJUnit4NetworkRule")
            configureTaskWiring(project, "compileTestJava", "injectJUnit4NetworkRule")
            configureTaskWiring(project, "test", null, "injectJUnit4NetworkRule")
        }

        project.logger.info("Configured JUnit 4 rule injection for JVM project")
    }

    private fun configureAndroidJUnit4Injection(
        project: Project,
        extension: JunitAirgapExtension,
    ) {
        // Configure injection for both Debug and Release variants
        listOf("Debug", "Release").forEach { variant ->
            configureAndroidVariantInjection(project, extension, variant)
        }

        project.logger.info("Configured JUnit 4 rule injection for Android project (Debug and Release variants)")
    }

    private fun configureAndroidVariantInjection(
        project: Project,
        extension: JunitAirgapExtension,
        variant: String,
    ) {
        val variantLower = variant.lowercase()
        val taskName = "inject${variant}JUnit4NetworkRule"
        val testTaskName = "test${variant}UnitTest"
        val compilationTaskName = "compile${variant}UnitTestKotlin"

        // Register injection task for this variant
        val injectionTask = project.tasks.register(taskName, JUnit4RuleInjectionTask::class.java) {
            testClassesDir.set(project.layout.buildDirectory.dir("tmp/kotlin-classes/${variantLower}UnitTest"))
            debug.set(extension.debug)
        }

        // Configure automatic task wiring after project evaluation
        project.afterEvaluate {
            // Wire test classpath using named() directly instead of .configure() to avoid task realization issues
            project.tasks.named(taskName, JUnit4RuleInjectionTask::class.java) {
                testClasspath.setFrom(
                    project.tasks.named(testTaskName, org.gradle.api.tasks.testing.Test::class.java)
                        .flatMap { it.classpath.elements }
                )
            }

            // Wire compilation task to finalize with injection
            configureTaskWiring(this, compilationTaskName, taskName)
            // Wire test task to depend on injection
            configureTaskWiring(this, testTaskName, null, taskName)
        }

        project.logger.debug("Configured injection task $taskName for Android $variant variant")
    }

    private fun configureKmpJUnit4Injection(
        project: Project,
        extension: JunitAirgapExtension,
    ) {
        // For KMP, we need to configure injection for each target platform
        // JVM target
        val jvmInjectionTask = project.tasks.register("injectJvmJUnit4NetworkRule", JUnit4RuleInjectionTask::class.java) {
            testClassesDir.set(project.layout.buildDirectory.dir("classes/kotlin/jvm/test"))
            debug.set(extension.debug)

            // Depend on compilation task
            project.tasks.findByName("compileTestKotlinJvm")?.let { compileTask ->
                mustRunAfter(compileTask)
                dependsOn(compileTask)
            }
        }

        // Android target - configure both Debug and Release variants
        listOf("Debug", "Release").forEach { variant ->
            configureKmpAndroidVariantInjection(project, extension, variant)
        }

        // Configure automatic task wiring after project evaluation
        project.afterEvaluate {
            // Wire test classpath for JVM target using named() directly instead of .configure()
            project.tasks.named("injectJvmJUnit4NetworkRule", JUnit4RuleInjectionTask::class.java) {
                testClasspath.setFrom(
                    project.tasks.named("jvmTest", org.gradle.api.tasks.testing.Test::class.java)
                        .flatMap { it.classpath.elements }
                )
            }

            // Wire JVM target tasks
            configureTaskWiring(project, "compileTestKotlinJvm", "injectJvmJUnit4NetworkRule")
            configureTaskWiring(project, "jvmTest", null, "injectJvmJUnit4NetworkRule")

            // Wire Android target tasks for both variants
            listOf("Debug", "Release").forEach { variant ->
                val injectionTaskName = "injectAndroid${variant}JUnit4NetworkRule"
                val compilationTaskName = "compile${variant}UnitTestKotlinAndroid"
                val testTaskName = "test${variant}UnitTest"

                // Wire test classpath for Android variant using named() directly instead of .configure()
                project.tasks.named(injectionTaskName, JUnit4RuleInjectionTask::class.java) {
                    testClasspath.setFrom(
                        project.tasks.named(testTaskName, org.gradle.api.tasks.testing.Test::class.java)
                            .flatMap { it.classpath.elements }
                    )
                }

                configureTaskWiring(project, compilationTaskName, injectionTaskName)
                configureTaskWiring(project, testTaskName, null, injectionTaskName)
            }
        }

        project.logger.info(
            "Configured JUnit 4 rule injection for KMP project with automatic task wiring (Debug and Release variants)",
        )
    }

    private fun configureKmpAndroidVariantInjection(
        project: Project,
        extension: JunitAirgapExtension,
        variant: String,
    ) {
        val variantLower = variant.lowercase()
        val taskName = "injectAndroid${variant}JUnit4NetworkRule"
        val testTaskName = "test${variant}UnitTest"
        val compilationTaskName = "compile${variant}UnitTestKotlinAndroid"

        project.tasks.register(taskName, JUnit4RuleInjectionTask::class.java) {
            testClassesDir.set(project.layout.buildDirectory.dir("tmp/kotlin-classes/${variantLower}UnitTest"))
            debug.set(extension.debug)

            // Depend on compilation task
            project.tasks.findByName(compilationTaskName)?.let { compileTask ->
                mustRunAfter(compileTask)
                dependsOn(compileTask)
            }
        }

        project.logger.debug("Configured injection task $taskName for KMP Android $variant variant (classpath will be wired in afterEvaluate)")
    }

    private fun configureTaskWiring(
        project: Project,
        taskName: String,
        finalizedByTask: String? = null,
        dependsOnTask: String? = null,
    ) {
        try {
            project.tasks.named(taskName) {
                finalizedByTask?.let { finalizedBy(it) }
                dependsOnTask?.let { dependsOn(it) }
            }
            project.logger.debug("Configured task wiring for $taskName")
        } catch (e: Exception) {
            project.logger.debug("Task $taskName not found, skipping wiring: ${e.message}")
        }
    }
}
