package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests that verify Ktor HTTP client network blocking works correctly
 * with both @BlockNetworkRequests and @AllowNetworkRequests annotations on Android/Robolectric.
 * Uses OkHttp engine which is compatible with Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class KtorClientTest {
    private fun makeKtorRequest(): String =
        runBlocking {
            val client = HttpClient(OkHttp)
            try {
                client.get("https://example.com").toString()
            } finally {
                client.close()
            }
        }

    @Test
    @BlockNetworkRequests
    fun ktorClientIsBlockedWithNoNetworkTest() {
        val exception =
            assertFailsWith<Exception> {
                makeKtorRequest()
            }
        // Ktor/OkHttp wraps NetworkRequestAttemptedException in IOException
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
    fun `ktor client with spaces in test name is blocked on Android`() {
        val exception =
            assertFailsWith<Exception> {
                makeKtorRequest()
            }
        // Ktor/OkHttp wraps NetworkRequestAttemptedException in IOException
        // Check if it's either the exception itself or in the cause chain
        assertTrue(
            exception is NetworkRequestAttemptedException ||
                exception.cause is NetworkRequestAttemptedException ||
                exception.message?.contains("NetworkRequestAttemptedException") == true,
            "Expected NetworkRequestAttemptedException but got: ${exception::class.simpleName}: ${exception.message}",
        )
    }
}
