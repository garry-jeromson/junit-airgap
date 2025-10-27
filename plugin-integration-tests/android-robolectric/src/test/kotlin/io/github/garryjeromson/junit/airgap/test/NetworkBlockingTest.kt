package io.github.garryjeromson.junit.airgap.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestBlocked
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.Socket
import kotlin.test.assertNotNull

/**
 * Android unit tests using Robolectric that verify network blocking with JUnit 4.
 * Tests the bytecode enhancement path for @Rule injection with actual Android APIs.
 */
@RunWith(RobolectricTestRunner::class)
class NetworkBlockingTest {
    @Test
    @BlockNetworkRequests
    fun networkIsBlockedWithNoNetworkTest() {
        // Verify we can access Android framework
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context, "Android context is available via Robolectric")

        assertRequestBlocked {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    @AllowNetworkRequests
    fun networkIsAllowedWithAllowNetwork() {
        // Verify we can access Android framework
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context, "Android context is available via Robolectric")

        assertRequestAllowed {
            Socket("example.com", 80).use { }
        }
    }
}
