package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import kotlin.test.Test

/**
 * Tests that verify Ktor HTTP client network blocking works correctly
 * with both @BlockNetworkRequests and @AllowNetworkRequests annotations in KMP context.
 *
 * Uses kotlin.test annotations (not JUnit).
 * Uses expect/actual pattern for platform-specific HTTP client implementations:
 * - JVM: CIO engine
 * - Android: OkHttp engine (compatible with Robolectric)
 */
class KtorClientTest {
    @Test
    @BlockNetworkRequests
    fun ktorClientIsBlockedWithNoNetworkTest() {
        assertRequestBlocked {
            makeKtorRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun ktorClientIsAllowedWithAllowNetwork() {
        assertRequestAllowed {
            makeKtorRequest()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `ktor client with spaces in test name is blocked in KMP`() {
        assertRequestBlocked {
            makeKtorRequest()
        }
    }
}

/**
 * Platform-specific function to make an HTTP request using Ktor.
 * Actual implementations use different engines per platform.
 */
expect fun makeKtorRequest(): String
