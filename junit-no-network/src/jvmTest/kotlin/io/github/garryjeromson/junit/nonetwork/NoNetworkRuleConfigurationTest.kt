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
        @AllowNetworkRequests
        fun shouldAllowNetworkWithAllowNetworkAnnotation() {
            try {
                Socket("example.com", 80)
                // May fail with connection error, but should not be blocked
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("@AllowNetworkRequests should prevent blocking", e)
            } catch (e: Exception) {
                // Other network errors are fine
            }
        }

        @Test
        @AllowRequestsToHosts(hosts = ["localhost"])
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
     * Test 2: @BlockNetworkRequests annotation with JUnit 4
     */
    @BlockNetworkRequests
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
        @AllowNetworkRequests
        fun shouldAllowNetworkWithAllowNetworkOptOut() {
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("@AllowNetworkRequests should override @BlockNetworkRequests", e)
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
            // Existing behavior - no blocking without @BlockNetworkRequests
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block without annotation", e)
            } catch (e: Exception) {
                // Other errors are fine
            }
        }

        @Test
        @BlockNetworkRequests
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
     * Test 4: Class-level @AllowNetworkRequests
     */
    @AllowNetworkRequests
    @BlockNetworkRequests
    class ClassLevelAllowNetworkTest {
        @get:Rule
        val noNetworkRule = NoNetworkRule()

        @Test
        fun shouldAllowNetworkWithClassLevelAllowNetwork() {
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Class-level @AllowNetworkRequests should prevent blocking", e)
            } catch (e: Exception) {
                // Other errors are fine
            }
        }
    }

    /**
     * Test 5: Constructor parameter applyToAllTests = false
     */
    class ConstructorParameterFalseTest {
        @get:Rule
        val noNetworkRule = NoNetworkRule(applyToAllTests = false)

        @Test
        fun shouldNotBlockWhenApplyToAllTestsIsFalse() {
            // Should not block without annotation when explicitly set to false
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block when applyToAllTests = false", e)
            } catch (e: Exception) {
                // Other errors are fine
            }
        }

        @Test
        @BlockNetworkRequests
        fun shouldStillBlockWithAnnotationWhenApplyToAllTestsIsFalse() {
            // Annotation should still work
            try {
                Socket("example.com", 80)
                throw AssertionError("Should have blocked with @BlockNetworkRequests")
            } catch (e: NetworkRequestAttemptedException) {
                // Expected
            }
        }
    }

    /**
     * Test 6: System property junit.nonetwork.applyToAllTests
     */
    class SystemPropertyTest {
        @get:Rule
        val noNetworkRule = NoNetworkRule()

        companion object {
            @org.junit.BeforeClass
            @JvmStatic
            fun setSystemProperty() {
                System.setProperty("junit.nonetwork.applyToAllTests", "true")
            }

            @org.junit.AfterClass
            @JvmStatic
            fun clearSystemProperty() {
                System.clearProperty("junit.nonetwork.applyToAllTests")
            }
        }

        @Test
        fun shouldBlockWhenSystemPropertyIsTrue() {
            try {
                Socket("example.com", 80)
                throw AssertionError("Should have blocked when system property is true")
            } catch (e: NetworkRequestAttemptedException) {
                // Expected
            }
        }
    }
}
