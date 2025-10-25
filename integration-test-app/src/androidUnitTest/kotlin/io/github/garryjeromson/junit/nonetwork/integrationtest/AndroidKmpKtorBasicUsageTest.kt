package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.DefaultApiClient
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

/**
 * Android tests for basic KMP Ktor usage with shared client.
 * Validates OkHttp engine is properly blocked on Android.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidKmpKtorBasicUsageTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @NoNetworkTest
    fun shouldBlockSharedApiClientRequests() = runTest {
        val apiClient = DefaultApiClient()
        try {
            // OkHttp wraps NetworkRequestAttemptedException in IOException
            val exception = assertFailsWith<Exception> {
                apiClient.fetchUser(1)
            }
            val message = exception.message ?: ""
            val hasNetworkBlockedMessage = message.contains("Network request blocked") ||
                message.contains("NetworkRequestAttemptedException")
            if (!hasNetworkBlockedMessage) {
                throw AssertionError("Expected network to be blocked, but got: $exception")
            }
        } finally {
            apiClient.close()
        }
    }

    @Test
    @NoNetworkTest
    fun shouldBlockSharedApiClientFetchPosts() = runTest {
        val apiClient = DefaultApiClient()
        try {
            val exception = assertFailsWith<Exception> {
                apiClient.fetchPosts()
            }
            val message = exception.message ?: ""
            val hasNetworkBlockedMessage = message.contains("Network request blocked") ||
                message.contains("NetworkRequestAttemptedException")
            if (!hasNetworkBlockedMessage) {
                throw AssertionError("Expected network to be blocked, but got: $exception")
            }
        } finally {
            apiClient.close()
        }
    }

    @Test
    @NoNetworkTest
    fun shouldBlockUserRepositoryRequests() = runTest {
        val repository = UserRepository()
        try {
            val exception = assertFailsWith<Exception> {
                repository.getUser(1)
            }
            val message = exception.message ?: ""
            val hasNetworkBlockedMessage = message.contains("Network request blocked") ||
                message.contains("NetworkRequestAttemptedException")
            if (!hasNetworkBlockedMessage) {
                throw AssertionError("Expected network to be blocked, but got: $exception")
            }
        } finally {
            repository.close()
        }
    }

    @Test
    fun shouldAllowNetworkWithoutAnnotation() = runTest {
        val apiClient = DefaultApiClient()
        try {
            // Without @NoNetworkTest, network should be allowed
            apiClient.fetchUser(1)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Network should be allowed without @NoNetworkTest", e)
        } catch (e: Exception) {
            // Other network errors are expected
        } finally {
            apiClient.close()
        }
    }
}
