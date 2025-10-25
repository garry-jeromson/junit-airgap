package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL
import kotlin.test.assertFailsWith

/**
 * Android tests for blocking HTTP client requests.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidHttpClientTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @NoNetworkTest
    fun shouldBlockOkHttpRequests() {
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
    fun shouldBlockHttpURLConnectionRequests() {
        assertFailsWith<NetworkRequestAttemptedException> {
            val connection = URL("https://example.com").openConnection()
            connection.connect()
        }
    }
}
