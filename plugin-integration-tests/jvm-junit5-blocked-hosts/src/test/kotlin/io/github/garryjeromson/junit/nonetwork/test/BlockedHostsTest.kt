package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import org.junit.jupiter.api.Test
import java.net.Socket

/**
 * Tests for blockedHosts plugin configuration.
 *
 * Plugin is configured with:
 * - allowedHosts = ["*"]  (allow all hosts by default)
 * - blockedHosts = ["*.example.com", "badhost.io"]  (except these)
 *
 * Expected behavior:
 * - Requests to *.example.com should be blocked when network blocking is enabled
 * - Requests to badhost.io should be blocked when network blocking is enabled
 * - Requests to other hosts should be allowed when network blocking is enabled
 * - Annotation-level @BlockRequestsToHosts should merge with global configuration
 * - Annotation-level @AllowRequestsToHosts merges with global allowed hosts
 * - Global blocked hosts ALWAYS take precedence (cannot be overridden by annotations)
 *
 * Note: blockedHosts takes precedence over allowedHosts. To use blockedHosts as a
 * blacklist, set allowedHosts = ["*"] to allow everything except the blocked hosts.
 */
class BlockedHostsTest {
    @Test
    @BlockNetworkRequests
    fun `blocks requests to wildcard example_com domain`() {
        assertRequestBlocked {
            Socket("api.example.com", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks requests to badhost_io`() {
        assertRequestBlocked {
            Socket("badhost.io", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `allows requests to non-blocked hosts`() {
        assertRequestAllowed {
            Socket("localhost", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `allows requests to google_com not in blocked list`() {
        assertRequestAllowed {
            Socket("google.com", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    @BlockRequestsToHosts(hosts = ["another-bad-host.com"])
    fun `annotation-level blocked hosts merge with global configuration`() {
        // Global blockedHosts should still be blocked
        assertRequestBlocked {
            Socket("api.example.com", 80).use { }
        }

        // Annotation-level blocked host should also be blocked
        assertRequestBlocked {
            Socket("another-bad-host.com", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["other-host.com"])
    fun `annotation-level allowed hosts combine with global configuration`() {
        // Annotation-level allowed hosts should be added to global allowed hosts
        assertRequestAllowed {
            Socket("other-host.com", 80).use { }
        }

        // Global blocked hosts should still take precedence
        // (blockedHosts takes precedence over allowedHosts per NetworkConfiguration docs)
        assertRequestBlocked {
            Socket("api.example.com", 80).use { }
        }
    }
}