package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.jupiter.api.Test
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Android unit tests that verify network blocking with JUnit 5.
 */
class NetworkBlockingTest {
    @Test
    @NoNetworkTest
    fun `network should be blocked with NoNetworkTest`() {
        // Network should be blocked - expect exception
        assertFailsWith<NetworkRequestAttemptedException>("Network should be blocked") {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @AllowNetwork
    fun `network should be allowed with AllowNetwork`() {
        // Network should be allowed - may throw IOException but not NetworkRequestAttemptedException
        try {
            Socket("example.com", 80).close()
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Network should NOT be blocked with @AllowNetwork", e)
        } catch (e: Exception) {
            // Other exceptions (no internet, DNS failure, etc.) are OK
        }
    }
}
