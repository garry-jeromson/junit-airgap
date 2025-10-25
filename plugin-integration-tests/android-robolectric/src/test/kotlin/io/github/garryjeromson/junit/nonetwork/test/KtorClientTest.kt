package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
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
 * with both @NoNetworkTest and @AllowNetwork annotations on Android/Robolectric.
 * Uses OkHttp engine which is compatible with Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class KtorClientTest {

    private fun makeKtorRequest(): String = runBlocking {
        val client = HttpClient(OkHttp)
        try {
            client.get("https://example.com").toString()
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    fun ktorClientShouldBeBlockedWithNoNetworkTest() {
        val exception = assertFailsWith<Exception> {
            makeKtorRequest()
        }
        // Ktor/OkHttp wraps NetworkRequestAttemptedException in IOException
        // Check if it's either the exception itself or in the cause chain
        assertTrue(
            exception is NetworkRequestAttemptedException ||
            exception.cause is NetworkRequestAttemptedException ||
            exception.message?.contains("NetworkRequestAttemptedException") == true,
            "Expected NetworkRequestAttemptedException but got: ${exception::class.simpleName}: ${exception.message}"
        )
    }

    @Test
    @AllowNetwork
    fun ktorClientShouldBeAllowedWithAllowNetwork() {
        try {
            makeKtorRequest()
        } catch (e: NetworkRequestAttemptedException) {
            fail("Network should be allowed with @AllowNetwork, but was blocked: ${e.message}")
        } catch (e: Exception) {
            // Other exceptions (like actual network errors) are fine - we just want to ensure
            // NetworkRequestAttemptedException is not thrown
        }
    }

    @Test
    @NoNetworkTest
    fun `ktor client with spaces in test name should be blocked on Android`() {
        val exception = assertFailsWith<Exception> {
            makeKtorRequest()
        }
        // Ktor/OkHttp wraps NetworkRequestAttemptedException in IOException
        // Check if it's either the exception itself or in the cause chain
        assertTrue(
            exception is NetworkRequestAttemptedException ||
            exception.cause is NetworkRequestAttemptedException ||
            exception.message?.contains("NetworkRequestAttemptedException") == true,
            "Expected NetworkRequestAttemptedException but got: ${exception::class.simpleName}: ${exception.message}"
        )
    }
}
