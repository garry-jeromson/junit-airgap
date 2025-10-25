package io.github.garryjeromson.junit.nonetwork

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Tests for default network blocking configuration.
 * These tests verify the new configuration options:
 * - Constructor parameter (applyToAllTests)
 * - @NoNetworkByDefault annotation
 * - @AllowNetwork opt-out
 * - System property
 */
class DefaultBlockingConfigurationTest {
    /**
     * Test 1: Constructor parameter applyToAllTests = true
     * When applyToAllTests is true, all tests should block network by default
     */
    @Nested
    inner class ConstructorParameterTest {
        @JvmField
        @RegisterExtension
        val extension = NoNetworkExtension(applyToAllTests = true)

        @Test
        fun `should block network when applyToAllTests is true without annotation`() {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("example.com", 80)
            }
        }

        @Test
        @AllowNetwork
        fun `should allow network when AllowNetwork annotation is present`() {
            // This should NOT throw NetworkRequestAttemptedException
            try {
                Socket("example.com", 80)
                // May fail with connection error, but should not be blocked
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("@AllowNetwork should prevent blocking", e)
            } catch (e: Exception) {
                // Other network errors are fine
            }
        }

        @Test
        @AllowedHosts(hosts = ["localhost"])
        fun `should respect AllowedHosts configuration when applyToAllTests is true`() {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("example.com", 80)
            }
        }
    }

    /**
     * Test 2: @NoNetworkByDefault annotation
     * When applied to a class, all tests should block network
     */
    @Nested
    @NoNetworkByDefault
    inner class NoNetworkByDefaultAnnotationTest {
        @JvmField
        @RegisterExtension
        val extension = NoNetworkExtension()

        @Test
        fun `should block network when NoNetworkByDefault is on class`() {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("example.com", 80)
            }
        }

        @Test
        @AllowNetwork
        fun `should allow network with AllowNetwork even when NoNetworkByDefault is set`() {
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("@AllowNetwork should override @NoNetworkByDefault", e)
            } catch (e: Exception) {
                // Other network errors are fine
            }
        }
    }

    /**
     * Test 3: Priority/Precedence Rules
     * @AllowNetwork should take precedence over everything
     */
    @Nested
    inner class PriorityTest {
        @JvmField
        @RegisterExtension
        val extension = NoNetworkExtension(applyToAllTests = true)

        @Test
        @NoNetworkTest
        @AllowNetwork
        fun `AllowNetwork should override NoNetworkTest`() {
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("@AllowNetwork should have highest priority", e)
            } catch (e: Exception) {
                // Other network errors are fine
            }
        }
    }

    /**
     * Test 4: Backward Compatibility
     * Existing behavior should still work with default constructor
     */
    @Nested
    inner class BackwardCompatibilityTest {
        @JvmField
        @RegisterExtension
        val extension = NoNetworkExtension()

        @Test
        fun `should NOT block network without any annotation when applyToAllTests is false`() {
            // This should work as before - no blocking without @NoNetworkTest
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block without annotation when applyToAllTests=false", e)
            } catch (e: Exception) {
                // Other network errors are fine
            }
        }

        @Test
        @NoNetworkTest
        fun `should block network with NoNetworkTest annotation (existing behavior)`() {
            // Existing behavior should still work
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("example.com", 80)
            }
        }
    }

    /**
     * Test 5: Class-level @AllowNetwork
     * When applied at class level, should allow all tests
     */
    @Nested
    @AllowNetwork
    @NoNetworkByDefault
    inner class ClassLevelAllowNetworkTest {
        @JvmField
        @RegisterExtension
        val extension = NoNetworkExtension()

        @Test
        fun `should allow network when AllowNetwork is at class level`() {
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Class-level @AllowNetwork should prevent blocking", e)
            } catch (e: Exception) {
                // Other network errors are fine
            }
        }
    }
}
