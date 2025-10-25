package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Demonstrates zero-configuration automatic network blocking using auto-registration.
 *
 * This test class demonstrates the "almost zero-config" approach:
 * - No @ExtendWith annotation needed
 * - Extension is auto-registered via ServiceLoader
 * - Only need @RegisterExtension to configure applyToAllTests for this specific test
 *
 * How it works:
 * 1. ServiceLoader mechanism auto-registers the extension (META-INF/services)
 * 2. junit-platform.properties enables auto-detection: junit.jupiter.extensions.autodetection.enabled=true
 * 3. @RegisterExtension configures applyToAllTests=true for this test class only
 *
 * Note: With the Gradle plugin, even the @RegisterExtension can be eliminated!
 * The plugin sets junit.nonetwork.applyToAllTests=true via system properties or
 * junit-platform.properties, giving true zero-configuration.
 */
class ZeroConfigAutoRegistrationTest {
    // No @ExtendWith annotation needed - extension is auto-registered!
    // We only need @RegisterExtension to configure applyToAllTests for THIS test class
    @JvmField
    @RegisterExtension
    val extension = NoNetworkExtension(applyToAllTests = true)

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
