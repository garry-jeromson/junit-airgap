package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import org.junit.jupiter.api.Test
import java.net.Socket

/**
 * JVM tests that verify basic network blocking with JUnit 5 using Socket connections.
 * Uses shared test contracts for consistent behavior verification.
 */
class NetworkBlockingTest {
    @Test
    @BlockNetworkRequests
    fun `network is blocked with NoNetworkTest`() {
        assertRequestBlocked {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @AllowNetworkRequests
    fun `network is allowed with AllowNetwork`() {
        assertRequestAllowed {
            Socket("example.com", 80).use { }
        }
    }
}
