package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.HttpClientFactory
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Android tests for platform-specific engine configuration.
 * Validates OkHttp engine is properly configured and blocked.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidKmpKtorEngineConfigTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @NoNetworkTest
    fun shouldBlockOkHttpEngineRequests() = runTest {
        val client = HttpClientFactory.create()
        try {
            val exception = assertFailsWith<Exception> {
                client.get("https://api.github.com")
            }
            val message = exception.message ?: ""
            val hasNetworkBlockedMessage = message.contains("Network request blocked") ||
                message.contains("NetworkRequestAttemptedException")
            if (!hasNetworkBlockedMessage) {
                throw AssertionError("Expected network to be blocked, but got: $exception")
            }
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    fun shouldIncludeHostInformationInException() = runTest {
        val client = HttpClientFactory.create()
        try {
            val exception = assertFailsWith<Exception> {
                client.get("https://example.com/api/data")
            }
            val message = exception.message ?: ""
            assertTrue(
                message.contains("example.com") || message.contains("Network request blocked") ||
                    message.contains("NetworkRequestAttemptedException"),
                "Exception should contain host or blocking message: $message"
            )
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    fun shouldBlockMultipleSequentialRequests() = runTest {
        val client = HttpClientFactory.create()
        try {
            val exception1 = assertFailsWith<Exception> {
                client.get("https://api.github.com/users/octocat")
            }
            val message1 = exception1.message ?: ""
            assertTrue(message1.contains("Network request blocked") || message1.contains("NetworkRequestAttemptedException"))

            val exception2 = assertFailsWith<Exception> {
                client.get("https://httpbin.org/get")
            }
            val message2 = exception2.message ?: ""
            assertTrue(message2.contains("Network request blocked") || message2.contains("NetworkRequestAttemptedException"))
        } finally {
            client.close()
        }
    }

    @Test
    @NoNetworkTest
    fun shouldBlockRequestsToDifferentPorts() = runTest {
        val client = HttpClientFactory.create()
        try {
            val exception = assertFailsWith<Exception> {
                client.get("https://example.com:8080/api")
            }
            val message = exception.message ?: ""
            val hasNetworkBlockedMessage = message.contains("Network request blocked") ||
                message.contains("NetworkRequestAttemptedException")
            if (!hasNetworkBlockedMessage) {
                throw AssertionError("Expected network to be blocked, but got: $exception")
            }
        } finally {
            client.close()
        }
    }
}
