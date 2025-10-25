package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests that verify Ktor HTTP client network blocking works correctly
 * with both @BlockNetworkRequests and @AllowNetworkRequests annotations on Android/Robolectric.
 * Uses OkHttp engine which is compatible with Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class KtorClientTest {
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
    fun `ktor client with spaces in test name is blocked on Android`() {
        assertRequestBlocked {
            makeKtorRequest()
        }
    }
}
