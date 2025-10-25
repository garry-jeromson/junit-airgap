package io.github.garryjeromson.junit.nonetwork

import io.github.garryjeromson.junit.nonetwork.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkNotBlocked
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android integration tests for HTTP clients using Robolectric.
 * Tests that the extension properly blocks network requests for common Android HTTP clients.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class AndroidHttpClientIntegrationTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeClass
        fun startMockServer() {
            mockServer = MockHttpServer(MockHttpServer.DEFAULT_PORT)
            mockServer.start()
            Thread.sleep(100)
        }

        @JvmStatic
        @AfterClass
        fun stopMockServer() {
            mockServer.stop()
        }
    }

    // ==================== HttpURLConnection Tests ====================

    @Test
    @BlockNetworkRequests
    fun `should block HttpURLConnection to external host`() {
        assertNetworkBlocked("HttpURLConnection should be blocked") {
            val url = URL("http://example.com/api")
            val connection = url.openConnection() as HttpURLConnection
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
            connection.connect()
            connection.responseCode // Verify we can access the response
        }
    }

    @Test
    @BlockNetworkRequests
    fun `should block HTTPS connections`() {
        assertNetworkBlocked("HTTPS should be blocked") {
            val url = URL("https://api.example.com/data")
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
        }
    }

    // ==================== OkHttp Tests ====================

    @Test
    @BlockNetworkRequests
    fun `should block OkHttp to external host`() {
        assertNetworkBlocked("OkHttp should be blocked") {
            val client = OkHttpClient()
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
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `should allow OkHttp to localhost`() {
        assertNetworkNotBlocked("OkHttp to localhost should work") {
            val client = OkHttpClient()
            val request =
                Request
                    .Builder()
                    .url("http://localhost:${MockHttpServer.DEFAULT_PORT}/api/test")
                    .build()
            val response = client.newCall(request).execute()
            response.use {
                // Verify we can read the response
                assert(it.code in 200..299 || it.body != null)
            }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `should block OkHttp HTTPS requests`() {
        assertNetworkBlocked("OkHttp HTTPS should be blocked") {
            val client = OkHttpClient()
            val request =
                Request
                    .Builder()
                    .url("https://api.github.com/users/test")
                    .build()
            client.newCall(request).execute()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `should block OkHttp POST requests`() {
        assertNetworkBlocked("OkHttp POST should be blocked") {
            val client = OkHttpClient()
            val request =
                Request
                    .Builder()
                    .url("http://example.com/api")
                    .post(RequestBody.create(null, """{"test": "data"}"""))
                    .build()
            client.newCall(request).execute()
        }
    }

    // ==================== Configuration Tests ====================

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    fun `should allow all HTTP clients when wildcard is configured`() {
        assertNetworkNotBlocked("Wildcard should allow HttpURLConnection") {
            val url = URL("http://localhost:${MockHttpServer.DEFAULT_PORT}/api/test")
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
        }

        assertNetworkNotBlocked("Wildcard should allow OkHttp") {
            val client = OkHttpClient()
            val request =
                Request
                    .Builder()
                    .url("http://localhost:${MockHttpServer.DEFAULT_PORT}/api/test")
                    .build()
            client.newCall(request).execute().close()
        }
    }

    // ==================== Multiple Client Tests ====================

    @Test
    @BlockNetworkRequests
    fun `should block multiple different HTTP clients`() {
        // Test that all Android HTTP client types are blocked
        assertNetworkBlocked("HttpURLConnection should be blocked") {
            URL("http://example.com").openConnection().connect()
        }

        assertNetworkBlocked("OkHttp should be blocked") {
            val client = OkHttpClient()
            val request = Request.Builder().url("http://example.com").build()
            client.newCall(request).execute()
        }
    }
}
