package io.github.garryjeromson.junit.nonetwork.plugin

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.jupiter.api.Test
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Validates that the Gradle plugin provides true zero-configuration network blocking.
 *
 * This test demonstrates the absolute simplest usage via the Gradle plugin:
 * - No @ExtendWith annotation
 * - No @RegisterExtension field
 * - No manual dependency declaration
 * - No junit-platform.properties in this module
 *
 * Everything is configured automatically by the plugin in build.gradle.kts:
 * ```
 * plugins {
 *     id("io.github.garryjeromson.junit-no-network")
 * }
 *
 * junitNoNetwork {
 *     enabled = true
 *     applyToAllTests = true
 * }
 * ```
 *
 * The plugin automatically:
 * 1. Adds the junit-no-network library dependency
 * 2. Creates junit-platform.properties with auto-detection enabled
 * 3. Configures test tasks with required system properties
 */
class PluginBasicUsageTest {
    // Absolutely NO extension setup needed!
    // The plugin handles everything automatically.

    @Test
    fun `network requests are blocked automatically by plugin configuration`() {
        // Network is blocked even though we have:
        // - NO @ExtendWith annotation
        // - NO @RegisterExtension field
        // - NO manual dependency in build.gradle.kts
        // This proves the plugin configured everything correctly
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @AllowNetwork
    fun `specific tests can opt-out with AllowNetwork annotation`() {
        // Even with plugin's global auto-blocking, we can opt-out individual tests
        // This test verifies that @AllowNetwork works with plugin configuration
        try {
            Socket("localhost", 12345)
        } catch (e: java.net.ConnectException) {
            // Expected - proves @AllowNetwork disabled the blocking
            // If blocking were active, we'd get NetworkRequestAttemptedException instead
        }
    }

    @Test
    fun `all tests in class block network by default`() {
        // Every test blocks network automatically thanks to plugin's applyToAllTests=true
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("google.com", 443)
        }
    }

    @Test
    fun `URL connections are also blocked`() {
        // Verify that URL-based connections are blocked too
        assertFailsWith<NetworkRequestAttemptedException> {
            java.net.URL("https://example.com").openConnection().connect()
        }
    }
}
