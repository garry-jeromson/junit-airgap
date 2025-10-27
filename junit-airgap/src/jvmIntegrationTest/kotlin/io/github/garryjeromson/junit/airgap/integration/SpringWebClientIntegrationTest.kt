package io.github.garryjeromson.junit.airgap.integration

import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.AirgapExtension
import io.github.garryjeromson.junit.airgap.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.airgap.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.airgap.integration.fixtures.assertNetworkNotBlocked
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient

/**
 * Integration tests verifying that Spring WebClient is properly blocked.
 * Spring WebClient is the reactive HTTP client used in Spring Boot WebFlux applications.
 */
@ExtendWith(AirgapExtension::class)
class SpringWebClientIntegrationTest {
    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeAll
        fun startMockServer() {
            mockServer = MockHttpServer()
            mockServer.start()
            Thread.sleep(100)
        }

        @JvmStatic
        @AfterAll
        fun stopMockServer() {
            mockServer.stop()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Spring WebClient GET requests to external host`() {
        assertNetworkBlocked("Spring WebClient should be blocked") {
            val client = WebClient.create("http://example.com")
            client
                .get()
                .uri("/api/test")
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Spring WebClient POST requests to external host`() {
        assertNetworkBlocked("Spring WebClient POST should be blocked") {
            val client = WebClient.create("http://example.com")
            client
                .post()
                .uri("/api/submit")
                .bodyValue("test data")
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `allows Spring WebClient to localhost`() {
        assertNetworkNotBlocked("Spring WebClient to localhost should work") {
            val client = WebClient.create("http://localhost:${mockServer.listeningPort}")
            val response =
                client
                    .get()
                    .uri("/api/test")
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()
            // Verify response was received
            response
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1"])
    fun `allows Spring WebClient to allowed IP address`() {
        assertNetworkNotBlocked("Spring WebClient to 127.0.0.1 should work") {
            val client = WebClient.create("http://127.0.0.1:${mockServer.listeningPort}")
            val response =
                client
                    .get()
                    .uri("/api/test")
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()
            response
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    fun `allows Spring WebClient with wildcard configuration`() {
        assertNetworkNotBlocked("Spring WebClient should work with wildcard") {
            val client = WebClient.create("http://localhost:${mockServer.listeningPort}")
            val response =
                client
                    .get()
                    .uri("/api/test")
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()
            response
        }
    }
}
