package io.github.garryjeromson.junit.airgap.test.contracts

import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import kotlin.test.fail
import kotlin.test.assertTrue

/**
 * Android/Robolectric implementation: Assert that a network request is blocked.
 * HTTP clients may wrap NetworkRequestAttemptedException in IOException or other exceptions,
 * so we check by class name and in the cause chain.
 */
actual fun assertRequestBlocked(block: () -> Unit) {
    try {
        block()
        fail("Expected NetworkRequestAttemptedException to be thrown, but no exception was thrown")
    } catch (e: Throwable) {
        // Check if it's NetworkRequestAttemptedException directly or if it's wrapped
        val exceptionClass = e::class.simpleName
        val causeClass = e.cause?.let { it::class.simpleName }
        val isNetworkException =
            exceptionClass == "NetworkRequestAttemptedException" ||
                causeClass == "NetworkRequestAttemptedException" ||
                e.message?.contains("NetworkRequestAttemptedException") == true

        assertTrue(
            isNetworkException,
            "Expected NetworkRequestAttemptedException but got: $exceptionClass (cause: $causeClass): ${e.message}"
        )
    }
}

/**
 * Android implementation: Assert that a network request is allowed.
 * Should NOT throw NetworkRequestAttemptedException (even if wrapped).
 */
actual fun assertRequestAllowed(block: () -> Unit) {
    try {
        block()
    } catch (e: NetworkRequestAttemptedException) {
        fail("Network should be allowed, but NetworkRequestAttemptedException was thrown: ${e.message}")
    } catch (e: Throwable) {
        // Check if NetworkRequestAttemptedException is wrapped in the cause chain
        val causeChain = generateSequence(e.cause) { it.cause }
        val hasNetworkException = causeChain.any { it is NetworkRequestAttemptedException }
        if (hasNetworkException) {
            fail("Network should be allowed, but NetworkRequestAttemptedException was found in cause chain: ${e.message}")
        }
        // Other exceptions (DNS failure, connection errors, etc.) are OK
    }
}
