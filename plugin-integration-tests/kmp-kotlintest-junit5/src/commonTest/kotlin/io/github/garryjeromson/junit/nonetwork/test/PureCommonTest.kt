package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pure commonTest using kotlin.test annotations.
 * Tests that the plugin works with kotlin.test framework when no explicit
 * JUnit dependencies are added and KMP handles test framework selection.
 */
class PureCommonTest {
    @Test
    @NoNetworkTest
    fun testWithNoNetworkAnnotation() {
        // Simple assertion - verifies test execution works
        assertTrue(true, "Pure commonTest with kotlin.test executed")
    }

    @Test
    @AllowNetwork
    fun testWithAllowNetwork() {
        // Verifies opt-out mechanism
        assertTrue(true, "AllowNetwork test with kotlin.test executed")
    }
}
