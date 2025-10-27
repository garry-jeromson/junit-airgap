package io.github.garryjeromson.junit.airgap

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for JvmtiNetworkBlocker debug logging.
 */
class JvmtiNetworkBlockerTest {
    @AfterEach
    fun cleanup() {
        // Restore default logger after each test
        DebugLogger.setTestInstance(null)
    }

    @Test
    fun `install logs debug messages`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val configuration =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        val blocker = JvmtiNetworkBlocker(configuration)

        blocker.install()

        // Should log two messages
        assertEquals(2, testLogger.messages.size)
        assertTrue(testLogger.messages[0].contains("JvmtiNetworkBlocker: Installed for thread"))
        assertTrue(testLogger.messages[1].contains("NOTE: JVMTI agent must be loaded via -agentpath"))
    }

    @Test
    fun `install logs thread name`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val configuration =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        val blocker = JvmtiNetworkBlocker(configuration)

        blocker.install()

        val currentThreadName = Thread.currentThread().name
        assertTrue(testLogger.messages[0].contains(currentThreadName))
    }

    @Test
    fun `uninstall logs debug message`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val configuration =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        val blocker = JvmtiNetworkBlocker(configuration)

        // Install first
        blocker.install()
        testLogger.clear()

        // Now uninstall
        blocker.uninstall()

        assertEquals(1, testLogger.messages.size)
        assertTrue(testLogger.messages[0].contains("JvmtiNetworkBlocker: Uninstalled for thread"))
    }

    @Test
    fun `uninstall logs thread name`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val configuration =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        val blocker = JvmtiNetworkBlocker(configuration)

        blocker.install()
        testLogger.clear()

        blocker.uninstall()

        val currentThreadName = Thread.currentThread().name
        assertTrue(testLogger.messages[0].contains(currentThreadName))
    }

    @Test
    fun `install twice only logs once`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val configuration =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        val blocker = JvmtiNetworkBlocker(configuration)

        blocker.install()
        val messagesAfterFirstInstall = testLogger.messages.size

        blocker.install() // Second install should be no-op

        // Should still have same number of messages
        assertEquals(messagesAfterFirstInstall, testLogger.messages.size)
    }

    @Test
    fun `uninstall before install does not log`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val configuration =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        val blocker = JvmtiNetworkBlocker(configuration)

        blocker.uninstall() // Uninstall without install

        // Should not log anything
        assertEquals(0, testLogger.messages.size)
    }

    @Test
    fun `isAvailable always returns true`() {
        val configuration =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        val blocker = JvmtiNetworkBlocker(configuration)

        // isAvailable should always return true (graceful degradation)
        assertTrue(blocker.isAvailable())
    }
}
