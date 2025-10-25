package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.Socket
import java.net.URL
import kotlin.test.assertFailsWith

/**
 * Basic usage integration tests.
 * Validates that the published library works when consumed as a dependency.
 */
@ExtendWith(NoNetworkExtension::class)
class BasicUsageTest {

    @Test
    @NoNetworkTest
    fun `should block Socket connections`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @NoNetworkTest
    fun `should block HttpURLConnection`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            val connection = URL("http://example.com").openConnection()
            connection.connect()
        }
    }

    @Test
    fun `should allow network without annotation`() {
        // Without @NoNetworkTest, network should be allowed
        try {
            Socket("example.com", 80)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Network should be allowed without @NoNetworkTest", e)
        } catch (e: Exception) {
            // Other network errors are fine
        }
    }
}
