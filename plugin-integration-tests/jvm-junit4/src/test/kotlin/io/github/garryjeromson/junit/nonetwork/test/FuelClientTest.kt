package io.github.garryjeromson.junit.nonetwork.test

import com.github.kittinunf.fuel.Fuel
import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import org.junit.Test

/**
 * Tests that verify Fuel HTTP client works with @AllowNetworkRequests.
 * Note: Fuel blocking tests are covered comprehensively in the main
 * integration test suite (junit-no-network/src/jvmIntegrationTest).
 * These plugin integration tests verify the plugin works correctly.
 */
class FuelClientTest {
    private fun makeFuelRequest(): String {
        val (_, _, result) = Fuel.get("https://example.com/").responseString()
        return result.get()
    }

    @Test
    @AllowNetworkRequests
    fun fuelIsAllowedWithAllowNetwork() {
        assertRequestAllowed {
            makeFuelRequest()
        }
    }
}
