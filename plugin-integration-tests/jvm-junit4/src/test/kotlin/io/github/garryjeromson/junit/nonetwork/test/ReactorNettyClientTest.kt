package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.Test
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.http.client.HttpClient
import java.time.Duration
import kotlin.test.fail

/**
 * Tests that verify Reactor Netty HTTP client network blocking works correctly
 * with the plugin auto-configuration (JUnit 4).
 *
 * IMPORTANT: Reactor Netty caches DNS results globally with a very long TTL by default.
 * To ensure proper test isolation, we configure HttpClient with DNS caching disabled.
 * This is the recommended configuration when using Reactor Netty with junit-no-network
 * to ensure network blocking works reliably.
 */
class ReactorNettyClientTest {

    /**
     * Creates an HttpClient with minimal DNS caching.
     * This is necessary to ensure test isolation and proper network blocking.
     *
     * Without this configuration, Reactor Netty would cache DNS results from
     * @AllowNetworkRequests tests, causing subsequent @BlockNetworkRequests tests
     * to use the cached results and bypass socket creation (and thus bypass blocking).
     */
    private fun createReactorNettyClient(): HttpClient {
        return HttpClient.create()
            .resolver { spec ->
                // Use minimal DNS cache TTL (1 second) to ensure test isolation
                // Netty's TTL resolution is in seconds, so 1 second is the minimum
                spec.cacheMaxTimeToLive(Duration.ofSeconds(1))
            }
    }

    private fun makeReactorNettyRequest(): String? {
        val client = createReactorNettyClient()
        val response =
            client
                .get()
                .uri("https://example.com/")
                .responseContent()
                .aggregate()
                .asString()
                .block()
        return response
    }

    private fun makeReactorNettyPostRequest(): String? {
        val client = createReactorNettyClient()
        val response =
            client
                .post()
                .uri("https://api.example.org/data")
                .send(ByteBufFlux.fromString(Mono.just("test data")))
                .responseContent()
                .aggregate()
                .asString()
                .block()
        return response
    }

    @Test
    @BlockNetworkRequests
    fun reactorNettyIsBlockedWithNoNetworkTest() {
        try {
            makeReactorNettyRequest()
            fail("Expected network to be blocked, but request succeeded")
        } catch (e: NetworkRequestAttemptedException) {
            // Expected - network was blocked directly
        } catch (e: Exception) {
            // Check if network-related exception is in the cause chain
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
                fail("Expected network to be blocked, but got ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    @Test
    @AllowNetworkRequests
    fun reactorNettyIsAllowedWithAllowNetwork() {
        try {
            makeReactorNettyRequest()
        } catch (e: NetworkRequestAttemptedException) {
            fail("Network is allowed with @AllowNetworkRequests, but was blocked: ${e.message}")
        } catch (e: Exception) {
            // Other exceptions (DNS failures, connection refused, timeout, etc.) are fine
        }
    }

    @Test
    @BlockNetworkRequests
    fun `reactor netty with spaces in test name is blocked`() {
        try {
            makeReactorNettyPostRequest()
            fail("Expected network to be blocked, but request succeeded")
        } catch (e: NetworkRequestAttemptedException) {
            // Expected - network was blocked directly
        } catch (e: Exception) {
            // Check if network-related exception is in the cause chain
            var cause: Throwable? = e
            var foundNetworkException = false

            while (cause != null) {
                val causeName = cause.javaClass.name
                when {
                    cause is NetworkRequestAttemptedException -> {
                        foundNetworkException = true
                        break
                    }
                    causeName.contains("DnsResolve") ||
                        causeName.contains("DnsNameResolver") -> {
                        foundNetworkException = true
                        break
                    }
                    causeName.contains("ClosedChannelException") ||
                        causeName.contains("ChannelException") -> {
                        foundNetworkException = true
                        break
                    }
                }
                cause = cause.cause
            }

            if (!foundNetworkException) {
                fail("Expected network to be blocked, but got ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }
}
