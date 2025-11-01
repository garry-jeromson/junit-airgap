package io.github.garryjeromson.junit.airgap.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Integration tests for ByteBuddy agent extraction using Gradle TestKit.
 *
 * These tests verify that:
 * 1. The ByteBuddy agent JAR is extracted correctly on first test run
 * 2. The agent is cached and reused on subsequent runs
 * 3. The agent is re-extracted when the cached version is outdated (size mismatch)
 * 4. The agent JAR contains the required MANIFEST attributes
 */
class BytebuddyAgentExtractionTest {
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
            rootProject.name = "bytebuddy-agent-extraction-test"
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

    @Test
    fun `ByteBuddy agent is extracted on first test run`() {
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

        // Task should complete (outcome may vary based on test discovery)
        val taskOutcome = result.task(":test")?.outcome
        assertTrue(
            taskOutcome == TaskOutcome.SUCCESS ||
                taskOutcome == TaskOutcome.NO_SOURCE ||
                taskOutcome == TaskOutcome.FAILED,
            "Test task should have completed (was $taskOutcome)",
        )

        // Verify ByteBuddy agent was extracted
        val agentFile = File(testProjectDir, "build/junit-airgap/bytebuddy/junit-airgap-bytebuddy-agent.jar")
        assertTrue(agentFile.exists(), "ByteBuddy agent should be extracted to build directory")
        assertTrue(agentFile.length() > 0, "Extracted ByteBuddy agent should have content")
    }

    @Test
    fun `ByteBuddy agent is reused from cache on subsequent runs`() {
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

        // Get agent file metadata
        val agentFile = File(testProjectDir, "build/junit-airgap/bytebuddy/junit-airgap-bytebuddy-agent.jar")
        val firstTimestamp = agentFile.lastModified()
        val firstSize = agentFile.length()

        // Wait to ensure different timestamp if re-extracted
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
            "ByteBuddy agent should be reused from cache (timestamp unchanged)",
        )
        assertEquals(
            firstSize,
            agentFile.length(),
            "ByteBuddy agent size should remain the same",
        )
    }

    @Test
    fun `ByteBuddy agent is re-extracted when cached version has wrong size`() {
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

        val agentFile = File(testProjectDir, "build/junit-airgap/bytebuddy/junit-airgap-bytebuddy-agent.jar")
        val correctSize = agentFile.length()

        // Corrupt the cached agent (simulate stale cache from old plugin version)
        agentFile.writeText("CORRUPTED OLD BYTEBUDDY AGENT")
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
            "ByteBuddy agent should be re-extracted to correct size",
        )
        assertNotEquals(
            corruptedSize,
            agentFile.length(),
            "ByteBuddy agent should not remain corrupted",
        )
    }

    @Test
    fun `ByteBuddy agent JAR contains required MANIFEST attributes`() {
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

        // Run test task to extract agent
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("test")
                .withPluginClasspath()
                .build()

        // Get extracted agent
        val agentFile = File(testProjectDir, "build/junit-airgap/bytebuddy/junit-airgap-bytebuddy-agent.jar")
        assertTrue(agentFile.exists(), "ByteBuddy agent should be extracted")

        // Read MANIFEST.MF from JAR
        val process =
            ProcessBuilder("unzip", "-p", agentFile.absolutePath, "META-INF/MANIFEST.MF")
                .redirectErrorStream(true)
                .start()

        val manifest = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()

        // Unfold manifest lines (remove line continuations starting with space)
        // JAR manifest spec: lines > 72 bytes are wrapped with \r\n<space>
        val unfoldedManifest = manifest.replace(Regex("\r?\n "), "")

        // Verify required MANIFEST attributes for Java agent
        assertTrue(
            unfoldedManifest.contains("Premain-Class:"),
            "MANIFEST should contain Premain-Class attribute",
        )
        assertTrue(
            unfoldedManifest.contains("io.github.garryjeromson.junit.airgap.bytebuddy.InetAddressBytebuddyAgent"),
            "Premain-Class should reference InetAddressBytebuddyAgent",
        )
        assertTrue(
            unfoldedManifest.contains("Can-Retransform-Classes: true"),
            "MANIFEST should allow class retransformation",
        )
    }

    @Test
    fun `multiple test tasks can share extracted ByteBuddy agent`() {
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
                filter {
                    isFailOnNoMatchingTests = false
                }
            }

            // Create a second test task that shares the same test sources
            tasks.register<Test>("integrationTest") {
                useJUnitPlatform()
                testClassesDirs = sourceSets["test"].output.classesDirs
                classpath = sourceSets["test"].runtimeClasspath
                filter {
                    isFailOnNoMatchingTests = false
                }
            }
            """.trimIndent(),
        )

        // Run both test tasks
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

        // Verify tasks ran
        assertTrue(result.task(":test") != null, "Test task should run")
        assertTrue(result.task(":integrationTest") != null, "Integration test task should run")

        // Verify only one ByteBuddy agent was extracted (shared between tasks)
        val agentFile = File(testProjectDir, "build/junit-airgap/bytebuddy/junit-airgap-bytebuddy-agent.jar")
        assertTrue(agentFile.exists(), "ByteBuddy agent should be extracted once and shared")
    }

    @Test
    fun `plugin applies javaagent JVM arg for ByteBuddy agent`() {
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
                // Print JVM args to verify agent is loaded
                doFirst {
                    println("TEST_JVM_ARGS: " + jvmArgs.joinToString(" "))
                }
            }
            """.trimIndent(),
        )

        // Run test task with --info to see JVM args
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("test", "--info")
                .withPluginClasspath()
                .build()

        val output = result.output

        // Verify ByteBuddy agent was extracted
        val agentFile = File(testProjectDir, "build/junit-airgap/bytebuddy/junit-airgap-bytebuddy-agent.jar")
        assertTrue(agentFile.exists(), "ByteBuddy agent should be extracted")

        // Verify -javaagent arg was applied with path to ByteBuddy agent
        assertTrue(
            output.contains("-javaagent:") &&
                output.contains("junit-airgap-bytebuddy-agent.jar"),
            "Test task should include -javaagent JVM arg for ByteBuddy agent. Output:\n$output",
        )

        // Verify the path in -javaagent points to the extracted agent
        val javaagentPattern = Regex("""-javaagent:([^\s]+junit-airgap-bytebuddy-agent\.jar)""")
        val match = javaagentPattern.find(output)
        assertTrue(match != null, "Should find -javaagent argument in output")

        val agentPathFromOutput = match!!.groupValues[1]
        assertTrue(
            File(agentPathFromOutput).exists(),
            "Agent path from -javaagent should point to existing file: $agentPathFromOutput",
        )
    }
}
