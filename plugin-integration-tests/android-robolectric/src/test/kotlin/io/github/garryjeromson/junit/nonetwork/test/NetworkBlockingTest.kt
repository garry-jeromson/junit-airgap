package io.github.garryjeromson.junit.nonetwork.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.Socket
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Android unit tests using Robolectric that verify network blocking with JUnit 4.
 * Tests the bytecode enhancement path for @Rule injection with actual Android APIs.
 */
@RunWith(RobolectricTestRunner::class)
class NetworkBlockingTest {
    @Test
    @NoNetworkTest
    fun networkShouldBeBlockedWithNoNetworkTest() {
        // Verify we can access Android framework
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context, "Android context should be available via Robolectric")

        // Network should be blocked - expect exception
        assertFailsWith<NetworkRequestAttemptedException>("Network should be blocked") {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @AllowNetwork
    fun networkShouldBeAllowedWithAllowNetwork() {
        // Verify we can access Android framework
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context, "Android context should be available via Robolectric")

        // Network should be allowed - may throw IOException but not NetworkRequestAttemptedException
        try {
            Socket("example.com", 80).close()
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Network should NOT be blocked with @AllowNetwork", e)
        } catch (e: Exception) {
            // Other exceptions (no internet, DNS failure, etc.) are OK
        }
    }
}
