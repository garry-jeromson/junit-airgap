package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.asynchttpclient.Dsl
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

class AsyncHttpClientTest {
    private fun makeAsyncRequest(): String {
        val client = Dsl.asyncHttpClient()
        return try {
            client
                .prepareGet("https://example.com/")
                .execute()
                .get()
                .responseBody
        } finally {
            client.close()
        }
    }

    @Test
    @BlockNetworkRequests
    fun asyncHttpClientIsBlockedWithNoNetworkTest() {
        assertFailsWith<NetworkRequestAttemptedException> {
            makeAsyncRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun asyncHttpClientIsAllowedWithAllowNetwork() {
        try {
            makeAsyncRequest()
        } catch (e: NetworkRequestAttemptedException) {
            fail("Network is allowed, but was blocked: ${e.message}")
        } catch (e: Exception) {
            // Other exceptions are fine
        }
    }

    @Test
    @BlockNetworkRequests
    fun `async http client with spaces in test name is blocked`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            makeAsyncRequest()
        }
    }
}
