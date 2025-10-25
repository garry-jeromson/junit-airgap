package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.AllowedHosts
import io.github.garryjeromson.junit.nonetwork.BlockedHosts
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.DefaultApiClient
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

/**
 * Android tests for host filtering with KMP shared client.
 * Validates @AllowedHosts and @BlockedHosts work with OkHttp engine.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidKmpKtorHostFilteringTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["localhost", "127.0.0.1"])
    fun shouldAllowLocalhostWithSharedClient() = runTest {
        val client = DefaultApiClient(baseUrl = "http://localhost:8080")
        try {
            client.fetchUser(1)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("localhost should be allowed", e)
        } catch (e: Exception) {
            // Other network errors are fine
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["localhost"])
    fun shouldBlockOtherHostsWhenLocalhostAllowed() = runTest {
        val client = DefaultApiClient(baseUrl = "https://example.com")
        try {
            val exception = assertFailsWith<Exception> {
                client.fetchUser(1)
            }
            val message = exception.message ?: ""
            val hasNetworkBlockedMessage = message.contains("Network request blocked") ||
                message.contains("NetworkRequestAttemptedException")
            if (!hasNetworkBlockedMessage) {
                throw AssertionError("Expected network to be blocked, but got: $exception")
            }
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["*.example.com"])
    fun shouldAllowWildcardPatternsWithSharedClient() = runTest {
        val client = DefaultApiClient(baseUrl = "https://api.example.com")
        try {
            client.fetchUser(1)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("*.example.com should allow api.example.com", e)
        } catch (e: Exception) {
            // Other network errors are fine
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["*"])
    @BlockedHosts(hosts = ["evil.com", "tracking.example.com"])
    fun shouldBlockSpecificHostsEvenWhenAllHostsAllowed() = runTest {
        val client = DefaultApiClient(baseUrl = "https://evil.com")
        try {
            val exception = assertFailsWith<Exception> {
                client.fetchUser(1)
            }
            val message = exception.message ?: ""
            val hasNetworkBlockedMessage = message.contains("Network request blocked") ||
                message.contains("NetworkRequestAttemptedException")
            if (!hasNetworkBlockedMessage) {
                throw AssertionError("Expected network to be blocked, but got: $exception")
            }
        } finally {
            client.close()
        }
    }
}
