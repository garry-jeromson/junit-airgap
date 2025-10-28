package io.github.garryjeromson.junit.airgap.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
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
                id("io.github.garryjeromson.junit-airgap")
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
                id("io.github.garryjeromson.junit-airgap")
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
                id("io.github.garryjeromson.junit-airgap")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            junitAirgap {
                enabled = false
            }
            """.trimIndent(),
        )

        GradleRunner
            .create()
            .withProjectDir(testProjectDir)
            .withArguments("help", "--stacktrace")
            .withPluginClasspath()
            .build()

        // Check that junit-platform.properties was NOT created when disabled
        val propsFile = File(testProjectDir, "build/generated/junit-platform/test/resources/junit-platform.properties")
        assertTrue(!propsFile.exists(), "junit-platform.properties should not be created when disabled")
    }

    @Test
    fun `plugin adds dependency to JVM project`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garryjeromson.junit-airgap")
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
            result.output.contains("io.github.garryjeromson:junit-airgap"),
            "Should add junit-airgap dependency",
        )
    }

    @Test
    fun `plugin configures test tasks with system properties`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                id("io.github.garryjeromson.junit-airgap")
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
                id("io.github.garryjeromson.junit-airgap")
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
                id("io.github.garryjeromson.junit-airgap")
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
}
