package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkByDefault
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Tests default blocking via @NoNetworkByDefault annotation.
 */
@ExtendWith(NoNetworkExtension::class)
@NoNetworkByDefault
class DefaultBlockingAnnotationTest {

    @Test
    fun `should block network by default with NoNetworkByDefault annotation`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @AllowNetwork
    fun `should allow network with AllowNetwork annotation`() {
        try {
            Socket("example.com", 80)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("@AllowNetwork should override @NoNetworkByDefault", e)
        } catch (e: Exception) {
            // Other network errors are fine
        }
    }
}
