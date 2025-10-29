package io.github.garryjeromson.junit.airgap

/**
 * iOS implementation of NetworkBlocker.
 *
 * This implementation uses NSURLProtocol to intercept URLSession requests (which Ktor Darwin engine uses).
 * The Objective-C bridge code (AirgapURLProtocol) registers with NSURLProtocol and calls back into
 * Kotlin to check if each request should be blocked based on the NetworkConfiguration.
 *
 * Note: This only intercepts URLSession-based requests. Raw BSD socket calls are not intercepted.
 */
actual class NetworkBlocker actual constructor(
    private val configuration: NetworkConfiguration,
) {
    private var isInstalled: Boolean = false

    companion object {
        // Shared configuration storage accessed by Objective-C bridge
        private var sharedConfiguration: NetworkConfiguration? = null

        // Protocol registration state - register once globally
        private var isProtocolRegistered: Boolean = false

        internal fun getSharedConfiguration(): NetworkConfiguration? = sharedConfiguration

        internal fun setSharedConfiguration(config: NetworkConfiguration?) {
            sharedConfiguration = config
        }

        /**
         * Ensure URLProtocol is registered globally.
         * This is idempotent - safe to call multiple times.
         */
        private fun ensureProtocolRegistered() {
            if (!isProtocolRegistered) {
                DebugLogger.log("Registering AirgapURLProtocol for network interception")
                if (registerURLProtocol()) {
                    isProtocolRegistered = true
                    DebugLogger.log("AirgapURLProtocol registered successfully")
                } else {
                    DebugLogger.log("Failed to register AirgapURLProtocol")
                }
            }
        }
    }

    /**
     * Installs the network blocker.
     *
     * This registers the NSURLProtocol (if not already registered) and sets the configuration
     * that will be used to determine which hosts to block.
     */
    actual fun install() {
        if (isInstalled) {
            return
        }

        DebugLogger.log("Installing iOS NetworkBlocker with configuration: $configuration")

        // Ensure URLProtocol is registered globally (idempotent)
        ensureProtocolRegistered()

        // Store configuration for this test
        setSharedConfiguration(configuration)

        // Update the Objective-C side configuration
        if (!setURLProtocolConfiguration(configuration)) {
            DebugLogger.log("Warning: Failed to set URLProtocol configuration")
        }

        isInstalled = true
        DebugLogger.log("iOS NetworkBlocker installed successfully")
    }

    /**
     * Uninstalls the network blocker.
     *
     * This clears the configuration for the current test. The URLProtocol remains registered
     * globally to avoid re-registration overhead, but without configuration it won't block requests.
     */
    actual fun uninstall() {
        if (!isInstalled) {
            return
        }

        DebugLogger.log("Uninstalling iOS NetworkBlocker")

        // Clear configuration
        setSharedConfiguration(null)

        // Clear the Objective-C side configuration
        val emptyConfig = NetworkConfiguration(
            allowedHosts = setOf("*"),  // Allow all
            blockedHosts = emptySet()
        )
        setURLProtocolConfiguration(emptyConfig)

        isInstalled = false
        DebugLogger.log("iOS NetworkBlocker uninstalled successfully")
    }
}
