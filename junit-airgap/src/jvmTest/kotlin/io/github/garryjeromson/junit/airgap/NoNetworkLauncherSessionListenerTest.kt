package io.github.garryjeromson.junit.airgap

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.platform.launcher.LauncherSession

/**
 * Unit tests for AirgapLauncherSessionListener debug logging.
 */
class AirgapLauncherSessionListenerTest {
    @AfterEach
    fun cleanup() {
        // Restore default logger after each test
        DebugLogger.setTestInstance(null)
    }

    @Test
    fun `launcherSessionOpened logs opening message`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val listener = AirgapLauncherSessionListener()
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

        val listener = AirgapLauncherSessionListener()
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

        val listener = AirgapLauncherSessionListener()
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

        val listener = AirgapLauncherSessionListener()
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

    @Test
    fun `launcherSessionOpened logs messages in correct order`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val listener = AirgapLauncherSessionListener()
        val session =
            object : LauncherSession {
                override fun getLauncher() = throw UnsupportedOperationException()

                override fun close() {}
            }

        listener.launcherSessionOpened(session)

        // Verify we have at least 2 messages
        assertTrue(testLogger.messages.size >= 2) {
            "Expected at least 2 log messages, but got ${testLogger.messages.size}"
        }

        // First message should be the opening message
        assertTrue(testLogger.messages[0].contains("launcherSessionOpened() called")) {
            "First message should be opening message, but was: ${testLogger.messages[0]}"
        }

        // Second message should be either success or failure
        val secondMessage = testLogger.messages[1]
        val isSuccessOrFailure =
            secondMessage.contains("initialized successfully") ||
                secondMessage.contains("initialization failed")
        assertTrue(isSuccessOrFailure) {
            "Second message should indicate initialization result, but was: $secondMessage"
        }
    }

    @Test
    fun `launcherSessionOpened logs all messages to same logger instance`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val listener = AirgapLauncherSessionListener()
        val session =
            object : LauncherSession {
                override fun getLauncher() = throw UnsupportedOperationException()

                override fun close() {}
            }

        val initialMessageCount = testLogger.messages.size
        listener.launcherSessionOpened(session)

        // Verify new messages were added
        assertTrue(testLogger.messages.size > initialMessageCount) {
            "Expected new log messages to be added"
        }

        // All messages should be captured in the same logger
        assertTrue(testLogger.messages.size >= 2) {
            "Expected at least 2 messages total"
        }
    }
}
