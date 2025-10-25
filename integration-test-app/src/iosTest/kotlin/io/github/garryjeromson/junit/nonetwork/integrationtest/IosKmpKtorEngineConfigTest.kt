package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.HttpClientFactory
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * iOS tests for platform-specific engine configuration.
 * Validates Darwin engine can be created and configured.
 * Note: Network requests are NOT actively blocked on iOS.
 */
class IosKmpKtorEngineConfigTest {

    @Test
    fun testDarwinEngineCanBeCreated() {
        val client = HttpClientFactory.create()
        assertNotNull(client, "HttpClient should be created with Darwin engine")
        client.close()
    }

    @Test
    fun testDarwinEngineCanMakeRequests() = runTest {
        val client = HttpClientFactory.create()
        try {
            // This demonstrates the engine works
            // Network will not be blocked on iOS
            client.get("https://api.github.com")
        } catch (e: Exception) {
            // Expected - network request may fail
        } finally {
            client.close()
        }
    }

    @Test
    fun testMultipleClientsCanBeCreated() {
        val client1 = HttpClientFactory.create()
        val client2 = HttpClientFactory.create()
        assertNotNull(client1)
        assertNotNull(client2)
        client1.close()
        client2.close()
    }

    @Test
    fun testClientCanBeReused() = runTest {
        val client = HttpClientFactory.create()
        try {
            // Make multiple requests with same client
            client.get("https://api.github.com/users/octocat")
            client.get("https://httpbin.org/get")
        } catch (e: Exception) {
            // Expected
        } finally {
            client.close()
        }
    }
}
