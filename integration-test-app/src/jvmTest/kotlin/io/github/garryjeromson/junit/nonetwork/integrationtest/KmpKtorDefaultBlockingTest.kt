package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkByDefault
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.DefaultApiClient
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertFailsWith

/**
 * Tests default blocking scenarios with KMP shared client.
 * Validates applyToAllTests and @NoNetworkByDefault work correctly with expect/actual pattern.
 */
class KmpKtorDefaultBlockingTest {

    /**
     * Test constructor parameter (applyToAllTests = true) with KMP client.
     */
    @Nested
    inner class ConstructorParameterTest {
        @JvmField
        @RegisterExtension
        val extension = NoNetworkExtension(applyToAllTests = true)

        @Test
        fun `should block shared client by default with applyToAllTests`() = runTest {
            val client = DefaultApiClient()
            try {
                assertFailsWith<NetworkRequestAttemptedException> {
                    client.fetchUser(1)
                }
            } finally {
                client.close()
            }
        }

        @Test
        @AllowNetwork
        fun `should allow with AllowNetwork annotation`() = runTest {
            val client = DefaultApiClient()
            try {
                // @AllowNetwork should override default blocking
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
     * Test @NoNetworkByDefault annotation with KMP client.
     */
    @Nested
    @NoNetworkByDefault
    @ExtendWith(NoNetworkExtension::class)
    inner class NoNetworkByDefaultAnnotationTest {

        @Test
        fun `should block shared client with NoNetworkByDefault`() = runTest {
            val repository = UserRepository()
            try {
                assertFailsWith<NetworkRequestAttemptedException> {
                    repository.getUser(1)
                }
            } finally {
                repository.close()
            }
        }

        @Test
        @AllowNetwork
        fun `should allow with AllowNetwork even when NoNetworkByDefault set`() = runTest {
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
}
