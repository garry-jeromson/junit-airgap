package io.github.garryjeromson.junit.nonetwork

/**
 * Test debug logger that captures log messages for assertion.
 * Used in unit tests to verify debug output.
 */
internal class TestDebugLogger : DebugLogger {
    private val _messages = mutableListOf<String>()

    /**
     * All log messages captured by this logger.
     */
    val messages: List<String>
        get() = _messages.toList()

    override fun debug(message: () -> String) {
        _messages.add(message())
    }

    /**
     * Clear all captured messages.
     */
    fun clear() {
        _messages.clear()
    }
}
