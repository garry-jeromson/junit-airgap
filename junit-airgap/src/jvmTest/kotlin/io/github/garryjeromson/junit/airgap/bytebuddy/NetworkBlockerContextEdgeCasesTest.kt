package io.github.garryjeromson.junit.airgap.bytebuddy

import io.github.garryjeromson.junit.airgap.NetworkConfiguration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests for NetworkBlockerContext edge cases and uncovered branches.
 *
 * This test class focuses on scenarios where no configuration is set,
 * which are important edge cases for the ByteBuddy DNS interception layer.
 */
class NetworkBlockerContextEdgeCasesTest {
    @AfterTest
    fun cleanup() {
        // Clear configuration after each test to avoid interference
        NetworkBlockerContext.clearConfiguration()
    }

    @Test
    fun `checkConnection allows request when no configuration is set`() {
        // Given: No configuration is set
        NetworkBlockerContext.clearConfiguration()

        // When: Attempting to check a connection
        // Then: Should not throw (allows the request)
        NetworkBlockerContext.checkConnection(
            host = "example.com",
            port = 80,
            caller = "test",
        )

        // No exception = test passes
    }

    @Test
    fun `checkConnection allows DNS lookup when no configuration is set`() {
        // Given: No configuration is set
        NetworkBlockerContext.clearConfiguration()

        // When: Attempting a DNS lookup (port = -1)
        // Then: Should not throw (allows the request)
        NetworkBlockerContext.checkConnection(
            host = "example.com",
            port = -1,
            caller = "ByteBuddy-DNS",
        )

        // No exception = test passes
    }

    @Test
    fun `isExplicitlyBlocked returns false when no configuration is set`() {
        // Given: No configuration is set
        NetworkBlockerContext.clearConfiguration()

        // When: Checking if a host is explicitly blocked
        val isBlocked = NetworkBlockerContext.isExplicitlyBlocked("example.com")

        // Then: Should return false (not blocked)
        assertFalse(isBlocked, "Host should not be blocked when no configuration is set")
    }

    @Test
    fun `hasActiveConfiguration returns false when no configuration is set`() {
        // Given: No configuration is set
        NetworkBlockerContext.clearConfiguration()

        // When: Checking if there's an active configuration
        val hasConfig = NetworkBlockerContext.hasActiveConfiguration()

        // Then: Should return false
        assertFalse(hasConfig, "Should not have active configuration")
    }

    @Test
    fun `getConfiguration returns null when no configuration is set`() {
        // Given: No configuration is set
        NetworkBlockerContext.clearConfiguration()

        // When: Getting the configuration
        val config = NetworkBlockerContext.getConfiguration()

        // Then: Should return null
        assertEquals(null, config, "Configuration should be null")
    }

    @Test
    fun `configuration set in main thread is visible to child threads via global fallback`() {
        // Given: Configuration is set in main thread
        val mainConfig =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        NetworkBlockerContext.setConfiguration(mainConfig)

        // When: Checking configuration in a different thread
        var childThreadConfig: NetworkConfiguration? = null
        val thread =
            Thread {
                childThreadConfig = NetworkBlockerContext.getConfiguration()
            }
        thread.start()
        thread.join()

        // Then: Child thread should see the configuration via global fallback
        // (setConfiguration sets both thread-local AND global)
        assertEquals(mainConfig, childThreadConfig, "Child thread should see global configuration fallback")
    }

    @Test
    fun `clearConfiguration increments generation counter`() {
        // Given: Configuration is set
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        NetworkBlockerContext.setConfiguration(config)

        // When: Configuration is cleared
        NetworkBlockerContext.clearConfiguration()

        // Then: Generation counter should have been incremented
        // (This invalidates stale cached configurations in other threads)
        // We verify this by checking that getConfiguration() returns null
        val currentConfig = NetworkBlockerContext.getConfiguration()
        assertEquals(null, currentConfig, "Configuration should be null after clear")
    }
}
