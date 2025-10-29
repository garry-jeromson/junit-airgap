package io.github.garryjeromson.junit.airgap

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for the Objective-C ↔ Kotlin/Native bridge for URLProtocol.
 *
 * These tests verify that:
 * 1. Kotlin can call into Objective-C (registerURLProtocol, etc.)
 * 2. Objective-C can call back into Kotlin (via staticCFunction callback)
 * 3. Configuration is properly marshalled between the two
 *
 * Note: Full end-to-end testing with actual network requests will be done
 * in integration tests with Ktor Darwin engine.
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
    fun `can set configuration with wildcards`() {
        val config = NetworkConfiguration(
            allowedHosts = setOf("*.example.com"),
            blockedHosts = setOf("evil.example.com")
        )

        // Should not crash
        val result = setURLProtocolConfiguration(config)
        assertTrue(result, "Configuration with wildcards should be set successfully")
    }

    @Test
    fun `can set empty configuration`() {
        val config = NetworkConfiguration(
            allowedHosts = emptySet(),
            blockedHosts = emptySet()
        )

        // Should not crash - empty config means block all
        val result = setURLProtocolConfiguration(config)
        assertTrue(result, "Empty configuration should be set successfully")
    }

    @Test
    fun `can set configuration with allow-all`() {
        val config = NetworkConfiguration(
            allowedHosts = setOf("*"),
            blockedHosts = emptySet()
        )

        // Should not crash
        val result = setURLProtocolConfiguration(config)
        assertTrue(result, "Allow-all configuration should be set successfully")
    }
}
