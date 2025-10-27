package io.github.garryjeromson.junit.airgap

/**
 * Internal debug logger for junit-no-network.
 *
 * This logger provides a testable abstraction over debug logging, allowing:
 * - Single source of truth for debug mode check
 * - Lazy evaluation of log messages (zero overhead when debug disabled)
 * - Injectable test logger for verifying debug output
 * - Clean separation between logging and business logic
 *
 * ## Usage
 * ```kotlin
 * private val logger = DebugLogger.instance
 *
 * logger.debug { "Connection attempt: $host:$port" }
 * logger.debug { "Configuration: $config" }
 * ```
 *
 * ## Testing
 * ```kotlin
 * val testLogger = TestDebugLogger()
 * DebugLogger.setTestInstance(testLogger)
 * try {
 *     // Run code that logs
 *     assertEquals("Expected message", testLogger.messages[0])
 * } finally {
 *     DebugLogger.setTestInstance(null)
 * }
 * ```
 *
 * ## Debug Mode
 * Enabled via system property: `-Djunit.airgap.debug=true`
 */
internal interface DebugLogger {
    /**
     * Log a debug message if debug mode is enabled.
     * The message lambda is only evaluated if debug is enabled.
     *
     * @param message Lazy message supplier (only evaluated if debug enabled)
     */
    fun debug(message: () -> String)

    companion object {
        /**
         * Get the current logger instance (either test or default).
         */
        val instance: DebugLogger
            get() = testInstance ?: defaultInstance

        /**
         * Default logger implementation using system property check.
         */
        private val defaultInstance: DebugLogger = SystemPropertyDebugLogger()

        /**
         * Test logger instance for unit testing.
         * When set, this takes precedence over the default instance.
         */
        @Volatile
        private var testInstance: DebugLogger? = null

        /**
         * Set a test logger instance for unit testing.
         * Pass null to restore default behavior.
         *
         * @param logger Test logger or null to use default
         */
        fun setTestInstance(logger: DebugLogger?) {
            testInstance = logger
        }
    }
}

/**
 * Default debug logger implementation that checks system property.
 */
private class SystemPropertyDebugLogger : DebugLogger {
    /**
     * Cache debug mode for performance.
     * Checking system property on every log call would be expensive.
     */
    private val debugEnabled: Boolean = System.getProperty("junit.airgap.debug") == "true"

    override fun debug(message: () -> String) {
        if (debugEnabled) {
            println("[junit-no-network] ${message()}")
        }
    }
}
