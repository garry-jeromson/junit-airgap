package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import org.junit.Test
import java.net.Socket

/**
 * Tests for applyToAllTests = true configuration with JUnit 4.
 *
 * This is an interesting case because the plugin uses bytecode injection
 * to add @Rule NoNetworkRule fields to test classes automatically.
 *
 * When applyToAllTests is true in the plugin configuration:
 * - The plugin injects @Rule NoNetworkRule(applyToAllTests = true) into test classes
 * - Tests WITHOUT annotations should have network blocked by default
 * - Tests with @AllowNetworkRequests should allow network (opt-out)
 * - Tests with explicit @BlockNetworkRequests should still block (redundant but allowed)
 *
 * Note: Unlike JUnit 5, JUnit 4 doesn't have automatic extension discovery,
 * so the plugin uses ByteBuddy to inject the rule at compile time.
 */
class ApplyToAllTestsJUnit4Test {
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
