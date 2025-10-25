package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import org.asynchttpclient.Dsl
import org.junit.jupiter.api.Test

/**
 * Tests that verify AsyncHttpClient network blocking works correctly
 * with the plugin auto-configuration (JUnit 5).
 */
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
        assertRequestBlocked {
            makeAsyncRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun asyncHttpClientIsAllowedWithAllowNetwork() {
        assertRequestAllowed {
            makeAsyncRequest()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `async http client with spaces in test name is blocked`() {
        assertRequestBlocked {
            makeAsyncRequest()
        }
    }
}
