package io.github.garryjeromson.junit.nonetwork

import java.security.Permission

/**
 * SecurityManager-based network blocker (legacy implementation).
 *
 * WARNING: SecurityManager is deprecated in Java 17+ and will be removed in Java 24+.
 * Use ByteBuddyNetworkBlocker for future compatibility.
 *
 * This implementation installs a custom SecurityManager that intercepts all
 * socket connection attempts via checkConnect().
 */
internal class SecurityManagerNetworkBlocker(
    private val configuration: NetworkConfiguration,
) : NetworkBlockerStrategy {
    private var originalSecurityManager: SecurityManager? = null
    private var isInstalled: Boolean = false

    /**
     * Installs the network blocker by setting a custom SecurityManager.
     * After installation, all socket connections will be checked against the configuration.
     */
    @Synchronized
    override fun install() {
        if (isInstalled) {
            return // Already installed, idempotent
        }

        try {
            // Save the original security manager so we can restore it later
            originalSecurityManager = System.getSecurityManager()

            // Install our custom security manager
            @Suppress("DEPRECATION")
            System.setSecurityManager(BlockingSecurityManager(configuration, originalSecurityManager))

            isInstalled = true
        } catch (e: SecurityException) {
            throw IllegalStateException(
                "Failed to install network blocker. Security manager cannot be replaced.",
                e,
            )
        }
    }

    /**
     * Uninstalls the network blocker and restores the original SecurityManager.
     * After uninstallation, network requests will work normally again.
     */
    @Synchronized
    override fun uninstall() {
        if (!isInstalled) {
            return // Not installed, idempotent
        }

        try {
            // Restore the original security manager
            @Suppress("DEPRECATION")
            System.setSecurityManager(originalSecurityManager)
        } catch (e: SecurityException) {
            // Best effort - if we can't restore, at least mark as uninstalled
            System.err.println("Warning: Could not restore original security manager: ${e.message}")
        } finally {
            isInstalled = false
        }
    }

    /**
     * Check if SecurityManager strategy is available.
     * Always returns true on JVM (SecurityManager is part of core JDK).
     */
    override fun isAvailable(): Boolean =
        try {
            // Check if we can get/set SecurityManager
            // This will be true on Java 17-23, false on Java 24+ (when removed)
            @Suppress("DEPRECATION")
            System.getSecurityManager()
            true
        } catch (e: Exception) {
            false
        }

    override fun getImplementation(): NetworkBlockerImplementation = NetworkBlockerImplementation.SECURITY_MANAGER

    /**
     * Custom SecurityManager that intercepts socket connection attempts.
     */
    private class BlockingSecurityManager(
        private val configuration: NetworkConfiguration,
        private val delegate: SecurityManager?,
    ) : SecurityManager() {
        override fun checkConnect(
            host: String,
            port: Int,
        ) {
            // Check if this connection should be blocked
            if (!configuration.isAllowed(host)) {
                val details =
                    NetworkRequestDetails(
                        host = host,
                        port = port,
                        url = "$host:$port",
                        stackTrace =
                            Thread
                                .currentThread()
                                .stackTrace
                                .drop(1) // Skip this method
                                .take(5) // Take top 5 frames
                                .joinToString("\n") { "  at $it" },
                    )

                throw NetworkRequestAttemptedException(
                    "Network request blocked by @NoNetworkTest: Attempted to connect to $host:$port",
                    requestDetails = details,
                )
            }

            // If allowed, delegate to the original security manager if present
            delegate?.checkConnect(host, port)
        }

        override fun checkConnect(
            host: String,
            port: Int,
            context: Any?,
        ) {
            // Also check the version with context
            checkConnect(host, port)
            delegate?.checkConnect(host, port, context)
        }

        // Delegate all other security checks to the original manager
        override fun checkPermission(perm: Permission?) {
            delegate?.checkPermission(perm)
        }

        override fun checkPermission(
            perm: Permission?,
            context: Any?,
        ) {
            delegate?.checkPermission(perm, context)
        }
    }
}
