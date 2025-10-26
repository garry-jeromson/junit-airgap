package io.github.garryjeromson.junit.nonetwork.integration

import feign.Feign
import feign.RequestLine
import feign.okhttp.OkHttpClient
import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkNotBlocked
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Integration tests verifying that Feign (OpenFeign) HTTP client is properly blocked.
 * Feign is the declarative REST client used extensively in Spring Cloud microservices.
 */
@ExtendWith(NoNetworkExtension::class)
class FeignClientIntegrationTest {
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

    // Feign client interface for testing
    interface TestApiClient {
        @RequestLine("GET /api/test")
        fun getTest(): String

        @RequestLine("GET /api/data")
        fun getData(): String

        @RequestLine("POST /api/submit")
        fun postData(body: String): String
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Feign GET requests to external host`() {
        assertNetworkBlocked("Feign should be blocked") {
            val client =
                Feign
                    .builder()
                    .client(OkHttpClient())
                    .target(TestApiClient::class.java, "http://example.com")

            client.getTest()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Feign POST requests to external host`() {
        assertNetworkBlocked("Feign POST should be blocked") {
            val client =
                Feign
                    .builder()
                    .client(OkHttpClient())
                    .target(TestApiClient::class.java, "https://api.example.com")

            client.postData("test data")
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `allows Feign to localhost`() {
        assertNetworkNotBlocked("Feign to localhost should work") {
            val client =
                Feign
                    .builder()
                    .client(OkHttpClient())
                    .target(TestApiClient::class.java, "http://localhost:${mockServer.listeningPort}")

            val response = client.getTest()
            // Verify response was received
            response
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1"])
    fun `allows Feign to allowed IP address`() {
        assertNetworkNotBlocked("Feign to 127.0.0.1 should work") {
            val client =
                Feign
                    .builder()
                    .client(OkHttpClient())
                    .target(TestApiClient::class.java, "http://127.0.0.1:${mockServer.listeningPort}")

            val response = client.getTest()
            response
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    fun `allows Feign with wildcard configuration`() {
        assertNetworkNotBlocked("Feign should work with wildcard") {
            val client =
                Feign
                    .builder()
                    .client(OkHttpClient())
                    .target(TestApiClient::class.java, "http://localhost:${mockServer.listeningPort}")

            val response = client.getTest()
            response
        }
    }
}
