package io.github.garryjeromson.junit.nonetwork.plugin

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.jupiter.api.Test
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Validates that the Gradle plugin works correctly for Android unit tests.
 *
 * This test class demonstrates that the plugin's zero-configuration approach
 * works identically across JVM and Android targets.
 *
 * The plugin configuration in build.gradle.kts applies to both:
 * - jvmTest (tested by PluginBasicUsageTest)
 * - androidUnitTest (tested by this class)
 *
 * Key validation points:
 * - Plugin adds dependency to Android test configurations
 * - Plugin configures Android test tasks correctly
 * - Network blocking works automatically without any annotations
 * - @AllowNetwork opt-out works on Android
 */
class PluginDefaultBlockingTest {
    // No @ExtendWith or @RegisterExtension needed!
    // Plugin handles all configuration for Android tests too.

    @Test
    fun `network blocking works automatically on Android unit tests`() {
        // Verifies the plugin configured Android test tasks correctly
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    fun `multiple Android tests all block by default`() {
        // All tests in Android source set block automatically
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("android.com", 443)
        }
    }

    @Test
    @AllowNetwork
    fun `AllowNetwork annotation works on Android tests`() {
        // Opt-out works the same way on Android
        try {
            Socket("localhost", 54321)
        } catch (e: java.net.ConnectException) {
            // Expected - proves @AllowNetwork works on Android
        }
    }

    @Test
    fun `plugin configuration is consistent across platforms`() {
        // This test verifies that the same plugin configuration
        // (applyToAllTests = true) works identically on Android
        assertFailsWith<NetworkRequestAttemptedException> {
            java.net.URL("https://android-example.com").openConnection().connect()
        }
    }
}
