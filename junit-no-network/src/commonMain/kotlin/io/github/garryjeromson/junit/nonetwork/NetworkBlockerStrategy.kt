package io.github.garryjeromson.junit.nonetwork

/**
 * Internal strategy interface for network blocking implementations.
 *
 * This interface defines the contract that all network blocking strategies must implement.
 * Different strategies use different techniques to intercept and block network requests.
 */
internal interface NetworkBlockerStrategy {
    /**
     * Install the network blocker.
     *
     * After installation, network requests should be intercepted and checked
     * against the configuration.
     *
     * This operation should be idempotent - calling install() multiple times
     * should have the same effect as calling it once.
     */
    fun install()

    /**
     * Uninstall the network blocker and restore normal network behavior.
     *
     * After uninstallation, network requests should work normally again.
     *
     * This operation should be idempotent - calling uninstall() multiple times
     * should have the same effect as calling it once.
     */
    fun uninstall()

    /**
     * Check if this strategy is available in the current environment.
     *
     * For example:
     * - JVMTI strategy requires native agent to be loaded via -agentpath
     * - SocketImpl strategy requires ability to set SocketImplFactory
     *
     * @return true if this strategy can be used, false otherwise
     */
    fun isAvailable(): Boolean
}
