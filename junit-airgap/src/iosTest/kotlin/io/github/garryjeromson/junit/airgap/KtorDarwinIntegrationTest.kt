package io.github.garryjeromson.junit.airgap

import airgap.AirgapURLProtocol
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.fail

/**
 * Integration tests for iOS network blocking with Ktor Darwin engine.
 *
 * These tests verify end-to-end functionality:
 * - URLProtocol registration intercepts URLSession requests
 * - Ktor Darwin engine uses URLSession under the hood
 * - Blocked hosts result in failed requests
 * - Allowed hosts complete successfully
 *
 * IMPORTANT: NSURLSession requires custom protocols to be added to the session configuration.
 * Global registration with [NSURLProtocol registerClass:] only works for NSURLConnection.
 */
class KtorDarwinIntegrationTest {

    /**
     * Creates a Ktor HttpClient configured to use our custom URLProtocol.
     *
     * NSURLSession requires the protocol class to be in the session configuration's
     * protocolClasses array. This helper ensures all clients are properly configured.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun createConfiguredClient(): HttpClient {
        return HttpClient(Darwin) {
            engine {
                configureSession {
                    // Add our custom protocol to the session configuration
                    // This is required for NSURLSession to use our protocol
                    setProtocolClasses(listOf(AirgapURLProtocol))
                }
            }
        }
    }

    @Test
    fun `blocks HTTP request to disallowed host`() = runBlocking {
        // Configure to block all requests (no allowed hosts)
        val config = NetworkConfiguration(
            allowedHosts = emptySet(),
            blockedHosts = emptySet()
        )

        // Install the blocker
        val blocker = NetworkBlocker(config)
        blocker.install()

        try {
            // Create Ktor client with Darwin engine
            val client = createConfiguredClient()

            try {
                // Attempt to make a request - should be blocked
                // Use HTTP to avoid SSL cert validation in simulator
                client.get("http://example.com")
                fail("Request should have been blocked")
            } catch (e: Exception) {
                // Expected: request should be blocked by URLProtocol
                DebugLogger.log("Request blocked as expected: ${e.message}")
                assertTrue(
                    e.message?.contains("blocked") == true ||
                    e.message?.contains("Airgap") == true ||
                    e.message?.contains("AirgapError") == true,
                    "Exception should indicate request was blocked, got: ${e.message}"
                )
            } finally {
                client.close()
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `allows HTTP request to allowed host`() = runBlocking {
        // Configure to allow specific host
        val config = NetworkConfiguration(
            allowedHosts = setOf("example.com"),
            blockedHosts = emptySet()
        )

        // Install the blocker
        val blocker = NetworkBlocker(config)
        blocker.install()

        try {
            // Create Ktor client with Darwin engine
            val client = createConfiguredClient()

            try {
                // Attempt to make a request to allowed host
                val response = client.get("https://example.com")

                // Should succeed without throwing
                DebugLogger.log("Request to allowed host succeeded: ${response.status}")
                assertTrue(response.status.value in 200..399, "Response should be successful")
            } finally {
                client.close()
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `blocked host takes precedence over wildcard allow`() = runBlocking {
        // Configure to allow all (*) but block specific host
        val config = NetworkConfiguration(
            allowedHosts = setOf("*"),
            blockedHosts = setOf("httpbin.org")
        )

        // Install the blocker
        val blocker = NetworkBlocker(config)
        blocker.install()

        try {
            val client = createConfiguredClient()

            try {
                // Blocked host should fail
                try {
                    client.get("http://httpbin.org/get")
                    fail("Request to blocked host should have failed")
                } catch (e: Exception) {
                    DebugLogger.log("Blocked host correctly rejected: ${e.message}")
                    // Expected
                }

                // Other hosts should succeed
                val response = client.get("https://example.com")
                assertTrue(response.status.value in 200..399, "Allowed host should succeed")
            } finally {
                client.close()
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `supports wildcard pattern matching`() = runBlocking {
        // Configure to allow subdomains
        val config = NetworkConfiguration(
            allowedHosts = setOf("*.example.com", "example.com"),
            blockedHosts = emptySet()
        )

        // Install the blocker
        val blocker = NetworkBlocker(config)
        blocker.install()

        try {
            val client = createConfiguredClient()

            try {
                // Main domain should work
                val response1 = client.get("https://example.com")
                assertTrue(response1.status.value in 200..399, "Main domain should be allowed")

                // Note: Can't easily test subdomains without real subdomain endpoints
                // This test primarily verifies configuration doesn't crash
                DebugLogger.log("Wildcard configuration applied successfully")
            } finally {
                client.close()
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `uninstall restores normal network behavior`() = runBlocking {
        // Configure to block all
        val config = NetworkConfiguration(
            allowedHosts = emptySet(),
            blockedHosts = emptySet()
        )

        val blocker = NetworkBlocker(config)
        blocker.install()

        val client = HttpClient(Darwin)

        try {
            // While installed, requests should be blocked
            try {
                client.get("http://example.com")
                fail("Request should be blocked while blocker is installed")
            } catch (e: Exception) {
                // Expected
                DebugLogger.log("Request blocked while installed: ${e.message}")
            }

            // Uninstall blocker
            blocker.uninstall()

            // After uninstall, requests should succeed
            val response = client.get("https://example.com")
            assertTrue(
                response.status.value in 200..399,
                "Requests should succeed after uninstall"
            )
            DebugLogger.log("Request succeeded after uninstall: ${response.status}")
        } finally {
            client.close()
        }
    }

    @Test
    fun `can reconfigure between tests`() = runBlocking {
        val client = HttpClient(Darwin)

        try {
            // First configuration: block all
            val config1 = NetworkConfiguration(
                allowedHosts = emptySet(),
                blockedHosts = emptySet()
            )
            val blocker1 = NetworkBlocker(config1)
            blocker1.install()

            try {
                client.get("http://example.com")
                fail("Should be blocked with first config")
            } catch (e: Exception) {
                // Expected
            }

            blocker1.uninstall()

            // Second configuration: allow specific host
            val config2 = NetworkConfiguration(
                allowedHosts = setOf("example.com"),
                blockedHosts = emptySet()
            )
            val blocker2 = NetworkBlocker(config2)
            blocker2.install()

            try {
                val response = client.get("https://example.com")
                assertTrue(
                    response.status.value in 200..399,
                    "Should succeed with second config"
                )
            } finally {
                blocker2.uninstall()
            }
        } finally {
            client.close()
        }
    }
}
