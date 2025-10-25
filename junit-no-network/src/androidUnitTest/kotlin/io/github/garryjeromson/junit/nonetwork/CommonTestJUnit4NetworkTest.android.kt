package io.github.garryjeromson.junit.nonetwork

import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Android-specific implementation of network test functions for commonTest.
 *
 * These actual functions are called by tests defined in commonTest,
 * allowing us to verify that bytecode enhancement works for tests
 * defined in the common source set but executed on Android.
 */

actual fun performNetworkBlockingTest() {
    // This should throw NetworkRequestAttemptedException
    // if bytecode enhancement successfully injected the @Rule field
    assertFailsWith<NetworkRequestAttemptedException>("Network should be blocked by injected rule") {
        Socket("example.com", 80).use { }
    }
}

actual fun performNonBlockingNetworkTest() {
    // Without @NoNetworkTest, network should not be blocked
    // Connection may fail for other reasons (no internet, etc) but should not throw our exception
    try {
        Socket("example.com", 80).close()
    } catch (e: NetworkRequestAttemptedException) {
        throw AssertionError("Network should NOT be blocked without @NoNetworkTest annotation", e)
    } catch (e: Exception) {
        // Other exceptions (UnknownHostException, SocketException, etc.) are OK
        // We're only verifying that our NetworkRequestAttemptedException is NOT thrown
    }
}
