package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.jupiter.api.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import kotlin.test.assertFailsWith
import kotlin.test.fail

/**
 * Tests that verify Retrofit HTTP client network blocking works correctly
 * with the plugin auto-configuration (JUnit 5).
 */
class RetrofitClientTest {

    interface TestApi {
        @GET("/")
        fun getData(): Call<String>
    }

    private fun makeRetrofitRequest(): String {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://example.com/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        val api = retrofit.create(TestApi::class.java)
        val response = api.getData().execute()
        return response.body() ?: ""
    }

    @Test
    @BlockNetworkRequests
    fun retrofitIsBlockedWithNoNetworkTest() {
        assertFailsWith<NetworkRequestAttemptedException> {
            makeRetrofitRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun retrofitIsAllowedWithAllowNetwork() {
        try {
            makeRetrofitRequest()
        } catch (e: NetworkRequestAttemptedException) {
            fail("Network is allowed with @AllowNetworkRequests, but was blocked: ${e.message}")
        } catch (e: Exception) {
            // Other exceptions (like actual network errors) are fine
        }
    }

    @Test
    @BlockNetworkRequests
    fun `retrofit with spaces in test name is blocked`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            makeRetrofitRequest()
        }
    }
}
