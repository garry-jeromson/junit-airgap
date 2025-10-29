package io.github.garryjeromson.junit.airgap

/**
 * Simple debug logger for iOS.
 *
 * iOS doesn't have system properties like JVM, so debug mode is controlled
 * at compile time or via a simple flag.
 */
internal object DebugLogger {
    // Simple flag to enable/disable debug logging
    // Can be set from tests or configuration
    var debugEnabled: Boolean = false

    /**
     * Log a debug message if debug mode is enabled.
     */
    fun log(message: String) {
        if (debugEnabled) {
            println("[Airgap] $message")
        }
    }
}
