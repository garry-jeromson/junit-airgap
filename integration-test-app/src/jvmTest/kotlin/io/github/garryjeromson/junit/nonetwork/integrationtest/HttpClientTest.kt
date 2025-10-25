package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.java.Java
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URL
import kotlin.test.assertFailsWith

/**
 * Tests blocking network requests from various HTTP client libraries.
 */
@ExtendWith(NoNetworkExtension::class)
class HttpClientTest {

    @Test
    @NoNetworkTest
    fun `should block OkHttp requests`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://example.com")
                .build()
            client.newCall(request).execute()
        }
    }

    @Test
    @NoNetworkTest
    fun `should block HttpURLConnection requests`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            val connection = URL("https://example.com").openConnection()
            connection.connect()
        }
    }

    @Test
    @NoNetworkTest
    fun `should block Apache HttpClient requests`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            val client = HttpClients.createDefault()
            val request = HttpGet("https://example.com")
            client.execute(request)
        }
    }

    @Test
    @NoNetworkTest
    fun `should block Ktor CIO client requests`() = runTest {
        assertFailsWith<NetworkRequestAttemptedException> {
            val client = HttpClient(CIO)
            try {
                client.get("https://example.com")
            } finally {
                client.close()
            }
        }
    }

    @Test
    @NoNetworkTest
    fun `should block Ktor OkHttp client requests`() = runTest {
        // Ktor OkHttp wraps NetworkRequestAttemptedException in IOException
        val exception = assertFailsWith<Exception> {
            val client = HttpClient(OkHttp)
            try {
                client.get("https://example.com")
            } finally {
                client.close()
            }
        }
        // Verify the cause or message contains our exception
        val message = exception.message ?: ""
        val hasNetworkBlockedMessage = message.contains("Network request blocked") ||
            message.contains("NetworkRequestAttemptedException")
        if (!hasNetworkBlockedMessage) {
            throw AssertionError("Expected network to be blocked, but got: $exception")
        }
    }

    @Test
    @NoNetworkTest
    fun `should block Ktor Java client requests`() = runTest {
        assertFailsWith<NetworkRequestAttemptedException> {
            val client = HttpClient(Java)
            try {
                client.get("https://example.com")
            } finally {
                client.close()
            }
        }
    }
}
