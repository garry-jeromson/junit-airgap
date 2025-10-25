package io.github.garryjeromson.junit.nonetwork

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit 4 Rule that blocks network requests during tests annotated with @BlockNetworkRequests.
 *
 * Usage:
 * ```
 * class MyTest {
 *     @get:Rule
 *     val noNetworkRule = NoNetworkRule()
 *
 *     @Test
 *     @BlockNetworkRequests
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
 *     @AllowNetworkRequests
 *     fun testWithNetwork() {
 *         // Network is allowed (opt-out)
 *     }
 * }
 * ```
 *
 * **Option 2: @BlockNetworkRequests annotation**
 * ```
 * @BlockNetworkRequests
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
 * The rule respects @AllowRequestsToHosts and @BlockRequestsToHosts annotations for fine-grained control.
 *
 * @param applyToAllTests If true, network blocking is applied to all tests by default.
 *                       When null, the value is determined by system property or @BlockNetworkRequests annotation.
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
                // 1. @AllowNetworkRequests (opt-out - highest priority)
                // 2. Constructor parameter (applyToAllTests)
                // 3. System property
                // 4. @BlockNetworkRequests annotation
                // 5. @BlockNetworkRequests annotation
                // 6. Default (no blocking)

                // Check for @AllowNetworkRequests (opt-out with highest priority)
                val hasAllowNetworkRequests =
                    description.getAnnotation(AllowNetworkRequests::class.java) != null ||
                        description.testClass?.getAnnotation(AllowNetworkRequests::class.java) != null

                if (hasAllowNetworkRequests) {
                    // @AllowNetworkRequests takes precedence - don't block
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

        // Priority 3: @BlockNetworkRequests annotation
        val hasBlockNetworkRequestsOnClass = description.testClass?.getAnnotation(BlockNetworkRequests::class.java) != null

        if (hasBlockNetworkRequestsOnClass) {
            return true
        }

        // Priority 4: @BlockNetworkRequests annotation (existing behavior)
        val hasBlockNetworkRequests =
            description.getAnnotation(BlockNetworkRequests::class.java) != null ||
                description.testClass?.getAnnotation(BlockNetworkRequests::class.java) != null

        return hasBlockNetworkRequests
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
