package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests that verify Ktor HTTP client network blocking works correctly
 * with both @BlockNetworkRequests and @AllowNetworkRequests annotations in KMP context (JUnit 5).
 *
 * Uses expect/actual pattern for platform-specific HTTP client implementations:
 * - JVM: CIO engine
 * - Android: OkHttp engine
 */
class KtorClientTest {
    @Test
    @BlockNetworkRequests
    fun ktorClientIsBlockedWithNoNetworkTest() {
        val exception =
            assertFailsWith<Exception> {
                makeKtorRequest()
            }
        // Ktor/OkHttp wraps NetworkRequestAttemptedException in IOException on Android
        // Check if it's either the exception itself or in the cause chain
        assertTrue(
            exception is NetworkRequestAttemptedException ||
                exception.cause is NetworkRequestAttemptedException ||
                exception.message?.contains("NetworkRequestAttemptedException") == true,
            "Expected NetworkRequestAttemptedException but got: ${exception::class.simpleName}: ${exception.message}",
        )
    }

    @Test
    @AllowNetworkRequests
    fun ktorClientIsAllowedWithAllowNetwork() {
        try {
            makeKtorRequest()
        } catch (e: NetworkRequestAttemptedException) {
            fail("Network is allowed with @AllowNetworkRequests, but was blocked: ${e.message}")
        } catch (e: Exception) {
            // Other exceptions (like actual network errors) are fine - we just want to ensure
            // NetworkRequestAttemptedException is not thrown
        }
    }

    @Test
    @BlockNetworkRequests
    fun `ktor client with spaces in test name is blocked in KMP JUnit5`() {
        val exception =
            assertFailsWith<Exception> {
                makeKtorRequest()
            }
        // Ktor/OkHttp wraps NetworkRequestAttemptedException in IOException on Android
        // Check if it's either the exception itself or in the cause chain
        assertTrue(
            exception is NetworkRequestAttemptedException ||
                exception.cause is NetworkRequestAttemptedException ||
                exception.message?.contains("NetworkRequestAttemptedException") == true,
            "Expected NetworkRequestAttemptedException but got: ${exception::class.simpleName}: ${exception.message}",
        )
    }
}

/**
 * Platform-specific function to make an HTTP request using Ktor.
 * Actual implementations use different engines per platform.
 */
expect fun makeKtorRequest(): String
