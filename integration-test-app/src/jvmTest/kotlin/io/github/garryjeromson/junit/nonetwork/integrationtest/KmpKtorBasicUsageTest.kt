package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.DefaultApiClient
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertFailsWith

/**
 * Tests basic KMP Ktor usage with shared client created in commonMain.
 * Validates that network blocking works with the expect/actual pattern.
 */
@ExtendWith(NoNetworkExtension::class)
class KmpKtorBasicUsageTest {

    @Test
    @NoNetworkTest
    fun `should block shared ApiClient requests`() = runTest {
        val apiClient = DefaultApiClient()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                apiClient.fetchUser(1)
            }
        } finally {
            apiClient.close()
        }
    }

    @Test
    @NoNetworkTest
    fun `should block shared ApiClient fetchPosts`() = runTest {
        val apiClient = DefaultApiClient()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                apiClient.fetchPosts()
            }
        } finally {
            apiClient.close()
        }
    }

    @Test
    @NoNetworkTest
    fun `should block UserRepository requests`() = runTest {
        val repository = UserRepository()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                repository.getUser(1)
            }
        } finally {
            repository.close()
        }
    }

    @Test
    fun `should allow network without annotation`() = runTest {
        val apiClient = DefaultApiClient()
        try {
            // Without @NoNetworkTest, network should be allowed
            // This will fail with connection error, but not NetworkRequestAttemptedException
            apiClient.fetchUser(1)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Network should be allowed without @NoNetworkTest", e)
        } catch (e: Exception) {
            // Other network errors are expected (connection refused, etc.)
        } finally {
            apiClient.close()
        }
    }
}
