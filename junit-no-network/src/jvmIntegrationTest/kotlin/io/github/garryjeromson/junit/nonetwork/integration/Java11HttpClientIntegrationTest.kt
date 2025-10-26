package io.github.garryjeromson.junit.nonetwork.integration

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
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Integration tests verifying that Java 11+ HttpClient is properly blocked.
 * java.net.http.HttpClient is the modern replacement for HttpURLConnection,
 * introduced in Java 11 and commonly used in modern JVM projects.
 */
@ExtendWith(NoNetworkExtension::class)
class Java11HttpClientIntegrationTest {
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
    fun `blocks Java 11 HttpClient to external host`() {
        assertNetworkBlocked("Java 11 HttpClient should be blocked") {
            val client =
                HttpClient
                    .newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()

            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://example.com/api"))
                    .GET()
                    .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `allows Java 11 HttpClient to localhost`() {
        assertNetworkNotBlocked("Java 11 HttpClient to localhost should work") {
            val client =
                HttpClient
                    .newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()

            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://localhost:${mockServer.listeningPort}/api/test"))
                    .GET()
                    .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.body() // Access response body to ensure call completed
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Java 11 HttpClient HTTPS requests`() {
        assertNetworkBlocked("Java 11 HttpClient HTTPS should be blocked") {
            val client =
                HttpClient
                    .newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()

            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("https://www.google.com"))
                    .GET()
                    .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Java 11 HttpClient POST requests`() {
        assertNetworkBlocked("Java 11 HttpClient POST should be blocked") {
            val client = HttpClient.newHttpClient()

            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://example.com/api/data"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"key\":\"value\"}"))
                    .header("Content-Type", "application/json")
                    .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    fun `allows Java 11 HttpClient when wildcard is configured`() {
        assertNetworkNotBlocked("Java 11 HttpClient should work with wildcard") {
            val client = HttpClient.newHttpClient()

            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://localhost:${mockServer.listeningPort}/api/test"))
                    .GET()
                    .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.body()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1"])
    fun `allows Java 11 HttpClient to allowed IP address`() {
        assertNetworkNotBlocked("Java 11 HttpClient to 127.0.0.1 should work") {
            val client = HttpClient.newHttpClient()

            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://127.0.0.1:${mockServer.listeningPort}/api/test"))
                    .GET()
                    .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.body()
        }
    }
}
