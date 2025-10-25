package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import org.junit.Test
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.http.client.HttpClient
import java.time.Duration

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
    private fun createReactorNettyClient(): HttpClient =
        HttpClient
            .create()
            .resolver { spec ->
                // Use minimal DNS cache TTL (1 second) to ensure test isolation
                // Netty's TTL resolution is in seconds, so 1 second is the minimum
                spec.cacheMaxTimeToLive(Duration.ofSeconds(1))
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
                .uri("https://example.com/data")
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
        assertRequestBlocked {
            makeReactorNettyRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun reactorNettyIsAllowedWithAllowNetwork() {
        assertRequestAllowed {
            makeReactorNettyRequest()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `reactor netty with spaces in test name is blocked`() {
        assertRequestBlocked {
            makeReactorNettyPostRequest()
        }
    }
}
