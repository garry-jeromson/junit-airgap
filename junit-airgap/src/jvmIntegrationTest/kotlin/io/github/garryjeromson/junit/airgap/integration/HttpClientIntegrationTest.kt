package io.github.garryjeromson.junit.airgap.integration

import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts
import io.github.garryjeromson.junit.airgap.AirgapExtension
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.airgap.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.airgap.integration.fixtures.assertNetworkNotBlocked
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
@ExtendWith(AirgapExtension::class)
class HttpClientIntegrationTest {
    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeAll
        fun startMockServer() {
            mockServer = MockHttpServer()
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
    fun `blocks HttpURLConnection to external host`() {
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
    fun `allows HttpURLConnection to localhost`() {
        assertNetworkNotBlocked("HttpURLConnection to localhost should work") {
            val url = URL("http://localhost:${mockServer.listeningPort}/api/test")
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
    fun `blocks OkHttp to external host`() {
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
    fun `allows OkHttp to localhost`() {
        assertNetworkNotBlocked("OkHttp to localhost should work") {
            val client =
                OkHttpClient
                    .Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .build()

            val request =
                Request
                    .Builder()
                    .url("http://localhost:${mockServer.listeningPort}/api/test")
                    .build()

            val response = client.newCall(request).execute()
            response.close()
        }
    }

    @Test
    @BlockNetworkRequests
    @Suppress("DEPRECATION")
    fun `blocks Apache HttpClient to external host`() {
        assertNetworkBlocked("Apache HttpClient should be blocked") {
            val httpClient = HttpClients.createDefault()
            val request = HttpGet("http://example.com/api")

            httpClient.execute(request)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1"])
    @Suppress("DEPRECATION")
    fun `allows Apache HttpClient to 127_0_0_1`() {
        assertNetworkNotBlocked("Apache HttpClient to 127.0.0.1 should work") {
            val httpClient = HttpClients.createDefault()
            val request = HttpGet("http://127.0.0.1:${mockServer.listeningPort}/api/test")

            val response = httpClient.execute(request)
            response.close()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks HTTPS connections`() {
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
    fun `allows all HTTP clients when wildcard is configured`() {
        assertNetworkNotBlocked("All clients should work with wildcard") {
            // Test with localhost since we have wildcard
            val url = URL("http://localhost:${mockServer.listeningPort}/api/test")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.connect()
            connection.disconnect()
        }
    }
}
