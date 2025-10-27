package io.github.garryjeromson.junit.airgap

/**
 * Exception thrown when a test attempts to make a network request while the NoNetwork extension is active.
 *
 * @param message Description of the attempted network request
 * @param requestDetails Additional details about the request (host, port, protocol, etc.)
 * @param cause The original exception that triggered this, if any
 */
class NetworkRequestAttemptedException(
    message: String,
    val requestDetails: NetworkRequestDetails? = null,
    cause: Throwable? = null,
) : AssertionError(buildMessage(message, requestDetails), cause) {
    companion object {
        private fun buildMessage(
            message: String,
            details: NetworkRequestDetails?,
        ): String =
            buildString {
                append(message)
                if (details != null) {
                    append("\n\nAttempted network request details:")
                    append("\n  Host: ${details.host}")
                    if (details.port != null) {
                        append("\n  Port: ${details.port}")
                    }
                    if (details.protocol != null) {
                        append("\n  Protocol: ${details.protocol}")
                    }
                    if (details.url != null) {
                        append("\n  URL: ${details.url}")
                    }
                    if (details.stackTrace != null) {
                        append("\n  Called from: ${details.stackTrace}")
                    }
                }
            }
    }
}

/**
 * Details about an attempted network request.
 */
data class NetworkRequestDetails(
    val host: String,
    val port: Int? = null,
    val protocol: String? = null,
    val url: String? = null,
    val stackTrace: String? = null,
)
