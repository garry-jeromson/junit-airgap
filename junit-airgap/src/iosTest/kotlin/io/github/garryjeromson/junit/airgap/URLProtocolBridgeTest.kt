package io.github.garryjeromson.junit.airgap

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the Objective-C ↔ Kotlin/Native bridge for URLProtocol.
 *
 * These tests verify that:
 * 1. Kotlin can call into Objective-C (registerURLProtocol, etc.)
 * 2. Objective-C can call back into Kotlin (shouldBlockHost check)
 * 3. Configuration is properly marshalled between the two
 */
class URLProtocolBridgeTest {

    @Test
    fun `can register URLProtocol from Kotlin`() {
        // Should not crash
        val result = registerURLProtocol()
        assertTrue(result, "URLProtocol registration should succeed")
    }

    @Test
    fun `can unregister URLProtocol from Kotlin`() {
        registerURLProtocol()

        // Should not crash
        val result = unregisterURLProtocol()
        assertTrue(result, "URLProtocol unregistration should succeed")
    }

    @Test
    fun `can set configuration from Kotlin`() {
        val config = NetworkConfiguration(
            allowedHosts = setOf("localhost", "127.0.0.1"),
            blockedHosts = setOf("evil.com")
        )

        // Should not crash
        val result = setURLProtocolConfiguration(config)
        assertTrue(result, "Configuration should be set successfully")
    }

    @Test
    fun `Objective-C can call back into Kotlin to check host`() {
        // Set up configuration
        val config = NetworkConfiguration(
            allowedHosts = setOf("localhost"),
            blockedHosts = emptySet()
        )
        NetworkBlocker.setSharedConfiguration(config)

        // Simulate Objective-C calling our exported function
        val blockedResult = shouldBlockHost("example.com")
        assertTrue(blockedResult, "example.com should be blocked")

        val allowedResult = shouldBlockHost("localhost")
        assertFalse(allowedResult, "localhost should be allowed")
    }

    @Test
    fun `blocked hosts take precedence over allowed hosts`() {
        val config = NetworkConfiguration(
            allowedHosts = setOf("*"),  // Allow all
            blockedHosts = setOf("evil.com")  // Except this
        )
        NetworkBlocker.setSharedConfiguration(config)

        val blockedResult = shouldBlockHost("evil.com")
        assertTrue(blockedResult, "evil.com should be blocked despite wildcard allow")

        val allowedResult = shouldBlockHost("example.com")
        assertFalse(allowedResult, "example.com should be allowed")
    }

    @Test
    fun `supports wildcard patterns in host matching`() {
        val config = NetworkConfiguration(
            allowedHosts = setOf("*.example.com"),
            blockedHosts = emptySet()
        )
        NetworkBlocker.setSharedConfiguration(config)

        val allowedSubdomain = shouldBlockHost("api.example.com")
        assertFalse(allowedSubdomain, "api.example.com should match *.example.com")

        val blockedOther = shouldBlockHost("other.com")
        assertTrue(blockedOther, "other.com should not match *.example.com")
    }

    @Test
    fun `configuration persists across multiple checks`() {
        val config = NetworkConfiguration(
            allowedHosts = setOf("localhost"),
            blockedHosts = emptySet()
        )
        NetworkBlocker.setSharedConfiguration(config)

        // Multiple checks should use same configuration
        assertTrue(shouldBlockHost("example.com"))
        assertFalse(shouldBlockHost("localhost"))
        assertTrue(shouldBlockHost("evil.com"))
        assertFalse(shouldBlockHost("localhost"))  // Still allowed
    }
}
