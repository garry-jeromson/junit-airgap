package io.github.garryjeromson.junit.nonetwork.integration.fixtures

import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import kotlin.test.fail

/**
 * Assert that a block of code throws NetworkRequestAttemptedException
 * or an exception caused by NetworkRequestAttemptedException (e.g., wrapped in IOException).
 */
fun assertNetworkBlocked(
    message: String = "Expected network to be blocked",
    block: () -> Unit,
) {
    try {
        block()
        fail("$message - but no exception was thrown")
    } catch (e: NetworkRequestAttemptedException) {
        // Expected - network was blocked
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
            fail("$message - but got ${e.javaClass.simpleName} instead of NetworkRequestAttemptedException: ${e.message}")
        }
        // If we found NetworkRequestAttemptedException in the cause chain, that's expected
    }
}

/**
 * Assert that a block of code does NOT throw NetworkRequestAttemptedException
 * (it may throw other exceptions like IOException, which is fine)
 */
fun assertNetworkNotBlocked(
    message: String = "Expected network NOT to be blocked",
    block: () -> Unit,
) {
    try {
        block()
        // Success - network was allowed
    } catch (e: NetworkRequestAttemptedException) {
        fail("$message - but NetworkRequestAttemptedException was thrown: ${e.message}")
    } catch (e: Exception) {
        // Other exceptions are acceptable (e.g., connection timeout, DNS failure)
        // We just verify it's not a NetworkRequestAttemptedException
    }
}
