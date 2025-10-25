package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.AllowedHosts
import io.github.garryjeromson.junit.nonetwork.BlockedHosts
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Tests host filtering with @AllowedHosts and @BlockedHosts.
 */
@ExtendWith(NoNetworkExtension::class)
class HostFilteringTest {

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["localhost", "127.0.0.1"])
    fun `should allow localhost connections`() {
        try {
            Socket("localhost", 8080)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("localhost should be allowed", e)
        } catch (e: Exception) {
            // Other network errors are fine (port not listening, etc.)
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["localhost"])
    fun `should block other hosts when localhost is allowed`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["*.example.com"])
    fun `should allow wildcard patterns`() {
        try {
            Socket("api.example.com", 80)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("*.example.com should allow api.example.com", e)
        } catch (e: Exception) {
            // Other network errors are fine
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["*"])
    @BlockedHosts(hosts = ["evil.com", "tracking.com"])
    fun `should block specific hosts even when all hosts allowed`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("evil.com", 80)
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["*"])
    @BlockedHosts(hosts = ["tracking.com"])
    fun `should allow non-blocked hosts when all hosts allowed`() {
        try {
            Socket("example.com", 80)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("example.com should be allowed", e)
        } catch (e: Exception) {
            // Other network errors are fine
        }
    }
}
