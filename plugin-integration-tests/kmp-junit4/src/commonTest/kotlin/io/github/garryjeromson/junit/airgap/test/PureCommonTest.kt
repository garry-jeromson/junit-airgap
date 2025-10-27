package io.github.garryjeromson.junit.airgap.test

import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Pure commonTest using JUnit 4 annotations.
 * Tests that the plugin works for JUnit 4 tests in common code.
 */
class PureCommonTest {
    @Test
    @BlockNetworkRequests
    fun testWithNoNetworkAnnotation() {
        // Simple assertion - verifies test execution works
        assertTrue(true, "Pure commonTest with JUnit 4 executed")
    }

    @Test
    @AllowNetworkRequests
    fun testWithAllowNetwork() {
        // Verifies opt-out mechanism
        assertTrue(true, "AllowNetwork test with JUnit 4 executed")
    }
}
