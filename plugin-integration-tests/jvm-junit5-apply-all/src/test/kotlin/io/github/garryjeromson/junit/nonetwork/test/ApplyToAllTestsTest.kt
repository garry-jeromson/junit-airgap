package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import org.junit.jupiter.api.Test
import java.net.Socket

/**
 * Tests for applyToAllTests = true configuration.
 *
 * When applyToAllTests is true in the plugin configuration:
 * - Tests WITHOUT annotations should have network blocked by default
 * - Tests with @AllowNetworkRequests should allow network (opt-out)
 * - Tests with explicit @BlockNetworkRequests should still block (redundant but allowed)
 */
class ApplyToAllTestsTest {
    @Test
    fun `network blocked by default without annotation`() {
        assertRequestBlocked {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @AllowNetworkRequests
    fun `network allowed when opted out with AllowNetworkRequests`() {
        assertRequestAllowed {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `network blocked with explicit BlockNetworkRequests annotation`() {
        assertRequestBlocked {
            Socket("example.com", 80).use { }
        }
    }
}
