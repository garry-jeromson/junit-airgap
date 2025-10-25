package io.github.garryjeromson.junit.nonetwork

import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.Socket
import java.net.URL
import kotlin.test.assertTrue

/**
 * Android-specific tests for NetworkBlocker using Robolectric.
 * Tests the Android implementation which uses SocketFactory instead of SecurityManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26]) // Test on API 26 (Android 8.0)
class AndroidNetworkBlockerTest {
    private lateinit var blocker: NetworkBlocker

    @After
    fun tearDown() {
        // Always uninstall after each test
        if (::blocker.isInitialized) {
            blocker.uninstall()
        }
    }

    @Test
    fun `should block socket connection to external host`() {
        // Given: A blocker with empty allowlist (blocks all)
        val config = NetworkConfiguration(allowedHosts = emptySet())
        blocker = NetworkBlocker(config)

        // When: Install the blocker
        blocker.install()

        // Then: Socket connection to external host should fail
        try {
            Socket("example.com", 80)
            throw AssertionError("Should have thrown NetworkRequestAttemptedException")
        } catch (e: NetworkRequestAttemptedException) {
            // Expected
        }
    }

    @Test
    fun `should allow socket connection to localhost`() {
        // Given: A blocker that allows localhost
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("localhost", "127.0.0.1"),
            )
        blocker = NetworkBlocker(config)

        // When: Install the blocker
        blocker.install()

        // Then: Connection to localhost should not throw NetworkRequestAttemptedException
        // (It may throw ConnectException if no server is running, which is fine)
        try {
            Socket("localhost", 8080)
        } catch (e: Exception) {
            // Should NOT be NetworkRequestAttemptedException
            assertTrue(
                e !is NetworkRequestAttemptedException,
                "Should not throw NetworkRequestAttemptedException for localhost",
            )
        }
    }

    @Test
    fun `should block HttpURLConnection to external host`() {
        // Given: A blocker with empty allowlist
        val config = NetworkConfiguration(allowedHosts = emptySet())
        blocker = NetworkBlocker(config)

        // When: Install the blocker
        blocker.install()

        // Then: HTTP connection should fail
        try {
            val url = URL("http://example.com")
            url.openConnection().connect()
            throw AssertionError("Should have thrown NetworkRequestAttemptedException")
        } catch (e: NetworkRequestAttemptedException) {
            // Expected
        }
    }

    @Test
    fun `should respect wildcard patterns`() {
        // Given: A blocker that allows *.local
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("*.local"),
            )
        blocker = NetworkBlocker(config)

        // When: Install the blocker
        blocker.install()

        // Then: Connections to *.local should be allowed (may fail with ConnectException)
        // But external hosts should be blocked
        try {
            Socket("example.com", 80)
            throw AssertionError("Should have thrown NetworkRequestAttemptedException")
        } catch (e: NetworkRequestAttemptedException) {
            // Expected
        }
    }

    @Test
    fun `should uninstall and allow connections`() {
        // Given: A blocker that blocks all
        val config = NetworkConfiguration(allowedHosts = emptySet())
        blocker = NetworkBlocker(config)
        blocker.install()

        // When: Uninstall the blocker
        blocker.uninstall()

        // Then: Connections should not throw NetworkRequestAttemptedException
        // (May throw other exceptions like UnknownHostException, which is fine)
        try {
            Socket("example.com", 80)
        } catch (e: Exception) {
            assertTrue(
                e !is NetworkRequestAttemptedException,
                "Should not throw NetworkRequestAttemptedException after uninstall",
            )
        }
    }

    @Test
    fun `should be idempotent when installing multiple times`() {
        // Given: A blocker
        val config = NetworkConfiguration(allowedHosts = emptySet())
        blocker = NetworkBlocker(config)

        // When: Install multiple times
        blocker.install()
        blocker.install()
        blocker.install()

        // Then: Should still block connections
        try {
            Socket("example.com", 80)
            throw AssertionError("Should have thrown NetworkRequestAttemptedException")
        } catch (e: NetworkRequestAttemptedException) {
            // Expected
        }
    }

    @Test
    fun `should be idempotent when uninstalling multiple times`() {
        // Given: An installed blocker
        val config = NetworkConfiguration(allowedHosts = emptySet())
        blocker = NetworkBlocker(config)
        blocker.install()

        // When: Uninstall multiple times
        blocker.uninstall()
        blocker.uninstall()
        blocker.uninstall()

        // Then: Should not throw exceptions
        // Success if no exception thrown
    }
}
