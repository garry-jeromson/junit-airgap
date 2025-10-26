package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

/**
 * Tests that verify Spring WebClient network blocking works correctly
 * with the plugin auto-configuration (JUnit 5).
 */
class SpringWebClientTest {
    private fun makeSpringWebClientRequest(): String {
        val client = WebClient.create("https://example.com")
        return client
            .get()
            .uri("/")
            .retrieve()
            .bodyToMono(String::class.java)
            .block() ?: ""
    }

    @Test
    @BlockNetworkRequests
    fun springWebClientIsBlockedWithNoNetworkTest() {
        assertRequestBlocked {
            makeSpringWebClientRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun springWebClientIsAllowedWithAllowNetwork() {
        assertRequestAllowed {
            makeSpringWebClientRequest()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `spring webclient with spaces in test name is blocked`() {
        assertRequestBlocked {
            makeSpringWebClientRequest()
        }
    }
}
