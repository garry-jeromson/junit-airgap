package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import org.junit.jupiter.api.Test
import java.net.Socket

/**
 * Tests for allowedHosts plugin configuration.
 *
 * Plugin is configured with:
 * - allowedHosts = ["localhost", "127.0.0.1", "*.local"]
 *
 * Expected behavior:
 * - Requests to localhost should be allowed
 * - Requests to 127.0.0.1 should be allowed
 * - Requests to *.local domains should be allowed (wildcard matching)
 * - Requests to other hosts should be blocked when @BlockNetworkRequests is present
 * - Annotation-level @AllowRequestsToHosts should merge with global configuration
 */
class AllowedHostsTest {
    @Test
    @BlockNetworkRequests
    fun `allows requests to localhost`() {
        assertRequestAllowed {
            Socket("localhost", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `allows requests to 127_0_0_1`() {
        assertRequestAllowed {
            Socket("127.0.0.1", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `allows requests to wildcard local domain`() {
        assertRequestAllowed {
            Socket("subdomain.local", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks requests to non-allowed hosts`() {
        assertRequestBlocked {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["example.org"])
    fun `annotation-level allowed hosts merge with global configuration`() {
        // Both global allowedHosts and annotation-level should be allowed
        assertRequestAllowed {
            Socket("localhost", 80).use { }
        }

        assertRequestAllowed {
            Socket("example.org", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*.example.com"])
    fun `annotation-level wildcard merges with global allowed hosts`() {
        // Global allowedHosts should still work
        assertRequestAllowed {
            Socket("localhost", 80).use { }
        }

        // Annotation wildcard should work
        assertRequestAllowed {
            Socket("api.example.com", 80).use { }
        }
    }
}
