package io.github.garryjeromson.junit.airgap

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.net.Socket

/**
 * Edge case tests for AirgapRule to improve branch coverage.
 * Focuses on system property and constructor parameter combinations.
 */
@RunWith(Enclosed::class)
class AirgapRuleEdgeCasesTest {
    /**
     * Test system property junit.airgap.applyToAllTests=true
     */
    class WithSystemPropertyEnabled {
        @get:Rule
        val noNetworkRule = AirgapRule()

        @Before
        fun setup() {
            System.setProperty("junit.airgap.applyToAllTests", "true")
        }

        @After
        fun tearDown() {
            System.clearProperty("junit.airgap.applyToAllTests")
        }

        @Test
        fun shouldBlockNetworkWhenSystemPropertyIsTrue() {
            try {
                Socket("example.com", 80)
                throw AssertionError("Should have blocked network request when system property is true")
            } catch (e: NetworkRequestAttemptedException) {
                // Expected - network should be blocked
            } catch (e: Exception) {
                throw AssertionError("Should throw NetworkRequestAttemptedException", e)
            }
        }

        @Test
        @AllowNetworkRequests
        fun shouldRespectAllowNetworkAnnotationOverSystemProperty() {
            try {
                Socket("example.com", 80)
                // May fail with connection error, but should not be blocked
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("@AllowNetworkRequests should override system property", e)
            } catch (e: Exception) {
                // Other network errors are fine
            }
        }
    }

    /**
     * Test constructor parameter applyToAllTests = false
     */
    class ConstructorParameterFalse {
        @get:Rule
        val noNetworkRule = AirgapRule(applyToAllTests = false)

        @Test
        fun shouldNotBlockNetworkWhenApplyToAllTestsIsFalse() {
            try {
                Socket("example.com", 80)
                // Should not block - may fail with connection error
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block when applyToAllTests=false", e)
            } catch (e: Exception) {
                // Connection error is fine
            }
        }

        @Test
        @BlockNetworkRequests
        fun shouldBlockWhenAnnotationIsPresentEvenIfConstructorIsFalse() {
            // Constructor applyToAllTests=false means "don't apply to all tests"
            // But @BlockNetworkRequests on the test should still work
            try {
                Socket("example.com", 80)
                throw AssertionError("Should have blocked network request with @BlockNetworkRequests")
            } catch (e: NetworkRequestAttemptedException) {
                // Expected
            }
        }
    }

    /**
     * Test that constructor parameter overrides system property
     */
    class ConstructorOverridesSystemProperty {
        @get:Rule
        val noNetworkRule = AirgapRule(applyToAllTests = false)

        @Before
        fun setup() {
            System.setProperty("junit.airgap.applyToAllTests", "true")
        }

        @After
        fun tearDown() {
            System.clearProperty("junit.airgap.applyToAllTests")
        }

        @Test
        fun shouldRespectConstructorOverSystemProperty() {
            // Constructor parameter (false) should override system property (true)
            try {
                Socket("example.com", 80)
                // Should not block
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError(
                    "Constructor applyToAllTests=false should override system property",
                    e,
                )
            } catch (e: Exception) {
                // Connection error is fine
            }
        }
    }
}
