package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import java.net.Socket
import kotlin.test.assertFailsWith

actual fun testNetworkBlocking() {
    // Network should be blocked - expect exception
    assertFailsWith<NetworkRequestAttemptedException>("Network should be blocked") {
        Socket("example.com", 80).use { }
    }
}

actual fun testNetworkAllowed() {
    // Network should be allowed - may throw IOException but not NetworkRequestAttemptedException
    try {
        Socket("example.com", 80).close()
    } catch (e: NetworkRequestAttemptedException) {
        throw AssertionError("Network should NOT be blocked with @AllowNetworkRequests", e)
    } catch (e: Exception) {
        // Other exceptions (no internet, DNS failure, etc.) are OK
    }
}
