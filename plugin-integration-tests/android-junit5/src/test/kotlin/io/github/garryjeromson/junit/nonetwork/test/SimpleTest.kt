package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Simple Android unit tests using JUnit 5.
 */
class SimpleTest {
    @Test
    @NoNetworkTest
    fun `test with NoNetworkTest annotation`() {
        assertTrue(true, "Android JUnit 5 test executed")
    }

    @Test
    @AllowNetwork
    fun `test with AllowNetwork`() {
        assertTrue(true, "AllowNetwork test executed")
    }
}
