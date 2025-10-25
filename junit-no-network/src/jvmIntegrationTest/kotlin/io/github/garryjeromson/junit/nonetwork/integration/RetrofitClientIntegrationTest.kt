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
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

/**
 * Integration tests verifying that Retrofit HTTP client is properly blocked.
 * Retrofit is the most popular REST client for Android and JVM Kotlin projects.
 */
@ExtendWith(NoNetworkExtension::class)
class RetrofitClientIntegrationTest {
    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeAll
        fun startMockServer() {
            mockServer = MockHttpServer(MockHttpServer.DEFAULT_PORT)
            mockServer.start()
            Thread.sleep(100)
        }

        @JvmStatic
        @AfterAll
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
    }

    @Test
    @BlockNetworkRequests
    fun `should block Retrofit calls to external host`() {
        assertNetworkBlocked("Retrofit should be blocked") {
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
    fun `should allow Retrofit calls to localhost`() {
        assertNetworkNotBlocked("Retrofit to localhost should work") {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl("http://localhost:${MockHttpServer.DEFAULT_PORT}/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

            val service = retrofit.create(TestApiService::class.java)
            val response = service.getTest().execute()
            response.body() // Access response to ensure call completed
        }
    }

    @Test
    @BlockNetworkRequests
    fun `should block Retrofit calls to HTTPS endpoints`() {
        assertNetworkBlocked("Retrofit HTTPS should be blocked") {
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
    @AllowRequestsToHosts(hosts = ["*"])
    fun `should allow Retrofit when wildcard is configured`() {
        assertNetworkNotBlocked("Retrofit should work with wildcard") {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl("http://localhost:${MockHttpServer.DEFAULT_PORT}/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

            val service = retrofit.create(TestApiService::class.java)
            val response = service.getTest().execute()
            response.body()
        }
    }
}
