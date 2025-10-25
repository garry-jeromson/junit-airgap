package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Simple JVM tests using JUnit 4 annotations.
 * Tests the bytecode enhancement path for @Rule injection.
 */
class SimpleTest {
    @Test
    @NoNetworkTest
    fun testWithNoNetworkAnnotation() {
        assertTrue(true, "JVM JUnit 4 test executed")
    }

    @Test
    @AllowNetwork
    fun testWithAllowNetwork() {
        assertTrue(true, "AllowNetwork test executed")
    }
}
