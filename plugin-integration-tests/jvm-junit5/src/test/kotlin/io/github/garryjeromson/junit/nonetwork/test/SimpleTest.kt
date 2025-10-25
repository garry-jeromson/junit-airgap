package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Simple JVM tests using JUnit 5.
 */
class SimpleTest {
    @Test
    @BlockNetworkRequests
    fun `test with NoNetworkTest annotation`() {
        assertTrue(true, "JVM JUnit 5 test executed")
    }

    @Test
    @AllowNetworkRequests
    fun `test with AllowNetwork`() {
        assertTrue(true, "AllowNetwork test executed")
    }
}
