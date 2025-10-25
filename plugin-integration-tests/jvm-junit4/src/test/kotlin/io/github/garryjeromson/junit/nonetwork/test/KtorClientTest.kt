package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

/**
 * Tests that verify Ktor HTTP client network blocking works correctly
 * with both @NoNetworkTest and @AllowNetwork annotations.
 */
class KtorClientTest {

    private fun makeKtorRequest(): String = runBlocking {
        val client = HttpClient(CIO)
        try {
            client.get("https://example.com").toString()
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    fun ktorClientIsBlockedWithNoNetworkTest() {
        assertFailsWith<NetworkRequestAttemptedException> {
            makeKtorRequest()
        }
    }

    @Test
    @AllowNetwork
    fun ktorClientIsAllowedWithAllowNetwork() {
        try {
            makeKtorRequest()
        } catch (e: NetworkRequestAttemptedException) {
            fail("Network is allowed with @AllowNetwork, but was blocked: ${e.message}")
        } catch (e: Exception) {
            // Other exceptions (like actual network errors) are fine - we just want to ensure
            // NetworkRequestAttemptedException is not thrown
        }
    }

    @Test
    @NoNetworkTest
    fun `ktor client with spaces in test name is blocked`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            makeKtorRequest()
        }
    }
}
