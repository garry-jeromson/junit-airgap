package io.github.garryjeromson.junit.nonetwork

import java.io.FilePermission
import java.lang.RuntimePermission
import java.lang.reflect.ReflectPermission
import java.net.NetPermission
import java.net.SocketPermission
import java.security.CodeSource
import java.security.Permission
import java.security.PermissionCollection
import java.security.Permissions
import java.security.Policy
import java.security.ProtectionDomain
import java.security.SecurityPermission
import java.util.PropertyPermission

/**
 * Security Policy-based network blocker (declarative implementation).
 *
 * WARNING: Still uses deprecated SecurityManager infrastructure. Will be removed in Java 24+.
 *
 * This implementation uses Java's Policy API to declaratively define permissions.
 * Unlike SecurityManagerNetworkBlocker (programmatic), this uses a declarative approach
 * by creating a custom Policy that denies SocketPermission for non-allowed hosts.
 *
 * ## How It Works
 * 1. Creates a custom Policy that grants all permissions EXCEPT SocketPermission for blocked hosts
 * 2. Installs this policy using Policy.setPolicy()
 * 3. Installs a default SecurityManager if none exists (required for policies to work)
 * 4. On uninstall, restores the original policy and security manager
 *
 * ## Differences from SecurityManagerNetworkBlocker
 * - SecurityManagerNetworkBlocker: Programmatic (custom SecurityManager.checkConnect())
 * - SecurityPolicyNetworkBlocker: Declarative (Policy API denies SocketPermission)
 * - Both rely on deprecated SecurityManager infrastructure
 *
 * Based on: https://tech.clevertap.com/using-javas-security-policy-to-block-internet-access-for-junit-tests/
 */
