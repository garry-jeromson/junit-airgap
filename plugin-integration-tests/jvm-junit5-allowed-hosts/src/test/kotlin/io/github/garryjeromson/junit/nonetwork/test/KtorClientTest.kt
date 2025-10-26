package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * Tests that verify Ktor HTTP client respects the allowedHosts configuration.
 *
 * Plugin is configured with:
 * - allowedHosts = ["localhost", "127.0.0.1", "*.local"]
 *
 * Expected behavior:
 * - Requests to non-allowed hosts should be blocked even with a real HTTP client like Ktor
 */
class KtorClientTest {
    private fun makeKtorRequest(url: String): String =
        runBlocking {
            val client = HttpClient(CIO)
            try {
                client.get(url).toString()
            } finally {
                client.close()
            }
        }

    @Test
    @BlockNetworkRequests
    fun `ktor blocks requests to non-allowed hosts`() {
        assertRequestBlocked {
            makeKtorRequest("https://example.com/")
        }
    }
}
