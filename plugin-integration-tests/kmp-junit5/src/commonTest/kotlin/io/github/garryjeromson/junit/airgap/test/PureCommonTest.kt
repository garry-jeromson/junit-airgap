package io.github.garryjeromson.junit.airgap.test

import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Pure commonTest - no expect/actual, just simple assertions.
 * Tests that the plugin works for common code on all platforms.
 */
class PureCommonTest {
    @Test
    @BlockNetworkRequests
    fun `test with NoNetworkTest annotation should work`() {
        // Simple assertion - verifies test execution works
        assertTrue(true, "Pure commonTest executed")
    }

    @Test
    @AllowNetworkRequests
    fun `test with AllowNetwork should work`() {
        // Verifies opt-out mechanism
        assertTrue(true, "AllowNetwork test executed")
    }
}
