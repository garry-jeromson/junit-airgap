package io.github.garryjeromson.junit.airgap.test

import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestBlocked
import org.junit.Test
import java.net.Socket
import kotlin.test.assertTrue

/**
 * JVM tests that verify network blocking with JUnit 4.
 * Tests the bytecode enhancement path for @Rule injection.
 */
class NetworkBlockingTest {
    @Test
    @BlockNetworkRequests
    fun networkIsBlockedWithNoNetworkTest() {
        assertRequestBlocked {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @AllowNetworkRequests
    fun networkIsAllowedWithAllowNetwork() {
        assertRequestAllowed {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `test method names with spaces work correctly in JUnit 4`() {
        // Verify that Kotlin backtick syntax (spaces in method names) works with ByteBuddy injection
        assertTrue(true, "Test with spaces in name executed successfully")
    }
}
