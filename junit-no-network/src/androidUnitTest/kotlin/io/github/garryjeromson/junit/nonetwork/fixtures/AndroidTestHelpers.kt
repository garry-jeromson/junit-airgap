package io.github.garryjeromson.junit.nonetwork.integration.fixtures

import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException

/**
 * Helper function to assert that a network request is blocked.
 * For Android/Robolectric integration tests.
 * Checks for NetworkRequestAttemptedException either directly or in the cause chain.
 */
fun assertNetworkBlocked(
    message: String,
    block: () -> Unit,
) {
    try {
        block()
        throw AssertionError("$message - Expected NetworkRequestAttemptedException but no exception was thrown")
    } catch (e: NetworkRequestAttemptedException) {
        // Expected - network was blocked
    } catch (e: AssertionError) {
        throw e
    } catch (e: Exception) {
        // Check if NetworkRequestAttemptedException is in the cause chain
        var cause: Throwable? = e.cause
        var foundNetworkException = false
        while (cause != null) {
            if (cause is NetworkRequestAttemptedException) {
                foundNetworkException = true
                break
            }
            cause = cause.cause
        }
        if (!foundNetworkException) {
            throw AssertionError(
                "$message - Expected NetworkRequestAttemptedException but got ${e::class.simpleName}: ${e.message}",
                e,
            )
        }
        // If we found NetworkRequestAttemptedException in the cause chain, that's expected
    }
}

/**
 * Helper function to assert that a network request is NOT blocked.
 * Allows connection exceptions but fails if NetworkRequestAttemptedException is thrown.
 */
fun assertNetworkNotBlocked(
    message: String,
    block: () -> Unit,
) {
    try {
        block()
        // Success - no exception or connection succeeded
    } catch (e: NetworkRequestAttemptedException) {
        throw AssertionError("$message - Network should not be blocked but got: ${e.message}", e)
    } catch (e: Exception) {
        // Other exceptions (ConnectException, etc.) are fine - we just verify no blocking occurred
    }
}
