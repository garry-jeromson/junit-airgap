package io.github.garryjeromson.junit.airgap

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for DebugLogger functionality.
 */
class DebugLoggerTest {
    @AfterEach
    fun cleanup() {
        // Restore default logger after each test
        DebugLogger.setTestInstance(null)
    }

    @Test
    fun `test logger captures debug messages`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val logger = DebugLogger.instance
        logger.debug { "Test message 1" }
        logger.debug { "Test message 2" }

        assertEquals(2, testLogger.messages.size)
        assertEquals("Test message 1", testLogger.messages[0])
        assertEquals("Test message 2", testLogger.messages[1])
    }

    @Test
    fun `test logger lazy evaluation`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val logger = DebugLogger.instance
        var evaluated = false
        logger.debug {
            evaluated = true
            "Lazy message"
        }

        assertTrue(evaluated, "Message lambda should be evaluated")
        assertEquals("Lazy message", testLogger.messages[0])
    }

    @Test
    fun `test logger clear method`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val logger = DebugLogger.instance
        logger.debug { "Message 1" }
        logger.debug { "Message 2" }

        assertEquals(2, testLogger.messages.size)

        testLogger.clear()

        assertEquals(0, testLogger.messages.size)
    }

    @Test
    fun `test logger instance changes with setTestInstance`() {
        val testLogger1 = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger1)

        val logger1 = DebugLogger.instance
        logger1.debug { "Message to logger 1" }

        assertEquals(1, testLogger1.messages.size)
        assertEquals("Message to logger 1", testLogger1.messages[0])

        val testLogger2 = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger2)

        val logger2 = DebugLogger.instance
        logger2.debug { "Message to logger 2" }

        // First logger should not have received second message
        assertEquals(1, testLogger1.messages.size)

        // Second logger should have received second message
        assertEquals(1, testLogger2.messages.size)
        assertEquals("Message to logger 2", testLogger2.messages[0])
    }

    @Test
    fun `test logger messages are immutable copies`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val logger = DebugLogger.instance
        logger.debug { "Message 1" }

        val messages1 = testLogger.messages
        assertEquals(1, messages1.size)

        logger.debug { "Message 2" }

        // Original list reference should not be modified
        assertEquals(1, messages1.size)

        // New call to messages should show updated list
        val messages2 = testLogger.messages
        assertEquals(2, messages2.size)
    }

    @Test
    fun `test multiple debug calls with string interpolation`() {
        val testLogger = TestDebugLogger()
        DebugLogger.setTestInstance(testLogger)

        val logger = DebugLogger.instance
        val host = "example.com"
        val port = 80
        val config = "test-config"

        logger.debug { "Connection to $host:$port" }
        logger.debug { "Configuration: $config" }

        assertEquals(2, testLogger.messages.size)
        assertEquals("Connection to example.com:80", testLogger.messages[0])
        assertEquals("Configuration: test-config", testLogger.messages[1])
    }

    @Test
    fun `test logger with debug enabled captures messages`() {
        // Create a custom debug logger that always has debug enabled
        val debugLogger =
            object : DebugLogger {
                val messages = mutableListOf<String>()

                override fun debug(message: () -> String) {
                    // Simulate what SystemPropertyDebugLogger does when debug is enabled
                    val msg = "[junit-no-network] ${message()}"
                    messages.add(msg)
                }
            }

        DebugLogger.setTestInstance(debugLogger)

        val logger = DebugLogger.instance
        logger.debug { "Test debug message" }

        assertEquals(1, debugLogger.messages.size)
        assertTrue(debugLogger.messages[0].contains("[junit-no-network]")) {
            "Output should contain [junit-no-network] prefix"
        }
        assertTrue(debugLogger.messages[0].contains("Test debug message")) {
            "Output should contain the debug message"
        }
    }
}
