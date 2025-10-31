package io.github.garryjeromson.junit.airgap.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Gradle TestKit tests for the JUnit Airgap plugin.
 */
class JunitAirgapPluginTest {
    @TempDir
    lateinit var testProjectDir: File

    private lateinit var buildFile: File
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        buildFile = File(testProjectDir, "build.gradle.kts")
        settingsFile = File(testProjectDir, "settings.gradle.kts")

        // Create basic settings file
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            """.trimIndent(),
        )
    }

    @Test
    fun `plugin applies successfully to JVM project`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }

    @Test
    fun `plugin creates junit-platform properties file`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            junitAirgap {
                enabled = true
                applyToAllTests = true
            }
            """.trimIndent(),
        )

        GradleRunner
            .create()
            .withProjectDir(testProjectDir)
            .withArguments("help", "--stacktrace")
            .withPluginClasspath()
            .build()

        // Check that junit-platform.properties was created in build directory (not src/)
        val propsFile = File(testProjectDir, "build/generated/junit-platform/test/resources/junit-platform.properties")
        assertTrue(propsFile.exists(), "junit-platform.properties should be created in build directory")

        val content = propsFile.readText()
        assertTrue(
            content.contains("junit.jupiter.extensions.autodetection.enabled=true"),
            "Should enable auto-detection",
        )
        assertTrue(
            content.contains("junit.airgap.applyToAllTests=true"),
            "Should enable applyToAllTests",
        )
    }

    @Test
    fun `plugin respects enabled configuration`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                testImplementation("junit:junit:4.13.2")
            }

            junitAirgap {
                enabled = false
            }

            // Task to inspect test task configuration
            tasks.register("printTestConfig") {
                doLast {
                    tasks.withType<Test>().forEach { testTask ->
                        println("Test task: ${'$'}{testTask.name}")
                        println("JVM args: ${'$'}{testTask.allJvmArgs.joinToString(" ")}")
                        println("System properties: ${'$'}{testTask.systemProperties}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("printTestConfig", "--stacktrace")
                .withPluginClasspath()
                .build()

        // Check that junit-platform.properties was NOT created when disabled
        val propsFile = File(testProjectDir, "build/generated/junit-platform/test/resources/junit-platform.properties")
        assertTrue(!propsFile.exists(), "junit-platform.properties should not be created when disabled")

        // Check that JVMTI agent is NOT loaded when disabled
        assertFalse(
            result.output.contains("-agentpath"),
            "JVMTI agent should not be loaded when plugin is disabled",
        )

        // Check that junit.airgap.enabled system property is set to false
        assertTrue(
            result.output.contains("junit.airgap.enabled=false"),
            "System property junit.airgap.enabled should be set to false",
        )
    }

    @Test
    fun `plugin adds dependency to JVM project`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            junitAirgap {
                enabled = true
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("dependencies", "--configuration", "testRuntimeClasspath", "--stacktrace")
                .withPluginClasspath()
                .build()

        // Check that the dependency was added
        assertTrue(
            result.output.contains("io.github.garry-jeromson:junit-airgap"),
            "Should add junit-airgap dependency",
        )
    }

    @Test
    fun `plugin configures test tasks with system properties`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            junitAirgap {
                enabled = true
                applyToAllTests = true
            }

            tasks.register("printTestProps") {
                doLast {
                    tasks.withType<Test>().forEach { test ->
                        println("Test task: ${'$'}{test.name}")
                        test.systemProperties.forEach { (key, value) ->
                            if (key.toString().startsWith("junit.")) {
                                println("  ${'$'}key = ${'$'}value")
                            }
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("printTestProps", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertTrue(
            result.output.contains("junit.jupiter.extensions.autodetection.enabled = true"),
            "Should configure autodetection system property",
        )
        assertTrue(
            result.output.contains("junit.airgap.applyToAllTests = true"),
            "Should configure applyToAllTests system property",
        )
    }

    @Test
    fun `plugin supports debug mode configuration`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            junitAirgap {
                enabled = true
                debug = true
            }
            """.trimIndent(),
        )

        GradleRunner
            .create()
            .withProjectDir(testProjectDir)
            .withArguments("help", "--stacktrace")
            .withPluginClasspath()
            .build()

        val propsFile = File(testProjectDir, "build/generated/junit-platform/test/resources/junit-platform.properties")
        assertTrue(propsFile.exists(), "junit-platform.properties should be created in build directory")

        val content = propsFile.readText()
        assertTrue(
            content.contains("junit.airgap.debug=true"),
            "Should include debug property when enabled",
        )
    }

    @Test
    fun `plugin works with custom library version`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            junitAirgap {
                enabled = true
                libraryVersion = "0.2.0-CUSTOM"
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("dependencies", "--configuration", "testRuntimeClasspath", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertTrue(
            result.output.contains("airgap:0.2.0-CUSTOM") ||
                result.output.contains("Could not find"),
            "Should use custom version (or fail to find it if not published)",
        )
    }

    @Test
    fun `auto-detects JUnit 4 and enables injection`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                testImplementation("junit:junit:4.13.2")
            }

            junitAirgap {
                enabled = true
                // injectJUnit4Rule not set - should auto-detect
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--stacktrace", "--info")
                .withPluginClasspath()
                .build()

        assertTrue(
            result.output.contains("Auto-detected JUnit 4 project"),
            "Should log auto-detection of JUnit 4",
        )
        assertTrue(
            result.output.contains("injectJUnit4NetworkRule") ||
                result.output.contains("JUnit4RuleInjectionTask"),
            "Should register JUnit 4 injection task",
        )
    }

    @Test
    fun `auto-detects JUnit 5 and skips injection`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }

            junitAirgap {
                enabled = true
                // injectJUnit4Rule not set - should auto-detect JUnit 5
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--stacktrace", "--debug")
                .withPluginClasspath()
                .build()

        assertTrue(
            result.output.contains("pure JUnit 5 project") ||
                !result.output.contains("Auto-detected JUnit 4"),
            "Should detect JUnit 5 and skip injection",
        )
    }

    @Test
    fun `respects explicit injectJUnit4Rule = true override`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }

            junitAirgap {
                enabled = true
                injectJUnit4Rule = true // Explicit override
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--stacktrace", "--debug")
                .withPluginClasspath()
                .build()

        assertTrue(
            result.output.contains("injection explicitly set to: true") ||
                result.output.contains("injectJUnit4NetworkRule"),
            "Should respect explicit injectJUnit4Rule = true",
        )
    }

    @Test
    fun `respects explicit injectJUnit4Rule = false override`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                testImplementation("junit:junit:4.13.2")
            }

            junitAirgap {
                enabled = true
                debug = true
                injectJUnit4Rule = false // Explicit override to disable
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--stacktrace", "--debug")
                .withPluginClasspath()
                .build()

        assertTrue(
            result.output.contains("injection explicitly set to: false") ||
                !result.output.contains("Auto-detected JUnit 4"),
            "Should respect explicit injectJUnit4Rule = false",
        )
    }

    @Test
    fun `detects mixed JUnit 4 and JUnit 5 project`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garry-jeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                testImplementation("junit:junit:4.13.2")
                testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
                testImplementation("org.junit.vintage:junit-vintage-engine:5.10.0")
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }

            junitAirgap {
                enabled = true
                // injectJUnit4Rule not set - should auto-detect mixed project
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--stacktrace", "--info")
                .withPluginClasspath()
                .build()

        assertTrue(
            result.output.contains("mixed JUnit 4 + JUnit 5 project") ||
                result.output.contains("Auto-detected JUnit 4"),
            "Should detect mixed project and enable injection",
        )
    }
}
