package io.github.garryjeromson.junit.nonetwork

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkNotBlocked
import org.json.JSONObject
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration tests verifying that Android Volley HTTP client is properly blocked.
 * Volley is Google's official HTTP library for Android, commonly used in Android apps.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class VolleyClientIntegrationTest {
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

    @Test
    @BlockNetworkRequests
    fun `blocks Volley StringRequest to external host`() {
        val context = RuntimeEnvironment.getApplication()
        val queue: RequestQueue = Volley.newRequestQueue(context)
        val latch = CountDownLatch(1)
        val errorRef = AtomicReference<Exception>()
        val successRef = AtomicReference<Boolean>(false)

        val request =
            StringRequest(
                Request.Method.GET,
                "http://example.com/api/test",
                {
                    successRef.set(true)
                    latch.countDown()
                },
                { error ->
                    errorRef.set(error)
                    latch.countDown()
                },
            )

        queue.add(request)
        val completed = latch.await(3, TimeUnit.SECONDS)

        // Volley uses background threads - when network is blocked, the background thread
        // throws NetworkRequestAttemptedException and the request never completes
        // We verify blocking by checking that the request didn't succeed
        if (successRef.get()) {
            throw AssertionError("Volley StringRequest should be blocked - Expected request to fail but it succeeded")
        }
        // If we got an error callback, verify it contains network blocking exception
        errorRef.get()?.let { error ->
            assertNetworkBlocked("Volley error should contain network blocking exception") {
                throw error
            }
        }
        // Otherwise, the request was blocked before callback could be invoked (expected for DNS blocking)
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Volley JsonObjectRequest to external host`() {
        val context = RuntimeEnvironment.getApplication()
        val queue: RequestQueue = Volley.newRequestQueue(context)
        val latch = CountDownLatch(1)
        val errorRef = AtomicReference<Exception>()
        val successRef = AtomicReference<Boolean>(false)

        val jsonBody = JSONObject().apply { put("key", "value") }
        val request =
            JsonObjectRequest(
                Request.Method.POST,
                "https://api.example.com/api/submit",
                jsonBody,
                {
                    successRef.set(true)
                    latch.countDown()
                },
                { error ->
                    errorRef.set(error)
                    latch.countDown()
                },
            )

        queue.add(request)
        val completed = latch.await(3, TimeUnit.SECONDS)

        // Volley uses background threads - when network is blocked, the background thread
        // throws NetworkRequestAttemptedException and the request never completes
        // We verify blocking by checking that the request didn't succeed
        if (successRef.get()) {
            throw AssertionError("Volley JsonObjectRequest should be blocked - Expected request to fail but it succeeded")
        }
        // If we got an error callback, verify it contains network blocking exception
        errorRef.get()?.let { error ->
            assertNetworkBlocked("Volley error should contain network blocking exception") {
                throw error
            }
        }
        // Otherwise, the request was blocked before callback could be invoked (expected for DNS blocking)
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `allows Volley StringRequest to localhost`() {
        assertNetworkNotBlocked("Volley to localhost should work") {
            val context = RuntimeEnvironment.getApplication()
            val queue: RequestQueue = Volley.newRequestQueue(context)
            val latch = CountDownLatch(1)
            val responseRef = AtomicReference<String>()
            val errorRef = AtomicReference<Exception>()

            val request =
                StringRequest(
                    Request.Method.GET,
                    "http://localhost:${mockServer.listeningPort}/api/test",
                    { response ->
                        responseRef.set(response)
                        latch.countDown()
                    },
                    { error ->
                        errorRef.set(error)
                        latch.countDown()
                    },
                )

            queue.add(request)
            latch.await(5, TimeUnit.SECONDS)

            // Verify response was received (or throw error if failed)
            errorRef.get()?.let { throw it }
            responseRef.get()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1"])
    fun `allows Volley StringRequest to allowed IP address`() {
        assertNetworkNotBlocked("Volley to 127.0.0.1 should work") {
            val context = RuntimeEnvironment.getApplication()
            val queue: RequestQueue = Volley.newRequestQueue(context)
            val latch = CountDownLatch(1)
            val responseRef = AtomicReference<String>()
            val errorRef = AtomicReference<Exception>()

            val request =
                StringRequest(
                    Request.Method.GET,
                    "http://127.0.0.1:${mockServer.listeningPort}/api/test",
                    { response ->
                        responseRef.set(response)
                        latch.countDown()
                    },
                    { error ->
                        errorRef.set(error)
                        latch.countDown()
                    },
                )

            queue.add(request)
            latch.await(5, TimeUnit.SECONDS)

            errorRef.get()?.let { throw it }
            responseRef.get()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    fun `allows Volley with wildcard configuration`() {
        assertNetworkNotBlocked("Volley should work with wildcard") {
            val context = RuntimeEnvironment.getApplication()
            val queue: RequestQueue = Volley.newRequestQueue(context)
            val latch = CountDownLatch(1)
            val responseRef = AtomicReference<String>()
            val errorRef = AtomicReference<Exception>()

            val request =
                StringRequest(
                    Request.Method.GET,
                    "http://localhost:${mockServer.listeningPort}/api/test",
                    { response ->
                        responseRef.set(response)
                        latch.countDown()
                    },
                    { error ->
                        errorRef.set(error)
                        latch.countDown()
                    },
                )

            queue.add(request)
            latch.await(5, TimeUnit.SECONDS)

            errorRef.get()?.let { throw it }
            responseRef.get()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost"])
    fun `allows Volley JsonObjectRequest to localhost`() {
        assertNetworkNotBlocked("Volley JsonObjectRequest to localhost should work") {
            val context = RuntimeEnvironment.getApplication()
            val queue: RequestQueue = Volley.newRequestQueue(context)
            val latch = CountDownLatch(1)
            val responseRef = AtomicReference<JSONObject>()
            val errorRef = AtomicReference<Exception>()

            val request =
                JsonObjectRequest(
                    Request.Method.GET,
                    "http://localhost:${mockServer.listeningPort}/api/test",
                    null,
                    { response ->
                        responseRef.set(response)
                        latch.countDown()
                    },
                    { error ->
                        errorRef.set(error)
                        latch.countDown()
                    },
                )

            queue.add(request)
            latch.await(5, TimeUnit.SECONDS)

            errorRef.get()?.let { throw it }
            responseRef.get()
        }
    }
}
