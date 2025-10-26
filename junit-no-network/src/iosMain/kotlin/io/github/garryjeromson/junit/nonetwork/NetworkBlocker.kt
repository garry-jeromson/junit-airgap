package io.github.garryjeromson.junit.nonetwork

/**
 * iOS implementation of NetworkBlocker.
 *
 * Note: iOS support for network blocking is currently limited due to platform constraints.
 * Unlike JVM/Android which use JVMTI native agent for socket interception, iOS requires different approaches:
 * - NSURLProtocol for URLSession (requires Objective-C bridge)
 * - No low-level socket interception available
 *
 * Current implementation provides the API structure but does not actively block requests.
 * This is a limitation of the iOS platform and Kotlin/Native interop with NSURLProtocol.
 *
 * For full iOS support, consider:
 * 1. Using mocking frameworks (MockURLProtocol)
 * 2. Dependency injection for network layers
 * 3. Custom Objective-C bridge code for NSURLProtocol
 */
actual class NetworkBlocker actual constructor(
    private val configuration: NetworkConfiguration,
) {
    private var isInstalled: Boolean = false

    companion object {
        // Shared configuration storage
        private var sharedConfiguration: NetworkConfiguration? = null

        internal fun getSharedConfiguration(): NetworkConfiguration? = sharedConfiguration

        internal fun setSharedConfiguration(config: NetworkConfiguration?) {
            sharedConfiguration = config
        }
    }

    /**
     * Installs the network blocker.
     *
     * Note: Current iOS implementation stores configuration but does not actively block.
     * See class documentation for limitations and alternatives.
     */
    actual fun install() {
        if (isInstalled) {
            return
        }

        // Store configuration
        setSharedConfiguration(configuration)
        isInstalled = true

        // Log warning about limited iOS support
        println("Warning: iOS NetworkBlocker installed but network blocking is limited on this platform")
        println("Consider using mock network layers or dependency injection for iOS testing")
    }

    /**
     * Uninstalls the network blocker.
     */
    actual fun uninstall() {
        if (!isInstalled) {
            return
        }

        setSharedConfiguration(null)
        isInstalled = false
    }
}
