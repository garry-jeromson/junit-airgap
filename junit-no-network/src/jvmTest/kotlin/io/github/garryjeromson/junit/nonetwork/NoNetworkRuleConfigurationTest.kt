package io.github.garryjeromson.junit.nonetwork

import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.net.Socket

/**
 * Tests for JUnit 4 NoNetworkRule with configuration options.
 * Tests constructor parameter and system property support.
 */
@RunWith(Enclosed::class)
class NoNetworkRuleConfigurationTest {
    /**
     * Test 1: Constructor parameter applyToAllTests = true
     */
    class ConstructorParameterTest {
        @get:Rule
        val noNetworkRule = NoNetworkRule(applyToAllTests = true)

        @Test
        fun shouldBlockNetworkWhenApplyToAllTestsIsTrue() {
            try {
                Socket("example.com", 80)
                throw AssertionError("Should have blocked network request")
            } catch (e: NetworkRequestAttemptedException) {
                // Expected - network should be blocked
            } catch (e: Exception) {
                // Connection may fail for other reasons, that's fine as long as it's not blocked
                throw AssertionError("Should throw NetworkRequestAttemptedException", e)
            }
        }

        @Test
        @AllowNetwork
        fun shouldAllowNetworkWithAllowNetworkAnnotation() {
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
        fun shouldRespectAllowedHostsConfiguration() {
            // Configuration should still work
            try {
                Socket("example.com", 80)
                throw AssertionError("Should have blocked example.com")
            } catch (e: NetworkRequestAttemptedException) {
                // Expected
            }
        }
    }

    /**
     * Test 2: @NoNetworkByDefault annotation with JUnit 4
     */
    @NoNetworkByDefault
    class NoNetworkByDefaultAnnotationTest {
        @get:Rule
        val noNetworkRule = NoNetworkRule()

        @Test
        fun shouldBlockNetworkWithNoNetworkByDefaultAnnotation() {
            try {
                Socket("example.com", 80)
                throw AssertionError("Should have blocked network request")
            } catch (e: NetworkRequestAttemptedException) {
                // Expected
            }
        }

        @Test
        @AllowNetwork
        fun shouldAllowNetworkWithAllowNetworkOptOut() {
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("@AllowNetwork should override @NoNetworkByDefault", e)
            } catch (e: Exception) {
                // Other errors are fine
            }
        }
    }

    /**
     * Test 3: Backward compatibility
     */
    class BackwardCompatibilityTest {
        @get:Rule
        val noNetworkRule = NoNetworkRule()

        @Test
        fun shouldNotBlockWithoutAnnotationByDefault() {
            // Existing behavior - no blocking without @NoNetworkTest
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block without annotation", e)
            } catch (e: Exception) {
                // Other errors are fine
            }
        }

        @Test
        @NoNetworkTest
        fun shouldBlockWithNoNetworkTestAnnotation() {
            // Existing behavior should still work
            try {
                Socket("example.com", 80)
                throw AssertionError("Should have blocked network request")
            } catch (e: NetworkRequestAttemptedException) {
                // Expected
            }
        }
    }

    /**
     * Test 4: Class-level @AllowNetwork
     */
    @AllowNetwork
    @NoNetworkByDefault
    class ClassLevelAllowNetworkTest {
        @get:Rule
        val noNetworkRule = NoNetworkRule()

        @Test
        fun shouldAllowNetworkWithClassLevelAllowNetwork() {
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Class-level @AllowNetwork should prevent blocking", e)
            } catch (e: Exception) {
                // Other errors are fine
            }
        }
    }
}
