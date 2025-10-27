package io.github.garryjeromson.junit.airgap.test

import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.jupiter.api.Test

/**
 * CommonTest with expect/actual for platform-specific network operations.
 * Uses JUnit 5 annotations.
 */
class NetworkBlockingTest {
    @Test
    @BlockNetworkRequests
    fun `network should be blocked with NoNetworkTest`() {
        // Platform-specific implementation will attempt network and verify blocking
        testNetworkBlocking()
    }

    @Test
    @AllowNetworkRequests
    fun `network should be allowed with AllowNetwork`() {
        // Platform-specific implementation will attempt network and verify it's allowed
        testNetworkAllowed()
    }
}

// Platform-specific implementations
expect fun testNetworkBlocking()

expect fun testNetworkAllowed()
