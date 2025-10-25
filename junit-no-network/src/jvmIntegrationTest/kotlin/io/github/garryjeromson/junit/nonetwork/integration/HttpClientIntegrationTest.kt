package io.github.garryjeromson.junit.nonetwork.integration

import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkNotBlocked
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Integration tests verifying that various HTTP client libraries are properly blocked.
 */
@ExtendWith(NoNetworkExtension::class)
class HttpClientIntegrationTest {
    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeAll
        fun startMockServer() {
            mockServer = MockHttpServer(MockHttpServer.DEFAULT_PORT)
            mockServer.start()
            // Give server time to start
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
    fun `should block HttpURLConnection to external host`() {
        assertNetworkBlocked("HttpURLConnection should be blocked") {
            val url = URL("http://example.com/api")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.connect()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `should allow HttpURLConnection to localhost`() {
        assertNetworkNotBlocked("HttpURLConnection to localhost should work") {
            val url = URL("http://localhost:${MockHttpServer.DEFAULT_PORT}/api/test")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.connect()
            val responseCode = connection.responseCode
            connection.disconnect()
            // If we got here, the connection was allowed
        }
    }

    @Test
    @BlockNetworkRequests
    fun `should block OkHttp to external host`() {
        assertNetworkBlocked("OkHttp should be blocked") {
            val client =
                OkHttpClient
                    .Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .build()

            val request =
                Request
                    .Builder()
                    .url("http://example.com/api")
                    .build()

            client.newCall(request).execute()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"]) // Need both since DNS resolves
    fun `should allow OkHttp to localhost`() {
        assertNetworkNotBlocked("OkHttp to localhost should work") {
            val client =
                OkHttpClient
                    .Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .build()

            val request =
                Request
                    .Builder()
                    .url("http://localhost:${MockHttpServer.DEFAULT_PORT}/api/test")
                    .build()

            val response = client.newCall(request).execute()
            response.close()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `should block Apache HttpClient to external host`() {
        assertNetworkBlocked("Apache HttpClient should be blocked") {
            val httpClient = HttpClients.createDefault()
            val request = HttpGet("http://example.com/api")

            httpClient.execute(request)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1"])
    fun `should allow Apache HttpClient to 127_0_0_1`() {
        assertNetworkNotBlocked("Apache HttpClient to 127.0.0.1 should work") {
            val httpClient = HttpClients.createDefault()
            val request = HttpGet("http://127.0.0.1:${MockHttpServer.DEFAULT_PORT}/api/test")

            val response = httpClient.execute(request)
            response.close()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `should block HTTPS connections`() {
        assertNetworkBlocked("HTTPS should be blocked") {
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.connect()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    fun `should allow all HTTP clients when wildcard is configured`() {
        assertNetworkNotBlocked("All clients should work with wildcard") {
            // Test with localhost since we have wildcard
            val url = URL("http://localhost:${MockHttpServer.DEFAULT_PORT}/api/test")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.connect()
            connection.disconnect()
        }
    }
}
