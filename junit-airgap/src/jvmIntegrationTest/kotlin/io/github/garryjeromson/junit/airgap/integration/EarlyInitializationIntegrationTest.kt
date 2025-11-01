package io.github.garryjeromson.junit.airgap.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests that verify the JVMTI agent handles early JVM initialization correctly.
 *
 * These tests simulate IntelliJ IDEA's test runner behavior, which loads classes eagerly
 * before VM_INIT fires. This caused "platform encoding not initialized" errors that were
 * fixed by implementing deferred wrapper installation.
 *
 * ## Background
 *
 * **Normal JVM Initialization (Gradle CLI)**:
 * 1. Agent loads (Agent_OnLoad)
 * 2. VM_INIT callback fires
 * 3. Platform encoding initializes
 * 4. Classes load
 * 5. Native methods bind
 * 6. Wrappers installed
 *
 * **IntelliJ IDEA Initialization**:
 * 1. Agent loads (Agent_OnLoad)
 * 2. Classes load eagerly (for test discovery)
 * 3. Native methods bind ← **BEFORE VM_INIT**
 * 4. VM_INIT callback fires
 * 5. Platform encoding initializes
 * 6. Deferred wrappers installed
 *
 * The agent now stores original function pointers during NativeMethodBind and installs
 * wrappers later during VM_INIT using JNI RegisterNatives().
 *
 * ## What These Tests Verify
 *
 * 1. No "platform encoding not initialized" errors when classes load early
 * 2. VM_INIT callback fires and installs deferred wrappers
 * 3. Network blocking works correctly after deferred installation
 * 4. Debug logging shows correct initialization sequence
 */
class EarlyInitializationIntegrationTest {
    private val javaHome = System.getProperty("java.home")
    private val agentPath = File("../native/build/libjunit-airgap-agent.dylib").absolutePath
    private val testClasspath = System.getProperty("java.class.path")

    @Test
    @EnabledOnOs(OS.MAC)
    fun `agent handles early class loading without platform encoding errors`() {
        val result =
            runTestSubprocess(
                mainClass = "io.github.garryjeromson.junit.airgap.integration.fixtures.EarlyClassLoadingMainKt",
                enableDebug = true,
            )

        // 1. Verify no platform encoding errors
        assertFalse(
            actual = result.stderr.contains("platform encoding not initialized"),
            message = "Should not have platform encoding errors. stderr:\n${result.stderr}",
        )

        // 2. Verify VM_INIT callback fired
        assertTrue(
            actual = result.stderr.contains("VM_INIT callback"),
            message =
                "VM_INIT callback should fire.\n" +
                    "stdout:\n${result.stdout}\n" +
                    "stderr:\n${result.stderr}\n" +
                    "exit code: ${result.exitCode}",
        )

        // 3. Verify wrappers were installed (either deferred during VM_INIT or immediately after)
        // The timing depends on when classes are loaded relative to VM_INIT
        assertTrue(
            actual =
                result.stderr.contains("Installing deferred wrapper") ||
                    result.stderr.contains("Successfully installed wrapper") ||
                    result.stderr.contains("Installing wrapper") ||
                    result.stderr.contains("Installed wrapper"),
            message = "Wrappers should be installed. stderr:\n${result.stderr}",
        )

        // 4. Verify network blocking works correctly
        assertTrue(
            actual = result.stdout.contains("Network request blocked as expected"),
            message = "Network requests should be blocked. stdout:\n${result.stdout}",
        )

        assertEquals(
            expected = 0,
            actual = result.exitCode,
            message = "Process should exit successfully",
        )
    }

    @Test
    @EnabledOnOs(OS.MAC)
    fun `agent handles Gradle worker initialization without errors`() {
        // This test simulates Gradle worker process initialization
        // where some networking may occur before test classes load
        val result =
            runTestSubprocess(
                mainClass = "io.github.garryjeromson.junit.airgap.integration.fixtures.GradleWorkerSimulationKt",
                enableDebug = true,
            )

        // Verify no encoding errors during worker initialization
        assertFalse(
            actual = result.stderr.contains("platform encoding not initialized"),
            message = "Worker initialization should not cause encoding errors",
        )

        // Verify successful completion
        assertEquals(
            expected = 0,
            actual = result.exitCode,
            message = "Worker simulation should complete successfully",
        )
    }

    @Test
    @EnabledOnOs(OS.MAC)
    fun `agent initialization sequence is logged correctly in debug mode`() {
        val result =
            runTestSubprocess(
                mainClass = "io.github.garryjeromson.junit.airgap.integration.fixtures.EarlyClassLoadingMainKt",
                enableDebug = true,
            )

        val stderr = result.stderr

        // Verify expected initialization sequence in debug log
        val agentLoadingIndex = stderr.indexOf("JVMTI Agent loading")
        val vmInitIndex = stderr.indexOf("VM_INIT callback")
        val wrapperInstallIndex = stderr.indexOf("Installing deferred wrapper")

        assertTrue(
            actual = agentLoadingIndex >= 0,
            message = "Should log agent loading",
        )

        assertTrue(
            actual = vmInitIndex >= 0,
            message = "Should log VM_INIT callback",
        )

        if (wrapperInstallIndex >= 0) {
            // If deferred installation occurred, verify order
            assertTrue(
                actual = agentLoadingIndex < vmInitIndex,
                message = "Agent should load before VM_INIT",
            )

            assertTrue(
                actual = vmInitIndex < wrapperInstallIndex,
                message = "Wrappers should be installed after VM_INIT",
            )
        }
    }

    /**
     * Run a test subprocess with the JVMTI agent loaded.
     *
     * @param mainClass Fully qualified main class name to execute
     * @param enableDebug Whether to enable debug logging
     * @return Process execution result
     */
    private fun runTestSubprocess(
        mainClass: String,
        enableDebug: Boolean = false,
    ): SubprocessResult {
        val agentArg =
            if (enableDebug) {
                "-agentpath:$agentPath=debug"
            } else {
                "-agentpath:$agentPath"
            }

        val processBuilder =
            ProcessBuilder(
                "$javaHome/bin/java",
                agentArg,
                "-cp",
                testClasspath,
                mainClass,
            ).apply {
                // Inherit environment to get JAVA_HOME and other necessary variables
                environment().putAll(System.getenv())
            }

        val process = processBuilder.start()

        // Capture stdout and stderr
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        // Wait for process to complete (with timeout)
        val completed = process.waitFor(30, TimeUnit.SECONDS)

        if (!completed) {
            process.destroyForcibly()
            throw AssertionError(
                "Process timed out after 30 seconds.\n" +
                    "stdout:\n$stdout\n" +
                    "stderr:\n$stderr",
            )
        }

        return SubprocessResult(
            stdout = stdout,
            stderr = stderr,
            exitCode = process.exitValue(),
        )
    }

    /**
     * Result of a subprocess execution.
     */
    private data class SubprocessResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
    )
}
