package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import kotlin.test.Test

/**
 * CommonTest with expect/actual for platform-specific network operations.
 * Uses kotlin.test annotations (not JUnit).
 */
class NetworkBlockingTest {
    @Test
    @NoNetworkTest
    fun networkShouldBeBlockedWithNoNetworkTest() {
        // Platform-specific implementation will attempt network and verify blocking
        testNetworkBlocking()
    }

    @Test
    @AllowNetwork
    fun networkShouldBeAllowedWithAllowNetwork() {
        // Platform-specific implementation will attempt network and verify it's allowed
        testNetworkAllowed()
    }
}

// Platform-specific implementations
expect fun testNetworkBlocking()

expect fun testNetworkAllowed()
