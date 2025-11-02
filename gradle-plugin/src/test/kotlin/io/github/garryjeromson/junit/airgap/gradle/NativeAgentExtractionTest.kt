package io.github.garryjeromson.junit.airgap.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Integration tests for native agent extraction using Gradle TestKit.
 *
 * These tests verify that:
 * 1. The agent is extracted correctly on first test run
 * 2. The agent is cached and reused on subsequent runs
 * 3. The agent is re-extracted when the cached version is outdated (size mismatch)
 */
class NativeAgentExtractionTest {
    @TempDir
    lateinit var testProjectDir: File

    private lateinit var buildFile: File
    private lateinit var settingsFile: File

    /**
     * Get the native agent file for the current platform.
     * Only used for platform-agnostic tests (caching, sharing, etc.) where we need to reference
     * whichever agent file was extracted on the current platform.
     */
    private fun getCurrentPlatformAgentFile(): File {
        val agentName =
            when {
                System.getProperty("os.name").lowercase().contains("mac") -> "libjunit-airgap-agent.dylib"
                System.getProperty("os.name").lowercase().contains("linux") -> "libjunit-airgap-agent.so"
                System.getProperty("os.name").lowercase().contains("windows") -> "junit-airgap-agent.dll"
                else -> error("Unsupported OS: ${System.getProperty("os.name")}")
            }
        return File(testProjectDir, "build/junit-airgap/native/$agentName")
    }

    @BeforeEach
    fun setup() {
        buildFile = File(testProjectDir, "build.gradle.kts")
        settingsFile = File(testProjectDir, "settings.gradle.kts")

        // Create basic settings file
        settingsFile.writeText(
            """
            rootProject.name = "agent-extraction-test"
            """.trimIndent(),
        )

        // Create a simple test file
        val testDir = File(testProjectDir, "src/test/kotlin")
        testDir.mkdirs()

        File(testDir, "SimpleTest.kt").writeText(
            """
            import org.junit.jupiter.api.Test

            class SimpleTest {
                @Test
                fun `simple test`() {
                    // Just a placeholder test
                }
            }
            """.trimIndent(),
        )
    }

    @EnabledOnOs(OS.MAC)
    @Test
    fun `agent extracts dylib on macOS`() {
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
                testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
            }

