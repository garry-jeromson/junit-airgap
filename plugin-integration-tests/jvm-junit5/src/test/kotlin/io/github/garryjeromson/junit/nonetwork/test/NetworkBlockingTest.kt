package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.jupiter.api.Test
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * JVM tests that verify network blocking with JUnit 5.
 */
class NetworkBlockingTest {
    @Test
    @NoNetworkTest
    fun `network is blocked with NoNetworkTest`() {
        // Network is blocked - expect exception
        assertFailsWith<NetworkRequestAttemptedException>("Network is blocked") {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @AllowNetwork
    fun `network is allowed with AllowNetwork`() {
        // Network is allowed - may throw IOException but not NetworkRequestAttemptedException
        try {
            Socket("example.com", 80).close()
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Network is NOT blocked with @AllowNetwork", e)
        } catch (e: Exception) {
            // Other exceptions (no internet, DNS failure, etc.) are OK
        }
    }
}
