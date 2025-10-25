package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.jupiter.api.Test
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Demonstrates zero-configuration automatic network blocking.
 *
 * This test class has NO @ExtendWith annotation, yet network blocking still works!
 *
 * How? The extension is automatically registered via:
 * 1. ServiceLoader mechanism (META-INF/services/org.junit.jupiter.api.extension.Extension)
 * 2. junit-platform.properties enables auto-detection: junit.jupiter.extensions.autodetection.enabled=true
 * 3. junit-platform.properties enables blocking: junit.nonetwork.applyToAllTests=true
 *
 * This is the ultimate UX - just add the dependency and configuration, no code changes needed!
 */
class ZeroConfigAutoRegistrationTest {
    // No @ExtendWith annotation!
    // No @RegisterExtension!
    // No @Rule!

    @Test
    fun `network requests are blocked automatically without any annotations`() {
        // Network is blocked even though we have NO @ExtendWith or @NoNetworkTest
        // This is because:
        // 1. The extension is auto-registered via ServiceLoader
        // 2. junit.nonetwork.applyToAllTests=true in junit-platform.properties
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @AllowNetwork
    fun `but we can still opt-out specific tests with AllowNetwork`() {
        // Even with global auto-blocking, we can opt-out individual tests
        // This test verifies that @AllowNetwork actually allows socket creation
        // If network blocking were active, this would throw NetworkRequestAttemptedException
        // Instead, we get ConnectException (connection refused) which proves blocking is disabled
        try {
            Socket("localhost", 12345)
        } catch (e: java.net.ConnectException) {
            // This is expected - the server doesn't exist
            // What matters is we got ConnectException, NOT NetworkRequestAttemptedException
            // This proves @AllowNetwork disabled the blocking
        }
    }

    @Test
    fun `another test also blocks automatically`() {
        // Every test in this class blocks network by default
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("google.com", 443)
        }
    }
}
