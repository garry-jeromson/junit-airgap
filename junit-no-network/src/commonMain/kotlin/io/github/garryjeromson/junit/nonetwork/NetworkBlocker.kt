package io.github.garryjeromson.junit.nonetwork

/**
 * Core component that intercepts and blocks network requests based on configuration.
 * Platform-specific implementations use different mechanisms:
 * - JVM: SecurityManager
 * - Android: SocketFactory replacement
 */
expect class NetworkBlocker(
    configuration: NetworkConfiguration,
) {
    /**
     * Installs the network blocker.
     * After installation, network connections will be checked against the configuration.
     */
    fun install()

    /**
     * Uninstalls the network blocker.
     * After uninstallation, network requests will work normally again.
     */
    fun uninstall()
}
