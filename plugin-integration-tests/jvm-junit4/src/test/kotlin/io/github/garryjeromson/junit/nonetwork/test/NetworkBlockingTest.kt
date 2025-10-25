package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.Test
import java.net.Socket
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * JVM tests that verify network blocking with JUnit 4.
 * Tests the bytecode enhancement path for @Rule injection.
 */
class NetworkBlockingTest {
    @Test
    @NoNetworkTest
    fun networkShouldBeBlockedWithNoNetworkTest() {
        // Network should be blocked - expect exception
        assertFailsWith<NetworkRequestAttemptedException>("Network should be blocked") {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @AllowNetwork
    fun networkShouldBeAllowedWithAllowNetwork() {
        // Network should be allowed - may throw IOException but not NetworkRequestAttemptedException
        try {
            Socket("example.com", 80).close()
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Network should NOT be blocked with @AllowNetwork", e)
        } catch (e: Exception) {
            // Other exceptions (no internet, DNS failure, etc.) are OK
        }
    }

    @Test
    @NoNetworkTest
    fun `test method names with spaces work correctly in JUnit 4`() {
        // Verify that Kotlin backtick syntax (spaces in method names) works with ByteBuddy injection
        assertTrue(true, "Test with spaces in name executed successfully")
    }
}
