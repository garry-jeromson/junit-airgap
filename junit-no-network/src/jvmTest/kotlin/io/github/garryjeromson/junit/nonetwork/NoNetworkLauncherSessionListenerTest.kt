package io.github.garryjeromson.junit.nonetwork

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.platform.launcher.LauncherSession

/**
 * Unit tests for NoNetworkLauncherSessionListener debug logging.
 */
class NoNetworkLauncherSessionListenerTest {
    @AfterEach
    fun cleanup() {
        // Restore default logger after each test
        DebugLogger.setTestInstance(null)
    }

    @Test
    fun `launcherSessionOpened logs opening message`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val listener = NoNetworkLauncherSessionListener()
        val session =
            object : LauncherSession {
                override fun getLauncher() = throw UnsupportedOperationException()

                override fun close() {}
            }

        listener.launcherSessionOpened(session)

        // Should log at least the opening message
        assertTrue(testLogger.messages.isNotEmpty())
        assertTrue(testLogger.messages[0].contains("LauncherSessionListener.launcherSessionOpened() called"))
    }

    @Test
    fun `launcherSessionOpened logs initialization success`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val listener = NoNetworkLauncherSessionListener()
        val session =
            object : LauncherSession {
                override fun getLauncher() = throw UnsupportedOperationException()

                override fun close() {}
            }

        listener.launcherSessionOpened(session)

        // Should log initialization success or failure
        assertTrue(testLogger.messages.size >= 2)
        assertTrue(
            testLogger.messages.any { it.contains("initialized successfully") } ||
                testLogger.messages.any { it.contains("initialization failed") },
        )
    }

    @Test
    fun `launcherSessionOpened logs initialization result`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val listener = NoNetworkLauncherSessionListener()
        val session =
            object : LauncherSession {
                override fun getLauncher() = throw UnsupportedOperationException()

                override fun close() {}
            }

        listener.launcherSessionOpened(session)

        // Verify we got at least the opening message
        assertTrue(testLogger.messages.isNotEmpty())

        // The second message depends on whether NetworkBlockerContext initialization succeeds or fails
        // If JVMTI agent is not loaded, it will fail gracefully
        // Both cases should be logged
        val hasSuccessLog = testLogger.messages.any { it.contains("NetworkBlockerContext initialized successfully") }
        val hasFailureLog = testLogger.messages.any { it.contains("NetworkBlockerContext initialization failed") }

        assertTrue(hasSuccessLog || hasFailureLog, "Should log either success or failure")
    }

    @Test
    fun `launcherSessionOpened handles initialization exception gracefully`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val listener = NoNetworkLauncherSessionListener()
        val session =
            object : LauncherSession {
                override fun getLauncher() = throw UnsupportedOperationException()

                override fun close() {}
            }

        // This should not throw even if initialization fails
        listener.launcherSessionOpened(session)

        // Should have at least one log message
        assertTrue(testLogger.messages.isNotEmpty())
    }
}
