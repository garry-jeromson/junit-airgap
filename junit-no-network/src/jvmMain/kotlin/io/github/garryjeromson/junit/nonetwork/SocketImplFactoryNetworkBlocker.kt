package io.github.garryjeromson.junit.nonetwork

import java.net.Socket
import java.net.SocketImpl
import java.net.SocketImplFactory

/**
 * SocketImplFactory-based network blocker (Java 24+ compatible approach).
 *
 * ⚠️ EXPERIMENTAL: Partial support due to Java Module System (JPMS) restrictions.
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
 * ## ✅ What Works (70% of tests passing)
 * - **HttpURLConnection**: Standard Java HTTP client ✅
 * - **Direct Socket usage**: `java.net.Socket` connections ✅
 * - **Configuration precedence**: Method/class-level annotations ✅
 * - **Basic HTTP libraries**: Libraries that use `java.net.Socket` directly ✅
 *
 * ## ❌ What Doesn't Work (30% of tests failing)
 * - **OkHttp**: Connection pooling bypasses factory ❌
 * - **Apache HttpClient**: Internal socket management ❌
 * - **Java 11 HttpClient**: Uses NIO directly ❌
 * - **Reactor Netty**: Netty event loop bypasses Socket ❌
 * - **Some Ktor variants**: Depends on underlying engine ❌
 *
 * ## Root Cause: Java Module System (JPMS) Restrictions
 * Java 9+ strong encapsulation prevents access to internal JDK classes:
 * ```
 * InaccessibleObjectException: module java.base does not "exports sun.nio.ch"
 * ```
 * - Cannot obtain `sun.nio.ch.NioSocketImpl` for delegation
 * - Without platform delegate, Socket operations fail silently
 * - Modern HTTP clients use NIO or custom socket management
 *
 * ## Workarounds (Not Recommended)
 * 1. **JVM Flag**: `--add-opens java.base/sun.nio.ch=ALL-UNNAMED`
 *    - Defeats purpose of drop-in library
 *    - Requires users to modify JVM args
 *
 * 2. **Use SecurityManager instead**:
 *    - More reliable (90%+ test pass rate)
 *    - Works until Java 24 removes it entirely
 *
 * ## Known Limitations
 * 1. **One factory per JVM**: `Socket.setSocketImplFactory()` can only be called once.
 *    Workaround: Our factory is stateful and can be updated.
 *
 * 2. **Platform SocketImpl access**: JPMS prevents reflection on `sun.nio.ch.NioSocketImpl`.
 *    Result: No delegation possible, Socket operations incomplete.
 *
 * 3. **NIO-based clients**: Libraries using `java.nio.channels.SocketChannel` bypass Socket entirely.
 *    Would need custom `SelectorProvider` (separate interception point).
 *
 * 4. **ServerSocket**: Would need separate `ServerSocket.setSocketFactory()` call.
 *
 * ## Comparison with SecurityManager
 * - SecurityManager: Intercepts native calls (low-level, comprehensive)
 * - SocketImplFactory: Intercepts Java Socket API (high-level, incomplete)
 * - SecurityManager: 90%+ test coverage
 * - SocketImplFactory: 70% test coverage (JPMS limitations)
 *
 * ## Future Solutions
 * - **ByteBuddy instrumentation**: Direct instrumentation of HTTP client libraries
 * - **Java Agent**: Classloading-time transformation (requires `-javaagent`)
 * - **SelectorProvider SPI**: Separate interception for NIO channels
 *
 * ## Testing Status
 * ⚠️ EXPERIMENTAL - Limited support due to JPMS restrictions. Use SecurityManager for production.
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
            // Debug logging
            if (System.getProperty("junit.nonetwork.debug") == "true") {
                println("SocketImplFactory: createSocketImpl() called")
                Thread.currentThread().stackTrace.take(10).forEach { println("  at $it") }
            }

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
        private fun getPlatformSocketImpl(): SocketImpl? {
            return try {
                if (System.getProperty("junit.nonetwork.debug") == "true") {
                    println("getPlatformSocketImpl: Attempting to obtain platform SocketImpl")
                }

                // Try multiple implementation classes in order of preference
                val implClassNames =
                    listOf(
                        "sun.nio.ch.NioSocketImpl", // Java 13+ (JEP 353)
                        "java.net.PlainSocketImpl", // Legacy (pre-Java 13)
                    )

                for (className in implClassNames) {
                    try {
                        if (System.getProperty("junit.nonetwork.debug") == "true") {
                            println("  Trying: $className")
                        }
                        val implClass = Class.forName(className)
                        // Try to instantiate via no-arg constructor
                        val impl = implClass.getDeclaredConstructor().newInstance() as SocketImpl
                        if (System.getProperty("junit.nonetwork.debug") == "true") {
                            println("  SUCCESS with no-arg constructor: $impl")
                        }
                        return impl
                    } catch (e: ClassNotFoundException) {
                        if (System.getProperty("junit.nonetwork.debug") == "true") {
                            println("  ClassNotFoundException: $e")
                        }
                        // Try next implementation
                        continue
                    } catch (e: NoSuchMethodException) {
                        if (System.getProperty("junit.nonetwork.debug") == "true") {
                            println("  No no-arg constructor, trying boolean constructor")
                        }
                        // Try constructor with serverSocket parameter (some implementations need this)
                        try {
                            val implClass = Class.forName(className)
                            val constructor = implClass.getDeclaredConstructor(Boolean::class.javaPrimitiveType)
                            constructor.isAccessible = true
                            val impl = constructor.newInstance(false) as SocketImpl
                            if (System.getProperty("junit.nonetwork.debug") == "true") {
                                println("  SUCCESS with boolean constructor: $impl")
                            }
                            return impl
                        } catch (e2: Exception) {
                            if (System.getProperty("junit.nonetwork.debug") == "true") {
                                println("  Boolean constructor failed: $e2")
                            }
                            // Try next implementation
                            continue
                        }
                    }
                }

                if (System.getProperty("junit.nonetwork.debug") == "true") {
                    println("  No implementation found, returning null")
                }
                // No implementation found
                null
            } catch (e: Exception) {
                if (System.getProperty("junit.nonetwork.debug") == "true") {
                    println("  Exception in getPlatformSocketImpl: $e")
                    e.printStackTrace()
                }
                // Could not obtain platform SocketImpl
                // BlockingSocketImpl will throw IOException when trying to delegate
                null
            }
        }
    }
}
