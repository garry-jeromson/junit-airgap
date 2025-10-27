package io.github.garryjeromson.junit.airgap.test

import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestBlocked
import org.asynchttpclient.Dsl
import org.junit.Test

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
