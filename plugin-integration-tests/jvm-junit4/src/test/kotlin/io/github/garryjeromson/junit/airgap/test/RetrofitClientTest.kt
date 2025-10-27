package io.github.garryjeromson.junit.airgap.test

import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestBlocked
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

class RetrofitClientTest {
    interface TestApi {
        @GET("/")
        fun getData(): Call<String>
    }

    private fun makeRetrofitRequest(): String {
        val retrofit =
            Retrofit
                .Builder()
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
        assertRequestBlocked {
            makeRetrofitRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun retrofitIsAllowedWithAllowNetwork() {
        assertRequestAllowed {
            makeRetrofitRequest()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `retrofit with spaces in test name is blocked`() {
        assertRequestBlocked {
            makeRetrofitRequest()
        }
    }
}
