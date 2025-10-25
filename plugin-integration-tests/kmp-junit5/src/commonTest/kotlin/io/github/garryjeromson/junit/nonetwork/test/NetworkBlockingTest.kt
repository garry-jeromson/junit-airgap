package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.jupiter.api.Test

/**
 * CommonTest with expect/actual for platform-specific network operations.
 * Tests actual network blocking behavior.
 */
class NetworkBlockingTest {
    @Test
    @NoNetworkTest
    fun `network should be blocked with NoNetworkTest`() {
        // Platform-specific implementation will attempt network and verify blocking
        testNetworkBlocking()
    }

    @Test
    @AllowNetwork
    fun `network should be allowed with AllowNetwork`() {
        // Platform-specific implementation will attempt network and verify it's allowed
        testNetworkAllowed()
    }
}

// Platform-specific implementations
expect fun testNetworkBlocking()

expect fun testNetworkAllowed()
