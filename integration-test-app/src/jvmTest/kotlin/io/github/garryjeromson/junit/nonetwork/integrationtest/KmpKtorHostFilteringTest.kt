package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.AllowedHosts
import io.github.garryjeromson.junit.nonetwork.BlockedHosts
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.DefaultApiClient
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertFailsWith

/**
 * Tests host filtering with KMP shared client pattern.
 * Validates @AllowedHosts and @BlockedHosts work correctly with expect/actual HttpClient.
 */
@ExtendWith(NoNetworkExtension::class)
class KmpKtorHostFilteringTest {

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["localhost", "127.0.0.1"])
    fun `should allow localhost with shared client`() = runTest {
        val client = DefaultApiClient(baseUrl = "http://localhost:8080")
        try {
            // localhost is allowed, so this should not throw NetworkRequestAttemptedException
            client.fetchUser(1)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("localhost should be allowed", e)
        } catch (e: Exception) {
            // Other network errors are fine (port not listening, etc.)
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["localhost"])
    fun `should block other hosts when localhost allowed`() = runTest {
        val client = DefaultApiClient(baseUrl = "https://example.com")
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                client.fetchUser(1)
            }
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["*.example.com"])
    fun `should allow wildcard patterns with shared client`() = runTest {
        val client = DefaultApiClient(baseUrl = "https://api.example.com")
        try {
            // *.example.com should allow api.example.com
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
    fun `should block specific hosts even when all hosts allowed`() = runTest {
        val client = DefaultApiClient(baseUrl = "https://evil.com")
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                client.fetchUser(1)
            }
        } finally {
            client.close()
        }
    }
}
