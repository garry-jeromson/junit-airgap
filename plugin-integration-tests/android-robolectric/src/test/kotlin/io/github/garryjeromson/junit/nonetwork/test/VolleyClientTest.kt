package io.github.garryjeromson.junit.nonetwork.test

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Tests that verify Android Volley HTTP client network blocking works correctly
 * with the plugin auto-configuration on Android/Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class VolleyClientTest {
    private fun makeVolleyRequest() {
        val context: Context = RuntimeEnvironment.getApplication()
        val queue: RequestQueue = Volley.newRequestQueue(context)
        val latch = CountDownLatch(1)
        val errorRef = AtomicReference<Exception>()
        val successRef = AtomicReference<Boolean>(false)

        val request =
            StringRequest(
                Request.Method.GET,
                "https://example.com/",
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

        if (successRef.get()) {
            throw AssertionError("Expected request to fail but it succeeded")
        }

        errorRef.get()?.let { error ->
            throw error
        }
    }

    @Test
    @BlockNetworkRequests
    fun volleyClientIsBlockedWithNoNetworkTest() {
        assertRequestBlocked {
            makeVolleyRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun volleyClientIsAllowedWithAllowNetwork() {
        assertRequestAllowed {
            makeVolleyRequest()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `volley client with spaces in test name is blocked on Android`() {
        assertRequestBlocked {
            makeVolleyRequest()
        }
    }
}
