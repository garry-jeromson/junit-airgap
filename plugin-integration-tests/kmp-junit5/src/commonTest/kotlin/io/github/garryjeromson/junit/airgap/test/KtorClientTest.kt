package io.github.garryjeromson.junit.airgap.test

import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestBlocked
import org.junit.jupiter.api.Test

/**
 * Tests that verify Ktor HTTP client network blocking works correctly
 * with both @BlockNetworkRequests and @AllowNetworkRequests annotations in KMP context.
 *
 * Uses expect/actual pattern for platform-specific HTTP client implementations:
 * - JVM: CIO engine
 * - Android: OkHttp engine (compatible with Robolectric)
 */
class KtorClientTest {
    @Test
    @BlockNetworkRequests
    fun `ktor client is blocked with NoNetworkTest`() {
        assertRequestBlocked {
            makeKtorRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun `ktor client is allowed with AllowNetwork`() {
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