internal class SecurityPolicyNetworkBlocker(
    private val configuration: NetworkConfiguration,
) : NetworkBlockerStrategy {
    private var originalPolicy: Policy? = null
    private var originalSecurityManager: SecurityManager? = null
    private var isInstalled: Boolean = false

    /**
     * Installs the network blocker by setting a custom Policy and SecurityManager.
     * After installation, all socket connections will be checked against the configuration.
     */
    @Synchronized
    override fun install() {
        if (isInstalled) {
            return // Already installed, idempotent
        }

        try {
            // Save the original policy so we can restore it later
            originalPolicy = Policy.getPolicy()

            // Install our custom policy that blocks network connections
            val blockingPolicy = BlockingPolicy(configuration, originalPolicy)
            Policy.setPolicy(blockingPolicy)

            // Save the original security manager
            @Suppress("DEPRECATION")
            originalSecurityManager = System.getSecurityManager()

            // Install a custom SecurityManager that catches AccessControlExceptions
            // for socket connections and converts them to NetworkRequestAttemptedException
            @Suppress("DEPRECATION")
            System.setSecurityManager(BlockingSecurityManager(configuration, originalSecurityManager))

            isInstalled = true
        } catch (e: SecurityException) {
            throw IllegalStateException(
                "Failed to install network blocker. Policy or SecurityManager cannot be set.",
                e,
            )
        }
    }

    /**
     * Uninstalls the network blocker and restores the original Policy and SecurityManager.
     * After uninstallation, network requests will work normally again.
     */
    @Synchronized
    override fun uninstall() {
        if (!isInstalled) {
            return // Not installed, idempotent
        }

        try {
            // Restore the original policy
            Policy.setPolicy(originalPolicy)

            // Restore the original security manager
            @Suppress("DEPRECATION")
            System.setSecurityManager(originalSecurityManager)
        } catch (e: SecurityException) {
            // Best effort - if we can't restore, at least mark as uninstalled
            System.err.println("Warning: Could not restore original policy/security manager: ${e.message}")
        } finally {
            isInstalled = false
        }
    }

    /**
     * Check if Security Policy strategy is available.
     * Returns true on JVM where Policy API is available.
     */
    override fun isAvailable(): Boolean =
        try {
            // Check if we can get/set Policy and SecurityManager
            // This will be true on Java 17-23, false on Java 24+ (when removed)
            Policy.getPolicy()
            @Suppress("DEPRECATION")
            System.getSecurityManager()
            true
        } catch (e: Exception) {
            false
        }

    override fun getImplementation(): NetworkBlockerImplementation = NetworkBlockerImplementation.SECURITY_POLICY

    /**
     * Custom Policy that blocks network connections based on configuration.
     *
     * This policy grants ALL permissions EXCEPT SocketPermission for non-allowed hosts.
     * When a SocketPermission is requested, we check if the host is allowed.
     */
    private class BlockingPolicy(
        private val configuration: NetworkConfiguration,
        private val delegate: Policy?,
    ) : Policy() {
        override fun getPermissions(codesource: CodeSource?): PermissionCollection = getAllowedPermissions()

        override fun getPermissions(domain: ProtectionDomain?): PermissionCollection = getAllowedPermissions()

        /**
         * Returns all permissions that should be granted.
         * Based on CleverTap article approach - grant extensive local permissions.
         */
        private fun getAllowedPermissions(): PermissionCollection {
            val permissions = Permissions()

            // File permissions - full access
            permissions.add(FilePermission("<<ALL FILES>>", "read,write,execute,delete,readlink"))

            // Property permissions - read/write all properties
            permissions.add(PropertyPermission("*", "read,write"))

            // Reflection permissions - all reflection access
            permissions.add(ReflectPermission("suppressAccessChecks"))

            // Runtime permissions - all runtime operations
            permissions.add(RuntimePermission("*"))

            // Security permissions - all security operations
            permissions.add(SecurityPermission("*"))

            // Network permissions - general network operations (not socket connections)
            permissions.add(NetPermission("*"))

            // Localhost socket permissions - always allow localhost on all ports
            permissions.add(SocketPermission("localhost:*", "accept,listen,connect,resolve"))
            permissions.add(SocketPermission("127.0.0.1:*", "accept,listen,connect,resolve"))
            permissions.add(SocketPermission("[0:0:0:0:0:0:0:1]:*", "accept,listen,connect,resolve"))

            // DNS resolution for all hosts (needed for hostname lookups)
            permissions.add(SocketPermission("*:*", "resolve"))

            // Note: We do NOT add connect permission for external hosts
            // This will be checked in implies() where we can throw detailed exceptions

            return permissions
        }

        /**
         * Check if a specific permission should be granted.
         *
         * This is called after getPermissions() to check additional permissions
         * not statically granted. We primarily use this to block socket connections
         * to non-allowed hosts while throwing detailed exceptions.
         */
        override fun implies(
            domain: ProtectionDomain?,
            permission: Permission,
        ): Boolean {
            // Check if permission is in the granted set from getPermissions()
            val grantedPermissions = getAllowedPermissions()
            if (grantedPermissions.implies(permission)) {
                return true // Already granted via getPermissions()
            }

            // For SocketPermission, always grant and let the SecurityManager handle blocking
            // This ensures checkConnect() is called where we can throw detailed exceptions
            if (permission is SocketPermission) {
                return true
            }

            // For anything else not in granted permissions, delegate to original policy
            // or grant by default (we don't want to interfere)
            return delegate?.implies(domain, permission) ?: true
        }

        /**
         * Parse socket target from SocketPermission name.
         * Format can be "host:port", "host", or "*.host:port"
         */
        private fun parseSocketTarget(target: String): Pair<String, String?> {
            val parts = target.split(":")
            return when {
                parts.size >= 2 -> Pair(parts[0], parts[1])
                else -> Pair(parts[0], null)
            }
        }
    }

    /**
     * Custom SecurityManager that works with the BlockingPolicy.
     *
     * It intercepts checkConnect() calls and throws NetworkRequestAttemptedException
     * with detailed error information when connections are blocked.
     */
    private class BlockingSecurityManager(
        private val configuration: NetworkConfiguration,
        private val delegate: SecurityManager?,
    ) : SecurityManager() {
        override fun checkConnect(
            host: String,
            port: Int,
        ) {
            // Always allow localhost connections (localhost, 127.0.0.1, ::1)
            val normalizedHost = host.lowercase()
            if (normalizedHost == "localhost" ||
                normalizedHost == "127.0.0.1" ||
                normalizedHost.startsWith("127.") ||
                normalizedHost == "::1" ||
                normalizedHost == "0:0:0:0:0:0:0:1"
            ) {
                delegate?.checkConnect(host, port)
                return
            }

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

        // Delegate all other security checks to the original manager or do nothing
        override fun checkPermission(perm: java.security.Permission?) {
            // Don't delegate - let the Policy handle all other permissions
        }

        override fun checkPermission(
            perm: java.security.Permission?,
            context: Any?,
        ) {
            // Don't delegate - let the Policy handle all other permissions
        }
    }
}
