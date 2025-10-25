package io.github.garryjeromson.junit.nonetwork

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit 4 Rule that blocks network requests during tests annotated with @NoNetworkTest.
 *
 * Usage:
 * ```
 * class MyTest {
 *     @get:Rule
 *     val noNetworkRule = NoNetworkRule()
 *
 *     @Test
 *     @NoNetworkTest
 *     fun testSomething() {
 *         // Network requests will be blocked here
 *     }
 * }
 * ```
 *
 * You can also configure the rule to apply network blocking to all tests by default:
 *
 * **Option 1: Constructor parameter**
 * ```
 * class MyTest {
 *     @get:Rule
 *     val noNetworkRule = NoNetworkRule(applyToAllTests = true)
 *
 *     @Test
 *     fun testSomething() {
 *         // Network is blocked by default
 *     }
 *
 *     @Test
 *     @AllowNetwork
 *     fun testWithNetwork() {
 *         // Network is allowed (opt-out)
 *     }
 * }
 * ```
 *
 * **Option 2: @NoNetworkByDefault annotation**
 * ```
 * @NoNetworkByDefault
 * class MyTest {
 *     @get:Rule
 *     val noNetworkRule = NoNetworkRule()
 *
 *     // All tests have network blocked by default
 * }
 * ```
 *
 * **Option 3: System property**
 * ```
 * -Djunit.nonetwork.applyToAllTests=true
 * ```
 *
 * The rule respects @AllowedHosts and @BlockedHosts annotations for fine-grained control.
 *
 * @param applyToAllTests If true, network blocking is applied to all tests by default.
 *                       When null, the value is determined by system property or @NoNetworkByDefault annotation.
 */
class NoNetworkRule(
    private val applyToAllTests: Boolean? = null,
) : TestRule {
    override fun apply(
        base: Statement,
        description: Description,
    ): Statement {
        return object : Statement() {
            override fun evaluate() {
                // Priority order for determining whether to block network:
                // 1. @AllowNetwork (opt-out - highest priority)
                // 2. Constructor parameter (applyToAllTests)
                // 3. System property
                // 4. @NoNetworkByDefault annotation
                // 5. @NoNetworkTest annotation
                // 6. Default (no blocking)

                // Check for @AllowNetwork (opt-out with highest priority)
                val hasAllowNetwork =
                    description.getAnnotation(AllowNetwork::class.java) != null ||
                        description.testClass?.getAnnotation(AllowNetwork::class.java) != null

                if (hasAllowNetwork) {
                    // @AllowNetwork takes precedence - don't block
                    base.evaluate()
                    return
                }

                // Determine if network should be blocked
                val shouldBlock = shouldBlockNetwork(description)

                if (!shouldBlock) {
                    // Don't block network
                    base.evaluate()
                    return
                }

                // Collect configuration from annotations
                val configuration = buildConfiguration(description)

                // Create and install the blocker
                val blocker = NetworkBlocker(configuration)
                blocker.install()

                try {
                    // Run the test
                    base.evaluate()
                } finally {
                    // Always uninstall the blocker
                    blocker.uninstall()
                }
            }
        }
    }

    /**
     * Determines if network should be blocked based on priority order.
     */
    private fun shouldBlockNetwork(description: Description): Boolean {
        // Priority 1: Constructor parameter
        if (applyToAllTests != null) {
            return applyToAllTests
        }

        // Priority 2: System property
        if (ExtensionConfiguration.isApplyToAllTestsEnabled()) {
            return true
        }

        // Priority 3: @NoNetworkByDefault annotation
        val hasNoNetworkByDefault = description.testClass?.getAnnotation(NoNetworkByDefault::class.java) != null

        if (hasNoNetworkByDefault) {
            return true
        }

        // Priority 4: @NoNetworkTest annotation (existing behavior)
        val hasNoNetworkTest =
            description.getAnnotation(NoNetworkTest::class.java) != null ||
                description.testClass?.getAnnotation(NoNetworkTest::class.java) != null

        return hasNoNetworkTest
    }

    private fun buildConfiguration(description: Description): NetworkConfiguration {
        // Collect all annotations from both the test method and class
        val methodAnnotations = description.annotations.toList()
        val classAnnotations = description.testClass?.annotations?.toList() ?: emptyList()

        // Combine annotations from class and method (method takes precedence)
        val allAnnotations = classAnnotations + methodAnnotations

        // Build configuration from annotations
        return NetworkConfiguration.fromAnnotations(allAnnotations)
    }
}
