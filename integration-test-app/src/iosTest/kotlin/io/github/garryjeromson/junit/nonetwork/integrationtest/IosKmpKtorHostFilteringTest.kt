package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.DefaultApiClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * iOS tests for host filtering structure with KMP shared client.
 * Note: Host filtering annotations are NOT enforced on iOS (API structure only).
 * These tests validate the API structure is correct.
 */
class IosKmpKtorHostFilteringTest {

    @Test
    fun testLocalhostClientStructure() = runTest {
        val client = DefaultApiClient(baseUrl = "http://localhost:8080")
        try {
            client.fetchUser(1)
        } catch (e: Exception) {
            // Expected
        } finally {
            client.close()
        }
    }

    @Test
    fun testWildcardHostClientStructure() = runTest {
        val client = DefaultApiClient(baseUrl = "https://api.example.com")
        try {
            client.fetchUser(1)
        } catch (e: Exception) {
            // Expected
        } finally {
            client.close()
        }
    }

    @Test
    fun testCustomBaseUrlWorks() = runTest {
        val client = DefaultApiClient(baseUrl = "https://jsonplaceholder.typicode.com")
        try {
            client.fetchUser(1)
        } catch (e: Exception) {
            // Expected
        } finally {
            client.close()
        }
    }

    @Test
    fun testMultipleHostsCanBeUsed() = runTest {
        val client1 = DefaultApiClient(baseUrl = "https://api.github.com")
        val client2 = DefaultApiClient(baseUrl = "https://httpbin.org")
        try {
            client1.fetchUser(1)
            client2.fetchUrl("https://httpbin.org/get")
        } catch (e: Exception) {
            // Expected
        } finally {
            client1.close()
            client2.close()
        }
    }
}
