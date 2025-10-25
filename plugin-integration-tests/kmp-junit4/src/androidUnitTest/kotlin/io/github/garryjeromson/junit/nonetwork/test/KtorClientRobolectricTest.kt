package io.github.garryjeromson.junit.nonetwork.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Robolectric-specific tests for Ktor HTTP client on Android in KMP context.
 * These tests run with Robolectric to verify Android framework integration.
 */
@RunWith(RobolectricTestRunner::class)
class KtorClientRobolectricTest {
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
    fun ktorClientIsBlockedWithNoNetworkTestOnAndroid() {
        // Verify we're running with Robolectric
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context, "Has Android context via Robolectric")

        // Verify network blocking works
        try {
            makeKtorRequest()
            fail("Expected exception to be thrown")
        } catch (e: Exception) {
            // Ktor/OkHttp wraps NetworkRequestAttemptedException in IOException
            assertTrue(
                e is NetworkRequestAttemptedException ||
                    e.cause is NetworkRequestAttemptedException ||
                    e.message?.contains("NetworkRequestAttemptedException") == true,
                "Expected NetworkRequestAttemptedException but got: ${e::class.simpleName}: ${e.message}",
            )
        }
    }

    @Test
    @AllowNetworkRequests
    fun ktorClientIsAllowedWithAllowNetworkOnAndroid() {
        // Verify we're running with Robolectric
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context, "Has Android context via Robolectric")

        // Verify network is allowed
        try {
            makeKtorRequest()
        } catch (e: NetworkRequestAttemptedException) {
            fail("Network is allowed with @AllowNetworkRequests, but was blocked: ${e.message}")
        } catch (e: Exception) {
            // Other exceptions (like actual network errors) are fine
        }
    }

    @Test
    @BlockNetworkRequests
    fun `ktor client with Robolectric and spaces in test name works on Android`() {
        // Verify we're running with Robolectric
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context, "Has Android context via Robolectric")

        // Verify network blocking works
        try {
            makeKtorRequest()
            fail("Expected exception to be thrown")
        } catch (e: Exception) {
            assertTrue(
                e is NetworkRequestAttemptedException ||
                    e.cause is NetworkRequestAttemptedException ||
                    e.message?.contains("NetworkRequestAttemptedException") == true,
                "Expected NetworkRequestAttemptedException but got: ${e::class.simpleName}: ${e.message}",
            )
        }
    }
}
