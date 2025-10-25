package io.github.garryjeromson.junit.nonetwork.test.contracts

import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import kotlin.test.fail
import kotlin.test.assertTrue

/**
 * JVM implementation: Assert that a network request is blocked.
 * HTTP clients may wrap NetworkRequestAttemptedException in other exceptions (e.g., Reactor's ReactiveException),
 * so we check in the cause chain.
 */
actual fun assertRequestBlocked(block: () -> Unit) {
    try {
        block()
        fail("Expected NetworkRequestAttemptedException to be thrown, but no exception was thrown")
    } catch (e: NetworkRequestAttemptedException) {
        // Direct throw - this is the expected case
        return
    } catch (e: Throwable) {
        // Check if NetworkRequestAttemptedException is in the cause chain
        val causeChain = generateSequence<Throwable>(e) { it.cause }
        val hasNetworkException = causeChain.any { it is NetworkRequestAttemptedException }

        assertTrue(
            hasNetworkException,
            "Expected NetworkRequestAttemptedException but got: ${e::class.simpleName}: ${e.message}"
        )
    }
}

/**
 * JVM implementation: Assert that a network request is allowed.
 * Should NOT throw NetworkRequestAttemptedException.
 * Other exceptions (network errors, timeouts, etc.) are acceptable - they indicate
 * real network issues, not our blocking mechanism.
 */
actual fun assertRequestAllowed(block: () -> Unit) {
    try {
        block()
    } catch (e: NetworkRequestAttemptedException) {
        fail("Network should be allowed, but NetworkRequestAttemptedException was thrown: ${e.message}")
    } catch (e: Exception) {
        // Other exceptions (DNS failure, connection refused, timeouts, etc.) are OK
        // We just want to ensure our blocking mechanism didn't trigger
    }
}