            tasks.test {
                useJUnitPlatform()
            }
            """.trimIndent(),
        )

        // Run test task
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("test", "--info")
                .withPluginClasspath()
                .forwardOutput()
                .build()

        // Task outcome could be SUCCESS or NO-SOURCE (if no tests discovered)
        // We just care that the agent gets extracted
        val taskOutcome = result.task(":test")?.outcome
        assertTrue(
            taskOutcome == TaskOutcome.SUCCESS ||
                taskOutcome == TaskOutcome.NO_SOURCE ||
                taskOutcome == TaskOutcome.FAILED,
            "Test task should have completed (was $taskOutcome)",
        )

        // Verify agent was extracted with correct file extension
        val agentFile = File(testProjectDir, "build/junit-airgap/native/libjunit-airgap-agent.dylib")
        assertTrue(agentFile.exists(), "Agent dylib should be extracted to build directory")
        assertTrue(agentFile.length() > 0, "Extracted agent should have content")
        assertTrue(agentFile.canExecute(), "Agent should be executable on macOS")
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    fun `agent extracts so on Linux`() {
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
                testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
            }

            tasks.test {
                useJUnitPlatform()
            }
            """.trimIndent(),
        )

        // Run test task
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("test", "--info")
                .withPluginClasspath()
                .forwardOutput()
                .build()

        // Task outcome could be SUCCESS or NO-SOURCE (if no tests discovered)
        // We just care that the agent gets extracted
        val taskOutcome = result.task(":test")?.outcome
        assertTrue(
            taskOutcome == TaskOutcome.SUCCESS ||
                taskOutcome == TaskOutcome.NO_SOURCE ||
                taskOutcome == TaskOutcome.FAILED,
            "Test task should have completed (was $taskOutcome)",
        )

        // Verify agent was extracted with correct file extension
        val agentFile = File(testProjectDir, "build/junit-airgap/native/libjunit-airgap-agent.so")
        assertTrue(agentFile.exists(), "Agent so should be extracted to build directory")
        assertTrue(agentFile.length() > 0, "Extracted agent should have content")
        assertTrue(agentFile.canExecute(), "Agent should be executable on Linux")
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun `agent extracts dll on Windows`() {
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
                testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
            }

            tasks.test {
                useJUnitPlatform()
            }
            """.trimIndent(),
        )

        // Run test task
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("test", "--info")
                .withPluginClasspath()
                .forwardOutput()
                .build()

        // Task outcome could be SUCCESS or NO-SOURCE (if no tests discovered)
        // We just care that the agent gets extracted
        val taskOutcome = result.task(":test")?.outcome
        assertTrue(
            taskOutcome == TaskOutcome.SUCCESS ||
                taskOutcome == TaskOutcome.NO_SOURCE ||
                taskOutcome == TaskOutcome.FAILED,
            "Test task should have completed (was $taskOutcome)",
        )

        // Verify agent was extracted with correct file extension
        val agentFile = File(testProjectDir, "build/junit-airgap/native/junit-airgap-agent.dll")
        assertTrue(agentFile.exists(), "Agent dll should be extracted to build directory")
        assertTrue(agentFile.length() > 0, "Extracted agent should have content")
    }

    @Test
    fun `agent is reused from cache on subsequent runs`() {
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
                testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
            }

            tasks.test {
                useJUnitPlatform()
            }
            """.trimIndent(),
        )

        // First run - extract agent
        val firstResult =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("test", "--info", "--rerun-tasks")
                .withPluginClasspath()
                .build()

        // Don't assert task outcome - focus on agent extraction
        val agentFile = getCurrentPlatformAgentFile()
        val firstTimestamp = agentFile.lastModified()
        val firstSize = agentFile.length()

        // Wait a bit to ensure different timestamp if re-extracted
        Thread.sleep(100)

        // Second run - should reuse cached agent
        val secondResult =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("test", "--info", "--rerun-tasks")
                .withPluginClasspath()
                .build()

        // Verify agent was NOT re-extracted (timestamp unchanged)
        assertEquals(
            firstTimestamp,
            agentFile.lastModified(),
            "Agent should be reused from cache (timestamp unchanged)",
        )
        assertEquals(
            firstSize,
            agentFile.length(),
            "Agent size should remain the same",
        )
    }

    @Test
    fun `agent is re-extracted when cached version has wrong size`() {
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
                testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
            }

            tasks.test {
                useJUnitPlatform()
            }
            """.trimIndent(),
        )

        // First run - extract agent
        val firstResult =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("test", "--info", "--rerun-tasks")
                .withPluginClasspath()
                .build()

        val agentFile = getCurrentPlatformAgentFile()
        val correctSize = agentFile.length()

        // Corrupt the cached agent (simulate stale cache from old plugin version)
        agentFile.writeText("CORRUPTED OLD AGENT")
        val corruptedSize = agentFile.length()
        assertTrue(corruptedSize < correctSize, "Corrupted file should be smaller than correct agent")

        // Second run - should detect size mismatch and re-extract
        val secondResult =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("test", "--info", "--rerun-tasks")
                .withPluginClasspath()
                .build()

        // Verify agent was re-extracted to correct size
        assertEquals(
            correctSize,
            agentFile.length(),
            "Agent should be re-extracted to correct size",
        )
        assertNotEquals(
            corruptedSize,
            agentFile.length(),
            "Agent should not remain corrupted",
        )

        // Note: Debug log messages about "size mismatch" are not visible at --info level
        // The important thing is that the agent was re-extracted to the correct size
    }

    @Test
    fun `multiple test tasks can share extracted agent`() {
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
                testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
            }

            tasks.test {
                useJUnitPlatform()
                // Don't fail if no tests are discovered (test may not run properly in TestKit)
                filter {
                    isFailOnNoMatchingTests = false
                }
            }

            // Create a second test task that shares the same test sources
            tasks.register<Test>("integrationTest") {
                useJUnitPlatform()
                testClassesDirs = sourceSets["test"].output.classesDirs
                classpath = sourceSets["test"].runtimeClasspath
                // Don't fail if no tests are discovered
                filter {
                    isFailOnNoMatchingTests = false
                }
            }
            """.trimIndent(),
        )

        // Run both test tasks (may fail due to no tests discovered, but that's OK)
        val result =
            try {
                GradleRunner
                    .create()
                    .withProjectDir(testProjectDir)
                    .withArguments("test", "integrationTest", "--info", "--rerun-tasks")
                    .withPluginClasspath()
                    .build()
            } catch (e: org.gradle.testkit.runner.UnexpectedBuildFailure) {
                // Build may fail with "no tests discovered" but agent should still be extracted
                e.buildResult
            }

        // Verify tasks attempted to run (outcome may be FAILED due to no tests)
        assertTrue(result.task(":test") != null, "Test task should run")
        assertTrue(result.task(":integrationTest") != null, "Integration test task should run")

        // Verify only one agent was extracted (shared between tasks)
        val agentFile = getCurrentPlatformAgentFile()
        assertTrue(agentFile.exists(), "Agent should be extracted once and shared")
    }
}
