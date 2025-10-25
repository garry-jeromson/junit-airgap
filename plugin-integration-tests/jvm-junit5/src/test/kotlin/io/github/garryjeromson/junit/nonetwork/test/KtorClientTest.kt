package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * Tests that verify Ktor HTTP client network blocking works correctly
 * with both @BlockNetworkRequests and @AllowNetworkRequests annotations (JUnit 5).
 */
class KtorClientTest {
    private fun makeKtorRequest(): String =
        runBlocking {
            val client = HttpClient(CIO)
            try {
                client.get("https://example.com").toString()
            } finally {
                client.close()
            }
        }

    @Test
    @BlockNetworkRequests
    fun ktorClientIsBlockedWithNoNetworkTest() {
        assertRequestBlocked {
            makeKtorRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun ktorClientIsAllowedWithAllowNetwork() {
        assertRequestAllowed {
            makeKtorRequest()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `ktor client with spaces in test name is blocked`() {
        assertRequestBlocked {
            makeKtorRequest()
        }
    }
}
