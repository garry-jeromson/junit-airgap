package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.Test

/**
 * CommonTest with expect/actual for platform-specific network operations.
 * Uses JUnit 4 annotations.
 */
class NetworkBlockingTest {
    @Test
    @BlockNetworkRequests
    fun networkShouldBeBlockedWithNoNetworkTest() {
        // Platform-specific implementation will attempt network and verify blocking
        testNetworkBlocking()
    }

    @Test
    @AllowNetworkRequests
    fun networkShouldBeAllowedWithAllowNetwork() {
        // Platform-specific implementation will attempt network and verify it's allowed
        testNetworkAllowed()
    }
}

// Platform-specific implementations
expect fun testNetworkBlocking()

expect fun testNetworkAllowed()
