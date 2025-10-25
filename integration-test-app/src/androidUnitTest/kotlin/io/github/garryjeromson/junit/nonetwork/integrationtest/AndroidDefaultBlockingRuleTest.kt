package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Android tests for default blocking via NoNetworkRule (JUnit 4).
 */
@RunWith(RobolectricTestRunner::class)
class AndroidDefaultBlockingRuleTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule(applyToAllTests = true)

    @Test
    fun shouldBlockNetworkByDefaultWithApplyToAllTestsTrue() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @AllowNetwork
    fun shouldAllowNetworkWithAllowNetworkAnnotation() {
        try {
            Socket("example.com", 80)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("@AllowNetwork should override default blocking", e)
        } catch (e: Exception) {
            // Other network errors are fine
        }
    }
}
