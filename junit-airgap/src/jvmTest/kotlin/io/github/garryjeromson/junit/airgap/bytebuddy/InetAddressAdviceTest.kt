package io.github.garryjeromson.junit.airgap.bytebuddy

import io.github.garryjeromson.junit.airgap.NetworkConfiguration
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for ByteBuddy advice classes that intercept DNS lookups.
 *
 * These tests verify that the advice methods correctly use reflection to
 * find and invoke NetworkBlockerContext.checkConnection(), and handle
 * various error conditions gracefully.
 *
 * Note: These tests directly invoke the advice methods, simulating what
 * ByteBuddy does when the advice is inlined into InetAddress methods.
 */
class InetAddressAdviceTest {
    @AfterTest
    fun cleanup() {
        // Clear configuration after each test
        NetworkBlockerContext.clearConfiguration()
    }

    @Test
    fun `getAllByName advice blocks unauthorized DNS lookup`() {
        // Given: Configuration blocks example.com
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = setOf("example.com"),
            )
        NetworkBlockerContext.setConfiguration(config)

        // When: Advice intercepts getAllByName("example.com")
        // Then: Should throw NetworkRequestAttemptedException
        assertFailsWith<NetworkRequestAttemptedException> {
            InetAddressGetAllByNameAdvice.enter("example.com")
        }
    }

    @Test
    fun `getAllByName advice allows authorized DNS lookup`() {
        // Given: Configuration allows localhost
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        NetworkBlockerContext.setConfiguration(config)

        // When: Advice intercepts getAllByName("localhost")
        // Then: Should not throw
        InetAddressGetAllByNameAdvice.enter("localhost")

        // No exception = test passes
    }

    @Test
    fun `getAllByName advice allows localhost when host is null`() {
        // Given: Configuration blocks all hosts
        val config =
            NetworkConfiguration(
                allowedHosts = emptySet(),
                blockedHosts = setOf("*"),
            )
        NetworkBlockerContext.setConfiguration(config)

        // When: Advice intercepts getAllByName(null) - represents localhost
        // Then: Should not throw (null is always allowed)
        InetAddressGetAllByNameAdvice.enter(null)

        // No exception = test passes
    }

    @Test
    fun `getAllByName advice allows request when NetworkBlockerContext not configured`() {
        // Given: No configuration is set
        NetworkBlockerContext.clearConfiguration()

        // When: Advice intercepts getAllByName("example.com")
        // Then: Should not throw (no config = allow all)
        InetAddressGetAllByNameAdvice.enter("example.com")

        // No exception = test passes
    }

    @Test
    fun `getByName advice blocks unauthorized DNS lookup`() {
        // Given: Configuration blocks example.com
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = setOf("example.com"),
            )
        NetworkBlockerContext.setConfiguration(config)

        // When: Advice intercepts getByName("example.com")
        // Then: Should throw NetworkRequestAttemptedException
        assertFailsWith<NetworkRequestAttemptedException> {
            InetAddressGetByNameAdvice.enter("example.com")
        }
    }

    @Test
    fun `getByName advice allows authorized DNS lookup`() {
        // Given: Configuration allows localhost
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("localhost"),
                blockedHosts = emptySet(),
            )
        NetworkBlockerContext.setConfiguration(config)

        // When: Advice intercepts getByName("localhost")
        // Then: Should not throw
        InetAddressGetByNameAdvice.enter("localhost")

        // No exception = test passes
    }

    @Test
    fun `getByName advice allows localhost when host is null`() {
        // Given: Configuration blocks all hosts
        val config =
            NetworkConfiguration(
                allowedHosts = emptySet(),
                blockedHosts = setOf("*"),
            )
        NetworkBlockerContext.setConfiguration(config)

        // When: Advice intercepts getByName(null) - represents localhost
        // Then: Should not throw (null is always allowed)
        InetAddressGetByNameAdvice.enter(null)

        // No exception = test passes
    }

    @Test
    fun `getByName advice allows request when NetworkBlockerContext not configured`() {
        // Given: No configuration is set
        NetworkBlockerContext.clearConfiguration()

        // When: Advice intercepts getByName("example.com")
        // Then: Should not throw (no config = allow all)
        InetAddressGetByNameAdvice.enter("example.com")

        // No exception = test passes
    }

    @Test
    fun `advice uses reflection to find NetworkBlockerContext dynamically`() {
        // This test verifies that the advice can successfully use reflection
        // to find and invoke NetworkBlockerContext.checkConnection()

        // Given: Configuration is set
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("allowed.com"),
                blockedHosts = setOf("blocked.com"),
            )
        NetworkBlockerContext.setConfiguration(config)

        // When: Advice uses reflection to check both allowed and blocked hosts
        // Then: Should correctly allow/block based on configuration

        // Allowed host should not throw
        InetAddressGetAllByNameAdvice.enter("allowed.com")

        // Blocked host should throw
        assertFailsWith<NetworkRequestAttemptedException> {
            InetAddressGetAllByNameAdvice.enter("blocked.com")
        }
    }

    @Test
    fun `advice works with wildcard patterns in configuration`() {
        // Given: Configuration with wildcard patterns
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("*.example.com"),
                blockedHosts = setOf("bad.*"),
            )
        NetworkBlockerContext.setConfiguration(config)

        // When/Then: Wildcard matching should work through reflection

        // Allowed wildcard match
        InetAddressGetAllByNameAdvice.enter("subdomain.example.com")

        // Blocked wildcard match
        assertFailsWith<NetworkRequestAttemptedException> {
            InetAddressGetAllByNameAdvice.enter("bad.domain.com")
        }
    }
}
