package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.Socket
import java.net.URL
import kotlin.test.assertFailsWith

/**
 * Android basic usage integration tests using Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidBasicUsageTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @NoNetworkTest
    fun shouldBlockSocketConnections() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @NoNetworkTest
    fun shouldBlockHttpURLConnection() {
        assertFailsWith<NetworkRequestAttemptedException> {
            val connection = URL("http://example.com").openConnection()
            connection.connect()
        }
    }

    @Test
    fun shouldAllowNetworkWithoutAnnotation() {
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
