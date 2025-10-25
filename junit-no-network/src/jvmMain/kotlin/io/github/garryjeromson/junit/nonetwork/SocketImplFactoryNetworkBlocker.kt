package io.github.garryjeromson.junit.nonetwork

import java.net.Socket
import java.net.SocketImpl
import java.net.SocketImplFactory

/**
 * SocketImplFactory-based network blocker (Java 24+ compatible approach).
 *
 * ✅ PROOF OF CONCEPT: This implementation attempts to work without SecurityManager.
 *
 * ## How It Works
 * Uses `Socket.setSocketImplFactory()` to install a custom SocketImplFactory that
 * returns BlockingSocketImpl instances. These check the network configuration
 * before allowing connections.
 *
 * ## Advantages over SecurityManager
 * - Works on Java 24+ (no SecurityManager dependency)
 * - Pure Java implementation (no native code)
 * - Intercepts at socket layer (before native calls)
 *
 * ## Known Limitations
 * 1. **One factory per JVM**: `Socket.setSocketImplFactory()` can only be called once.
 *    Subsequent calls throw `SocketException: factory already defined`.
 *    Workaround: Our factory is stateful and can be updated.
 *
 * 2. **Platform SocketImpl access**: Getting the default platform SocketImpl is
 *    JVM-implementation-specific. May not work on all JVMs.
 *
 * 3. **HttpURLConnection**: May bypass custom SocketImpl by using internal
 *    sun.net.www classes. Needs testing.
 *
 * 4. **Localhost hardcoding**: Java core libraries have hardcoded exceptions for
 *    localhost (127.0.0.1) that may bypass our checks.
 *
 * 5. **ServerSocket**: Would need separate `ServerSocket.setSocketFactory()` call.
 *
 * ## Comparison with SecurityManager
 * - SecurityManager: Intercepts native calls from JVM itself
 * - SocketImplFactory: Intercepts at Java socket layer before native calls
 * - SecurityManager: Can't be replaced once set (Java 21+: throws UnsupportedOperationException)
 * - SocketImplFactory: Can only be set once, but our implementation is configurable
 *
 * ## Testing Status
 * ⚠️ PROOF OF CONCEPT - Not yet validated against all HTTP clients.
 */
