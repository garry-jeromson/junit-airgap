package io.github.garryjeromson.junit.nonetwork

import io.github.garryjeromson.junit.nonetwork.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkNotBlocked
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Android integration tests for Retrofit HTTP client using Robolectric.
 * Retrofit is the most popular REST client for Android applications.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RetrofitClientIntegrationTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeClass
        fun startMockServer() {
            mockServer = MockHttpServer()
            mockServer.start()
            Thread.sleep(100)
        }

        @JvmStatic
        @AfterClass
        fun stopMockServer() {
            mockServer.stop()
        }
    }

    // Retrofit service interface for testing
    interface TestApiService {
        @GET("/api/test")
        fun getTest(): Call<String>

        @GET("/api/data")
        fun getData(): Call<String>

        @POST("/api/submit")
        fun postData(): Call<String>
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Retrofit calls to external host on Android`() {
        assertNetworkBlocked("Retrofit should be blocked on Android") {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl("http://example.com/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

            val service = retrofit.create(TestApiService::class.java)
            service.getTest().execute()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `allows Retrofit calls to localhost on Android`() {
        assertNetworkNotBlocked("Retrofit to localhost should work on Android") {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl("http://localhost:${mockServer.listeningPort}/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

            val service = retrofit.create(TestApiService::class.java)
            val response = service.getTest().execute()
            response.body() // Access response to ensure call completed
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Retrofit HTTPS calls on Android`() {
        assertNetworkBlocked("Retrofit HTTPS should be blocked on Android") {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl("https://api.example.com/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

            val service = retrofit.create(TestApiService::class.java)
            service.getData().execute()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Retrofit POST requests on Android`() {
        assertNetworkBlocked("Retrofit POST should be blocked on Android") {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl("http://example.com/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

            val service = retrofit.create(TestApiService::class.java)
            service.postData().execute()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    fun `allows Retrofit when wildcard is configured on Android`() {
        assertNetworkNotBlocked("Retrofit should work with wildcard on Android") {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl("http://localhost:${mockServer.listeningPort}/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

            val service = retrofit.create(TestApiService::class.java)
            val response = service.getTest().execute()
            response.body()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1"])
    fun `allows Retrofit to allowed IP address on Android`() {
        assertNetworkNotBlocked("Retrofit to 127.0.0.1 should work on Android") {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl("http://127.0.0.1:${mockServer.listeningPort}/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

            val service = retrofit.create(TestApiService::class.java)
            val response = service.getTest().execute()
            response.body()
        }
    }
}
