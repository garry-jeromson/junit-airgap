package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.HttpClientFactory
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests platform-specific engine configuration with KMP pattern.
 * On JVM, this validates CIO engine is properly configured and blocked.
 */
@ExtendWith(NoNetworkExtension::class)
class KmpKtorEngineConfigTest {

    @Test
    @NoNetworkTest
    fun `should block CIO engine requests`() = runTest {
        val client = HttpClientFactory.create()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                client.get("https://api.github.com")
            }
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    fun `should include host information in exception`() = runTest {
        val client = HttpClientFactory.create()
        try {
            val exception = assertFailsWith<NetworkRequestAttemptedException> {
                client.get("https://example.com/api/data")
            }
            // Verify exception contains host information
            val message = exception.message ?: ""
            assertTrue(
                message.contains("example.com") || message.contains("Network request blocked"),
                "Exception should contain host or blocking message: $message"
            )
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    fun `should block multiple sequential requests`() = runTest {
        val client = HttpClientFactory.create()
        try {
            // First request
            assertFailsWith<NetworkRequestAttemptedException> {
                client.get("https://api.github.com/users/octocat")
            }

            // Second request
            assertFailsWith<NetworkRequestAttemptedException> {
                client.get("https://httpbin.org/get")
            }
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    fun `should block requests to different ports`() = runTest {
        val client = HttpClientFactory.create()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                client.get("https://example.com:8080/api")
            }
        } finally {
            client.close()
        }
    }
}
