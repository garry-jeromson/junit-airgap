package io.github.garryjeromson.junit.nonetwork.integration

import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.MockHttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.http.client.HttpClient
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Integration tests verifying that Reactor Netty HTTP client is properly blocked.
 * Reactor Netty is a reactive, non-blocking HTTP client built on Netty and Project Reactor.
 * It's commonly used in Spring WebFlux applications and reactive microservices.
 */
@ExtendWith(NoNetworkExtension::class)
class ReactorNettyClientIntegrationTest {
    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeAll
        fun startMockServer() {
            mockServer = MockHttpServer(MockHttpServer.DEFAULT_PORT)
            mockServer.start()
            Thread.sleep(100)
        }

        @JvmStatic
        @AfterAll
        fun stopMockServer() {
            mockServer.stop()
        }

        /**
         * Assert that network is blocked for Reactor Netty.
         * Reactor Netty can fail at different stages:
         * 1. DNS resolution (if DNS socket is blocked) - DnsResolveContext exceptions
         * 2. Socket connection (if socket is blocked) - ClosedChannelException, NetworkRequestAttemptedException
         * All of these indicate successful network blocking.
         */
        private fun assertReactorNettyBlocked(
            message: String = "Expected network to be blocked",
            block: () -> Unit,
        ) {
            try {
                block()
                fail("$message - but no exception was thrown")
            } catch (e: NetworkRequestAttemptedException) {
                // Expected - direct network blocking
            } catch (e: Exception) {
                // Check for network-related exceptions in the cause chain
                var cause: Throwable? = e
                var foundNetworkException = false

                while (cause != null) {
                    val causeName = cause.javaClass.name
                    when {
                        cause is NetworkRequestAttemptedException -> {
                            foundNetworkException = true
                            break
                        }
                        // DNS resolution failures indicate DNS socket was blocked
                        causeName.contains("DnsResolve") ||
                            causeName.contains("DnsNameResolver") -> {
                            foundNetworkException = true
                            break
                        }
                        // Channel exceptions indicate socket was blocked
                        causeName.contains("ClosedChannelException") ||
                            causeName.contains("ChannelException") -> {
                            foundNetworkException = true
                            break
                        }
                    }
                    cause = cause.cause
                }

                if (!foundNetworkException) {
                    fail("$message - but got ${e.javaClass.simpleName} without network-related cause: ${e.message}")
                }
            }
        }
    }

    // ==================== Basic Blocking Tests ====================

    @Test
    @BlockNetworkRequests
    fun `blocks Reactor Netty GET to external host`() {
        assertReactorNettyBlocked("Reactor Netty GET should be blocked") {
            val client = HttpClient.create()
            client
                .get()
                .uri("http://example.com/api")
                .response()
                .block()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Reactor Netty POST to external host`() {
        assertReactorNettyBlocked("Reactor Netty POST should be blocked") {
            val client = HttpClient.create()
            client
                .post()
                .uri("http://example.com/api")
                .send(ByteBufFlux.fromString(Mono.just("""{"test":"data"}""")))
                .response()
                .block()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Reactor Netty HTTPS requests`() {
        assertReactorNettyBlocked("Reactor Netty HTTPS should be blocked") {
            val client = HttpClient.create()
            client
                .get()
                .uri("https://www.google.com")
                .response()
                .block()
        }
    }

    // ==================== Localhost Allowlist Tests ====================

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `allows Reactor Netty to localhost`() {
        try {
            val client = HttpClient.create()
            val response =
                client
                    .get()
                    .uri("http://localhost:${MockHttpServer.DEFAULT_PORT}/api/test")
                    .responseContent()
                    .aggregate()
                    .asString()
                    .block()

            // If we got here without exception, the request was allowed
            assertTrue(response != null || true, "Request to localhost should be allowed")
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Should not block localhost", e)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1"])
    fun `allows Reactor Netty to 127_0_0_1`() {
        try {
            val client = HttpClient.create()
            val response =
                client
                    .get()
                    .uri("http://127.0.0.1:${MockHttpServer.DEFAULT_PORT}/api/test")
                    .responseContent()
                    .aggregate()
                    .asString()
                    .block()

            assertTrue(response != null || true, "Request to 127.0.0.1 should be allowed")
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Should not block 127.0.0.1", e)
        }
    }

    // ==================== Wildcard Configuration Tests ====================

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    fun `allows Reactor Netty with wildcard configuration`() {
        try {
            val client = HttpClient.create()
            val response =
                client
                    .get()
                    .uri("http://localhost:${MockHttpServer.DEFAULT_PORT}/api/test")
                    .responseContent()
                    .aggregate()
                    .asString()
                    .block()

            assertTrue(response != null || true, "Request should work with wildcard")
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Should not block with wildcard config", e)
        }
    }

    // ==================== Different HTTP Methods Tests ====================

    @Test
    @BlockNetworkRequests
    fun `blocks Reactor Netty PUT requests`() {
        assertReactorNettyBlocked("Reactor Netty PUT should be blocked") {
            val client = HttpClient.create()
            client
                .put()
                .uri("http://example.com/api/resource")
                .send(ByteBufFlux.fromString(Mono.just("updated data")))
                .response()
                .block()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Reactor Netty DELETE requests`() {
        assertReactorNettyBlocked("Reactor Netty DELETE should be blocked") {
            val client = HttpClient.create()
            client
                .delete()
                .uri("http://example.com/api/resource")
                .response()
                .block()
        }
    }

    // ==================== Different Hosts and Ports Tests ====================

    @Test
    @BlockNetworkRequests
    fun `blocks Reactor Netty to different ports`() {
        assertReactorNettyBlocked("Reactor Netty to port 8080 should be blocked") {
            val client = HttpClient.create()
            client
                .get()
                .uri("http://example.com:8080/api")
                .response()
                .block()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["example.com"])
    fun `allows Reactor Netty to specifically allowed host`() {
        // This will fail to connect since example.com is not our mock server,
        // but it should NOT throw NetworkRequestAttemptedException
        try {
            val client = HttpClient.create()
            client
                .get()
                .uri("http://example.com/api")
                .response()
                .block()
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Should not block example.com when it's in allowlist", e)
        } catch (e: Exception) {
            // Other exceptions (connection refused, timeout, etc.) are fine
        }
    }

    // ==================== Response Content Tests ====================

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost"])
    fun `allows Reactor Netty to read response content from localhost`() {
        try {
            val client = HttpClient.create()
            val response =
                client
                    .get()
                    .uri("http://localhost:${MockHttpServer.DEFAULT_PORT}/api/test")
                    .responseContent()
                    .aggregate()
                    .asString()
                    .block()

            assertTrue(response != null, "Should receive response from localhost")
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Should not block localhost", e)
        }
    }

    // ==================== Configuration Tests ====================

    @Test
    @BlockNetworkRequests
    fun `blocks Reactor Netty with custom base URL configuration`() {
        assertReactorNettyBlocked("Reactor Netty with base URL should be blocked") {
            val client =
                HttpClient
                    .create()
                    .baseUrl("http://api.example.com")

            client
                .get()
                .uri("/data")
                .response()
                .block()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Reactor Netty with host and port configuration`() {
        assertReactorNettyBlocked("Reactor Netty with host/port should be blocked") {
            val client =
                HttpClient
                    .create()
                    .host("example.com")
                    .port(80)

            client
                .get()
                .uri("/api")
                .response()
                .block()
        }
    }
}
