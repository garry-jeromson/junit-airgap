package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Pure commonTest - no expect/actual, just simple assertions.
 * Tests that the plugin works for common code on all platforms.
 */
class PureCommonTest {
    @Test
    @NoNetworkTest
    fun `test with NoNetworkTest annotation should work`() {
        // Simple assertion - verifies test execution works
        assertTrue(true, "Pure commonTest executed")
    }

    @Test
    @AllowNetwork
    fun `test with AllowNetwork should work`() {
        // Verifies opt-out mechanism
        assertTrue(true, "AllowNetwork test executed")
    }
}