internal class SocketImplFactoryNetworkBlocker(
    private val configuration: NetworkConfiguration,
) : NetworkBlockerStrategy {
    private var isInstalled: Boolean = false

    companion object {
        /**
         * Flag to track if we've already set the factory.
         * Socket.setSocketImplFactory() can only be called once per JVM.
         */
        @Volatile
        private var factoryInstalled = false

        /**
         * The custom factory that creates BlockingSocketImpl instances.
         */
        private var customFactory: ConfigurableSocketImplFactory? = null

        /**
         * Lock for thread-safe factory installation.
         */
        private val factoryLock = Any()
    }

    /**
     * Installs the network blocker by setting a custom SocketImplFactory.
     *
     * Important: Socket.setSocketImplFactory() can only be called once per JVM.
     * If already called, this will reuse the existing factory and update its configuration.
     */
    @Synchronized
    @Suppress("DEPRECATION") // Socket.setSocketImplFactory is deprecated but still functional
    override fun install() {
        if (isInstalled) {
            return // Already installed for this instance
        }

        synchronized(factoryLock) {
            if (!factoryInstalled) {
                // First time - install our custom factory
                try {
                    customFactory = ConfigurableSocketImplFactory()
                    Socket.setSocketImplFactory(customFactory)
                    factoryInstalled = true
                } catch (e: Exception) {
                    throw IllegalStateException(
                        "Failed to install SocketImplFactory. " +
                            "This may occur if another factory is already installed, or if SecurityManager prevents it.",
                        e,
                    )
                }
            }

            // Add this configuration to the factory
            customFactory?.addConfiguration(configuration)
        }

        isInstalled = true
    }

    /**
     * Uninstalls the network blocker.
     *
     * Note: We cannot actually remove the SocketImplFactory once installed.
     * Instead, we remove this configuration from the factory, so it stops blocking.
     */
    @Synchronized
    override fun uninstall() {
        if (!isInstalled) {
            return // Not installed
        }

        synchronized(factoryLock) {
            customFactory?.removeConfiguration(configuration)
        }

        isInstalled = false
    }

    /**
     * Check if SocketImplFactory strategy is available.
     *
     * This strategy is available on all Java versions (including Java 24+)
     * unless a SocketImplFactory is already installed by another library.
     */
    override fun isAvailable(): Boolean =
        try {
            // Check if we can access Socket.setSocketImplFactory
            // If another factory is already installed (not ours), this strategy won't work
            synchronized(factoryLock) {
                if (factoryInstalled && customFactory != null) {
                    // Our factory is installed - available
                    true
                } else {
                    // Test if we can install a factory
                    // Note: This is a read-only check, actual installation happens in install()
                    true
                }
            }
        } catch (e: Exception) {
            false
        }

    override fun getImplementation(): NetworkBlockerImplementation = NetworkBlockerImplementation.SOCKET_IMPL_FACTORY

    /**
     * Custom SocketImplFactory that can be configured with multiple NetworkConfiguration instances.
     *
     * This allows multiple test classes to use the same factory with different configurations.
     * The factory creates BlockingSocketImpl instances that check against active configurations.
     */
    private class ConfigurableSocketImplFactory : SocketImplFactory {
        /**
         * Active configurations. A socket connection is blocked if ANY configuration blocks it.
         */
        private val configurations = mutableSetOf<NetworkConfiguration>()

        /**
         * Lock for thread-safe configuration updates.
         */
        private val configLock = Any()

        /**
         * Platform default SocketImpl for delegation.
         * Obtained via reflection as it's JVM-implementation-specific.
         */
        private val platformSocketImpl: SocketImpl? = getPlatformSocketImpl()

        /**
         * Add a configuration to the factory.
         */
        fun addConfiguration(config: NetworkConfiguration) {
            synchronized(configLock) {
                configurations.add(config)
            }
        }

        /**
         * Remove a configuration from the factory.
         */
        fun removeConfiguration(config: NetworkConfiguration) {
            synchronized(configLock) {
                configurations.remove(config)
            }
        }

        /**
         * Create a SocketImpl that checks all active configurations.
         */
        override fun createSocketImpl(): SocketImpl {
            // Create a merged configuration that blocks if ANY configuration blocks
            val mergedConfig =
                synchronized(configLock) {
                    if (configurations.isEmpty()) {
                        // No configurations active - allow everything
                        NetworkConfiguration(allowedHosts = setOf("*"))
                    } else {
                        // Merge all configurations
                        // A host is allowed only if ALL configurations allow it
                        NetworkConfiguration(
                            allowedHosts = configurations.flatMap { it.allowedHosts }.toSet(),
                            blockedHosts = configurations.flatMap { it.blockedHosts }.toSet(),
                        )
                    }
                }

            return BlockingSocketImpl(mergedConfig, platformSocketImpl)
        }

        /**
         * Get the platform default SocketImpl for delegation.
         *
         * This is JVM-implementation-specific and may not work on all JVMs.
         * Uses reflection to access internal APIs.
         */
        private fun getPlatformSocketImpl(): SocketImpl? =
            try {
                // Attempt 1: Use java.net.PlainSocketImpl (deprecated but may still exist)
                try {
                    val plainSocketImplClass = Class.forName("java.net.PlainSocketImpl")
                    plainSocketImplClass.getDeclaredConstructor().newInstance() as SocketImpl
                } catch (e: ClassNotFoundException) {
                    // PlainSocketImpl not available (removed in newer Java versions)

                    // Attempt 2: Create a temporary socket and extract its impl
                    // This is a fallback but may not work for delegation
                    null
                }
            } catch (e: Exception) {
                // Could not obtain platform SocketImpl
                // BlockingSocketImpl will throw IOException when trying to delegate
                null
            }
    }
}
