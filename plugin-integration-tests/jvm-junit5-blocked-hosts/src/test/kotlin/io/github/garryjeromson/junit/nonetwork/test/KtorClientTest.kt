package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * Tests that verify Ktor HTTP client respects the blockedHosts configuration.
 *
 * Plugin is configured with:
 * - allowedHosts = ["*"] (allow all hosts by default)
 * - blockedHosts = ["*.example.com", "badhost.io"]
 *
 * Expected behavior:
 * - Requests to blocked hosts should be blocked
 * - Requests to non-blocked hosts should be allowed (when @AllowNetworkRequests is used)
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
    @AllowNetworkRequests
    fun `ktor allows requests to non-blocked hosts`() {
        assertRequestAllowed {
            makeKtorRequest("https://google.com/")
        }
    }
}
