package io.github.garryjeromson.junit.airgap

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.platform.launcher.LauncherSession

/**
 * Unit tests for AirgapLauncherSessionListener debug logging.
 */
class AirgapLauncherSessionListenerTest {
    private val mockSession =
        object : LauncherSession {
            override fun getLauncher() = throw UnsupportedOperationException()

            override fun close() {}
        }

    @AfterEach
    fun cleanup() {
        // Restore default logger after each test
        DebugLogger.setTestInstance(null)
        // Restore enabled property
        System.clearProperty(ENABLED_PROPERTY)
    }

    @Test
    fun `launcherSessionOpened logs opening message`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val listener = AirgapLauncherSessionListener()
        listener.launcherSessionOpened(mockSession)

        // Should log at least the opening message
        assertTrue(testLogger.messages.isNotEmpty())
        assertTrue(testLogger.messages[0].contains("LauncherSessionListener.launcherSessionOpened() called"))
    }

    @Test
    fun `launcherSessionOpened logs initialization success`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val listener = AirgapLauncherSessionListener()
        listener.launcherSessionOpened(mockSession)

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
        listener.launcherSessionOpened(mockSession)

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

        // This should not throw even if initialization fails
        listener.launcherSessionOpened(mockSession)

        // Should have at least one log message
        assertTrue(testLogger.messages.isNotEmpty())
    }

    @Test
    fun `launcherSessionOpened logs messages in correct order`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val listener = AirgapLauncherSessionListener()
        listener.launcherSessionOpened(mockSession)

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

        val initialMessageCount = testLogger.messages.size
        listener.launcherSessionOpened(mockSession)

        // Verify new messages were added
        assertTrue(testLogger.messages.size > initialMessageCount) {
            "Expected new log messages to be added"
        }

        // All messages should be captured in the same logger
        assertTrue(testLogger.messages.size >= 2) {
            "Expected at least 2 messages total"
        }
    }

    @Test
    fun `launcherSessionOpened skips initialization when disabled via system property`() {
        // Given: Plugin is disabled via system property
        System.setProperty(ENABLED_PROPERTY, "false")
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        // When: Opening launcher session
        val listener = AirgapLauncherSessionListener()
        listener.launcherSessionOpened(mockSession)

        // Then: Should log opening message and disabled message, but NOT initialization messages
        assertTrue(testLogger.messages.size >= 2) {
            "Expected at least 2 log messages (opening + disabled), but got ${testLogger.messages.size}"
        }
        assertTrue(testLogger.messages[0].contains("launcherSessionOpened() called")) {
            "First message should be opening message"
        }
        assertTrue(testLogger.messages[1].contains("JUnit Airgap is disabled")) {
            "Second message should indicate plugin is disabled"
        }
        assertTrue(testLogger.messages.none { it.contains("NetworkBlockerContext initialized") }) {
            "Should not attempt to initialize NetworkBlockerContext when disabled"
        }
    }

    @Test
    fun `launcherSessionOpened respects enabled equals true system property`() {
        // Given: Plugin is explicitly enabled via system property
        System.setProperty(ENABLED_PROPERTY, "true")
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        // When: Opening launcher session
        val listener = AirgapLauncherSessionListener()
        listener.launcherSessionOpened(mockSession)

        // Then: Should proceed with initialization (not log disabled message)
        assertTrue(testLogger.messages.none { it.contains("is disabled") }) {
            "Should not log disabled message when explicitly enabled"
        }
        assertTrue(
            testLogger.messages.any { it.contains("initialized successfully") } ||
                testLogger.messages.any { it.contains("initialization failed") },
        ) {
            "Should attempt initialization when enabled"
        }
    }
}
