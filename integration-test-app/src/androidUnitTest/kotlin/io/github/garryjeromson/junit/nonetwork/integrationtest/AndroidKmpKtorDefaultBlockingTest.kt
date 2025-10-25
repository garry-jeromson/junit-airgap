package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkByDefault
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.DefaultApiClient
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

/**
 * Android tests for default blocking with KMP shared client.
 * Validates applyToAllTests and @NoNetworkByDefault work with OkHttp engine.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidKmpKtorDefaultBlockingConstructorTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule(applyToAllTests = true)

    @Test
    fun shouldBlockSharedClientByDefaultWithApplyToAllTests() = runTest {
        val client = DefaultApiClient()
        try {
            val exception = assertFailsWith<Exception> {
                client.fetchUser(1)
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
    @AllowNetwork
    fun shouldAllowWithAllowNetworkAnnotation() = runTest {
        val client = DefaultApiClient()
        try {
            client.fetchUser(1)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("@AllowNetwork should override default blocking", e)
        } catch (e: Exception) {
            // Other network errors are fine
        } finally {
            client.close()
        }
    }
}

/**
 * Test @NoNetworkByDefault annotation with Android KMP client.
 */
@RunWith(RobolectricTestRunner::class)
@NoNetworkByDefault
class AndroidKmpKtorDefaultBlockingAnnotationTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    fun shouldBlockSharedClientWithNoNetworkByDefault() = runTest {
        val repository = UserRepository()
        try {
            val exception = assertFailsWith<Exception> {
                repository.getUser(1)
            }
            val message = exception.message ?: ""
            val hasNetworkBlockedMessage = message.contains("Network request blocked") ||
                message.contains("NetworkRequestAttemptedException")
            if (!hasNetworkBlockedMessage) {
                throw AssertionError("Expected network to be blocked, but got: $exception")
            }
        } finally {
            repository.close()
        }
    }

    @Test
    @AllowNetwork
    fun shouldAllowWithAllowNetworkEvenWhenNoNetworkByDefaultSet() = runTest {
        val repository = UserRepository()
        try {
            repository.getUser(1)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("@AllowNetwork should override @NoNetworkByDefault", e)
        } catch (e: Exception) {
            // Other network errors are fine
        } finally {
            repository.close()
        }
    }
}
