package io.github.garryjeromson.junit.airgap.test

import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestBlocked
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Tests that verify Ktor HTTP client works correctly when applyToAllTests = true (JUnit 4).
 *
 * Plugin is configured with:
 * - applyToAllTests = true (network blocked by default for all tests)
 *
 * Expected behavior:
 * - Network should be blocked by default even without @BlockNetworkRequests annotation
 * - @AllowNetworkRequests should allow network access
 */
class KtorClientTest {
    private fun makeKtorRequest(): String =
        runBlocking {
            val client = HttpClient(CIO)
            try {
                client.get("https://example.com/").toString()
            } finally {
                client.close()
            }
        }

    @Test
    fun `ktor is blocked by default without annotation when applyToAllTests is enabled`() {
        assertRequestBlocked {
            makeKtorRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun `ktor is allowed with allow network annotation`() {
        assertRequestAllowed {
            makeKtorRequest()
        }
    }
}
