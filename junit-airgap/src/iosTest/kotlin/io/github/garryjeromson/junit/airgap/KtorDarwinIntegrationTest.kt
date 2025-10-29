package io.github.garryjeromson.junit.airgap

import airgap.AirgapURLProtocol
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
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
 *
 * Uses embedded Ktor server on localhost to avoid external network dependencies.
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

    /**
     * Creates a local test server running on the specified port.
     *
     * The server provides simple endpoints for testing:
     * - GET /success -> Returns "OK"
     * - GET /data -> Returns "Test data"
     */
    private fun createTestServer(port: Int): ApplicationEngine {
        return embeddedServer(CIO, port = port) {
            routing {
                get("/success") {
                    call.respondText("OK", ContentType.Text.Plain, HttpStatusCode.OK)
                }
                get("/data") {
                    call.respondText("Test data", ContentType.Text.Plain, HttpStatusCode.OK)
                }
            }
        }
    }

    /**
     * Starts the test server and waits briefly for it to be ready.
     */
    private suspend fun startTestServer(server: ApplicationEngine) {
        server.start(wait = false)
        delay(100) // Give server time to start
    }

    @Test
    fun `blocks HTTP request to disallowed host`() = runBlocking {
        val testPort = 8080
        val server = createTestServer(testPort)

        try {
            startTestServer(server)

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
                    client.get("http://localhost:$testPort/success")
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
        } finally {
            server.stop(1000, 2000)
        }
    }

    @Test
    fun `allows HTTP request to allowed host`() = runBlocking {
        val testPort = 8081
        val server = createTestServer(testPort)

        try {
            startTestServer(server)

            // Configure to allow localhost
            val config = NetworkConfiguration(
                allowedHosts = setOf("localhost"),
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
                    val response = client.get("http://localhost:$testPort/success")

                    // Should succeed without throwing
                    DebugLogger.log("Request to allowed host succeeded: ${response.status}")
                    assertTrue(response.status.value in 200..399, "Response should be successful")

                    val body = response.bodyAsText()
                    assertTrue(body == "OK", "Response body should be 'OK', got: $body")
                } finally {
                    client.close()
                }
            } finally {
                blocker.uninstall()
            }
        } finally {
            server.stop(1000, 2000)
        }
    }

    @Test
    fun `blocked host takes precedence over wildcard allow`() = runBlocking {
        val testPort1 = 8082
        val testPort2 = 8083
        val server1 = createTestServer(testPort1)
        val server2 = createTestServer(testPort2)

        try {
            startTestServer(server1)
            startTestServer(server2)

            // Configure to allow all (*) but block localhost
            // We'll use 127.0.0.1 as the blocked host to test precedence
            val config = NetworkConfiguration(
                allowedHosts = setOf("*"),
                blockedHosts = setOf("127.0.0.1")
            )

            // Install the blocker
            val blocker = NetworkBlocker(config)
            blocker.install()

            try {
                val client = createConfiguredClient()

                try {
                    // Blocked host (127.0.0.1) should fail even though * is allowed
                    try {
                        client.get("http://127.0.0.1:$testPort1/success")
                        fail("Request to blocked host should have failed")
                    } catch (e: Exception) {
                        DebugLogger.log("Blocked host correctly rejected: ${e.message}")
                        // Expected
                    }

                    // Localhost should succeed (different from 127.0.0.1)
                    val response = client.get("http://localhost:$testPort2/success")
                    assertTrue(response.status.value in 200..399, "Allowed host should succeed")
                } finally {
                    client.close()
                }
            } finally {
                blocker.uninstall()
            }
        } finally {
            server1.stop(1000, 2000)
            server2.stop(1000, 2000)
        }
    }

    @Test
    fun `supports wildcard pattern matching`() = runBlocking {
        val testPort = 8084
        val server = createTestServer(testPort)

        try {
            startTestServer(server)

            // Configure to allow all hosts using wildcard
            val config = NetworkConfiguration(
                allowedHosts = setOf("*"),
                blockedHosts = emptySet()
            )

            // Install the blocker
            val blocker = NetworkBlocker(config)
            blocker.install()

            try {
                val client = createConfiguredClient()

                try {
                    // Wildcard should allow localhost
                    val response1 = client.get("http://localhost:$testPort/success")
                    assertTrue(response1.status.value in 200..399, "Localhost should be allowed by wildcard")

                    // Wildcard should allow 127.0.0.1
                    val response2 = client.get("http://127.0.0.1:$testPort/data")
                    assertTrue(response2.status.value in 200..399, "127.0.0.1 should be allowed by wildcard")

                    DebugLogger.log("Wildcard configuration applied successfully")
                } finally {
                    client.close()
                }
            } finally {
                blocker.uninstall()
            }
        } finally {
            server.stop(1000, 2000)
        }
    }

    @Test
    fun `uninstall restores normal network behavior`() = runBlocking {
        val testPort = 8085
        val server = createTestServer(testPort)

        try {
            startTestServer(server)

            // Configure to block all
            val config = NetworkConfiguration(
                allowedHosts = emptySet(),
                blockedHosts = emptySet()
            )

            val blocker = NetworkBlocker(config)
            blocker.install()

            // Use configured client that has our protocol in protocolClasses
            val client = createConfiguredClient()

            try {
                // While installed, requests should be blocked
                try {
                    client.get("http://localhost:$testPort/success")
                    fail("Request should be blocked while blocker is installed")
                } catch (e: Exception) {
                    // Expected
                    DebugLogger.log("Request blocked while installed: ${e.message}")
                }

                // Uninstall blocker
                blocker.uninstall()

                // After uninstall, requests should succeed
                val response = client.get("http://localhost:$testPort/success")
                assertTrue(
                    response.status.value in 200..399,
                    "Requests should succeed after uninstall"
                )
                DebugLogger.log("Request succeeded after uninstall: ${response.status}")
            } finally {
                client.close()
            }
        } finally {
            server.stop(1000, 2000)
        }
    }

    @Test
    fun `can reconfigure between tests`() = runBlocking {
        val testPort = 8086
        val server = createTestServer(testPort)

        try {
            startTestServer(server)

            // Use configured client that has our protocol in protocolClasses
            val client = createConfiguredClient()

            try {
                // First configuration: block all
                val config1 = NetworkConfiguration(
                    allowedHosts = emptySet(),
                    blockedHosts = emptySet()
                )
                val blocker1 = NetworkBlocker(config1)
                blocker1.install()

                try {
                    client.get("http://localhost:$testPort/success")
                    fail("Should be blocked with first config")
                } catch (e: Exception) {
                    // Expected
                    DebugLogger.log("Request blocked with first config: ${e.message}")
                }

                blocker1.uninstall()

                // Second configuration: allow localhost
                val config2 = NetworkConfiguration(
                    allowedHosts = setOf("localhost"),
                    blockedHosts = emptySet()
                )
                val blocker2 = NetworkBlocker(config2)
                blocker2.install()

                try {
                    val response = client.get("http://localhost:$testPort/success")
                    assertTrue(
                        response.status.value in 200..399,
                        "Should succeed with second config"
                    )
                    DebugLogger.log("Request succeeded with second config: ${response.status}")
                } finally {
                    blocker2.uninstall()
                }
            } finally {
                client.close()
            }
        } finally {
            server.stop(1000, 2000)
        }
    }
}
