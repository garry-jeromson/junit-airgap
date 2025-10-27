package io.github.garryjeromson.junit.nonetwork

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit 5 Extension that blocks network requests during tests annotated with @BlockNetworkRequests.
 *
 * Usage:
 * ```
 * @ExtendWith(NoNetworkExtension::class)
 * class MyTest {
 *     @Test
 *     @BlockNetworkRequests
 *     fun testSomething() {
 *         // Network requests will be blocked here
 *     }
 * }
 * ```
 *
 * You can also configure the extension to apply network blocking to all tests by default:
 *
 * **Option 1: Constructor parameter**
 * ```
 * class MyTest {
 *     @JvmField
 *     @RegisterExtension
 *     val extension = NoNetworkExtension(applyToAllTests = true)
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
 * **Option 2: Class-level @BlockNetworkRequests annotation**
 * ```
 * @ExtendWith(NoNetworkExtension::class)
 * @BlockNetworkRequests
 * class MyTest {
 *     // All tests have network blocked by default
 * }
 * ```
 *
 * **Option 3: System property**
 * ```
 * -Djunit.nonetwork.applyToAllTests=true
 * ```
 *
 * The extension respects @AllowRequestsToHosts and @BlockRequestsToHosts annotations for fine-grained control.
 *
 * @param applyToAllTests If true, network blocking is applied to all tests by default.
 *                       When null, the value is determined by system property or class-level @BlockNetworkRequests annotation.
 */
class NoNetworkExtension(
    private val applyToAllTests: Boolean? = null,
) : BeforeEachCallback,
    AfterEachCallback {
    companion object {
        private const val BLOCKER_KEY = "junit-no-network-blocker"

        init {
            // Force NetworkBlockerContext to initialize early.
            // This ensures it registers with the JVMTI agent (if loaded) before any tests run.
            // Without this, the agent might intercept network calls before registration happens,
            // causing "NetworkBlockerContext not registered" warnings.
            try {
                // Access the object to trigger its init block (accessing just ::class doesn't work)
                // getConfiguration() is a simple getter that won't cause side effects
                io.github.garryjeromson.junit.nonetwork.bytebuddy.NetworkBlockerContext
                    .getConfiguration()
            } catch (e: Throwable) {
                // Ignore errors - NetworkBlockerContext will initialize when first used
            }
        }
    }

    override fun beforeEach(context: ExtensionContext) {
        // Priority order for determining whether to block network:
        // 1. @AllowNetworkRequests (opt-out - highest priority)
        // 2. Constructor parameter (applyToAllTests)
        // 3. System property
        // 4. Class-level @BlockNetworkRequests annotation
        // 5. Method-level @BlockNetworkRequests annotation
        // 6. Default (no blocking)

        // Check for @AllowNetworkRequests (opt-out with highest priority)
        val hasAllowNetworkRequests =
            context.testMethod
                .map { method ->
                    method.isAnnotationPresent(AllowNetworkRequests::class.java)
                }.orElse(false) ||
                context.testClass
                    .map { clazz ->
                        clazz.isAnnotationPresent(AllowNetworkRequests::class.java)
                    }.orElse(false)

        if (hasAllowNetworkRequests) {
            // @AllowNetworkRequests takes precedence - don't block
            return
        }

        // Determine if network should be blocked based on priority
        val shouldBlock = shouldBlockNetwork(context)

        if (!shouldBlock) {
            // Don't block network
            return
        }

        // Collect configuration from annotations
        val configuration = buildConfiguration(context)

        // Create and install the blocker
        val blocker = NetworkBlocker(configuration)
        blocker.install()

        // Store the blocker in the context so we can uninstall it later
        context
            .getStore(ExtensionContext.Namespace.create(NoNetworkExtension::class.java))
            .put(BLOCKER_KEY, blocker)
    }

    /**
     * Determines if network should be blocked based on priority order.
     */
    private fun shouldBlockNetwork(context: ExtensionContext): Boolean {
        // Priority 1: Constructor parameter
        if (applyToAllTests != null) {
            return applyToAllTests
        }

        // Priority 2: JUnit configuration parameter (from junit-platform.properties)
        val configParam =
            context
                .getConfigurationParameter(ExtensionConfiguration.APPLY_TO_ALL_TESTS_PROPERTY)
                .orElse(null)
        if (configParam != null && configParam.toBoolean()) {
            return true
        }

        // Priority 3: System property
        if (ExtensionConfiguration.isApplyToAllTestsEnabled()) {
            return true
        }

        // Priority 4: @BlockNetworkRequests annotation (class or method level)
        val hasBlockNetworkRequests =
            context.testMethod
                .map { method ->
                    method.isAnnotationPresent(BlockNetworkRequests::class.java)
                }.orElse(false) ||
                context.testClass
                    .map { clazz ->
                        clazz.isAnnotationPresent(BlockNetworkRequests::class.java)
                    }.orElse(false)

        return hasBlockNetworkRequests
    }

    override fun afterEach(context: ExtensionContext) {
        // Retrieve and uninstall the blocker if it was installed
        val blocker =
            context
                .getStore(ExtensionContext.Namespace.create(NoNetworkExtension::class.java))
                .get(BLOCKER_KEY, NetworkBlocker::class.java)

        blocker?.uninstall()
    }

    private fun buildConfiguration(context: ExtensionContext): NetworkConfiguration {
        // Collect annotations from both method and class
        val annotations = mutableListOf<Annotation>()

        // Add method annotations
        context.testMethod.ifPresent { method ->
            annotations.addAll(method.annotations)
        }

        // Add class annotations
        context.testClass.ifPresent { clazz ->
            annotations.addAll(clazz.annotations)
        }

        // Build configuration from annotations
        val annotationConfig = NetworkConfiguration.fromAnnotations(annotations)

        // Read global configuration from system properties
        val globalConfig =
            NetworkConfiguration(
                allowedHosts = ExtensionConfiguration.getAllowedHosts(),
                blockedHosts = ExtensionConfiguration.getBlockedHosts(),
            )

        // Merge configurations - annotation-level takes precedence for blocked hosts,
        // but allowed hosts are combined
        return globalConfig.merge(annotationConfig)
    }
}
