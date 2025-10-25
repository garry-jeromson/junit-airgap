package io.github.garryjeromson.junit.nonetwork.bytebuddy

import io.github.garryjeromson.junit.nonetwork.NetworkConfiguration
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NetworkRequestDetails

/**
 * Thread-local context for network blocking configuration.
 *
 * ByteBuddy Advice classes need static access to configuration. This context
 * provides thread-safe access using ThreadLocal storage.
 *
 * ## Why ThreadLocal?
 * - Each test runs in its own thread
 * - Configuration is test-specific (method/class-level annotations)
 * - Advice code is inlined (can't pass parameters)
 * - Need static access from intercepted methods
 *
 * ## Usage in Advice Classes
 * ```kotlin
 * @Advice.OnMethodEnter
 * @JvmStatic
 * fun enter(@Advice.Argument(0) host: String, @Advice.Argument(1) port: Int) {
 *     NetworkBlockerContext.checkConnection(host, port)
 * }
 * ```
 *
 * ## JVMTI Integration
 * This class also serves as the configuration source for the JVMTI native agent.
 * The static initializer registers this class with the agent (if loaded) to cache
 * class and method references, avoiding FindClass issues from native contexts.
 */
object NetworkBlockerContext {
    init {
        // Register with JVMTI agent (if loaded) to cache class/method references.
        // This avoids FindClass issues when the native agent tries to look us up
        // from a native method context (which uses bootstrap class loader).
        //
        // If the JVMTI agent is not loaded, this will throw UnsatisfiedLinkError,
        // which we catch and ignore (graceful degradation).
        try {
            registerWithAgent()
        } catch (e: UnsatisfiedLinkError) {
            // Agent not loaded - this is fine, JVMTI agent may not be available
            // The library will continue to function without native interception
        }
    }

    /**
     * Native method to register this class with the JVMTI agent.
     * Called from static initializer to cache class/method references.
     * Only works if JVMTI agent is loaded via -agentpath at JVM startup.
     */
    @JvmStatic
    private external fun registerWithAgent()

    /**
     * Thread-local storage for network configuration.
     * Each test thread has its own configuration.
     */
    private val configurationThreadLocal = ThreadLocal<NetworkConfiguration?>()

    /**
     * Debug mode flag (read from system property).
     */
    private val debugMode: Boolean
        get() = System.getProperty("junit.nonetwork.debug") == "true"

    /**
     * Set the configuration for the current thread.
     *
     * @param configuration Network configuration for this test
     */
    @JvmStatic
    fun setConfiguration(configuration: NetworkConfiguration) {
        if (debugMode) {
            println("NetworkBlockerContext: Setting configuration for thread ${Thread.currentThread().name}")
            println("  allowedHosts: ${configuration.allowedHosts}")
            println("  blockedHosts: ${configuration.blockedHosts}")
        }
        configurationThreadLocal.set(configuration)
    }

    /**
     * Clear the configuration for the current thread.
     */
    @JvmStatic
    fun clearConfiguration() {
        if (debugMode) {
            println("NetworkBlockerContext: Clearing configuration for thread ${Thread.currentThread().name}")
        }
        configurationThreadLocal.remove()
    }

    /**
     * Get the current thread's configuration.
     *
     * @return Current configuration, or null if not set
     */
    @JvmStatic
    fun getConfiguration(): NetworkConfiguration? = configurationThreadLocal.get()

    /**
     * Check if a connection to the given host:port should be allowed.
     *
     * This is the main entry point called by Advice classes.
     *
     * @param host Hostname or IP address
     * @param port Port number
     * @param caller Name of the calling class (for debug logging)
     * @throws NetworkRequestAttemptedException if connection is blocked
     */
    @JvmStatic
    fun checkConnection(
        host: String,
        port: Int,
        caller: String = "unknown",
    ) {
        val configuration = getConfiguration()

        if (debugMode) {
            println("NetworkBlockerContext.checkConnection: host=$host, port=$port, caller=$caller")
            println("  Configuration: $configuration")
        }

        // No configuration = no blocking
        if (configuration == null) {
            if (debugMode) {
                println("  No configuration set, allowing connection")
            }
            return
        }

        // Check if allowed
        val allowed = configuration.isAllowed(host)
        if (debugMode) {
            println("  isAllowed($host) = $allowed")
        }

        if (!allowed) {
            val details =
                NetworkRequestDetails(
                    host = host,
                    port = port,
                    url = "$host:$port",
                    stackTrace =
                        Thread
                            .currentThread()
                            .stackTrace
                            .drop(2) // Skip checkConnection and caller
                            .take(10)
                            .joinToString("\n") { "  at $it" },
                )

            throw NetworkRequestAttemptedException(
                "Network request blocked by @BlockNetworkRequests: " +
                    "Attempted to connect to $host:$port via $caller",
                requestDetails = details,
            )
        }
    }

    /**
     * Check connection with URL (for URL-based APIs).
     *
     * @param url Full URL string
     * @param caller Name of the calling class
     */
    @JvmStatic
    fun checkConnectionUrl(
        url: String,
        caller: String = "unknown",
    ) {
        try {
            val parsedUrl = java.net.URL(url)
            val host = parsedUrl.host
            val port = if (parsedUrl.port != -1) parsedUrl.port else parsedUrl.defaultPort
            checkConnection(host, port, caller)
        } catch (e: Exception) {
            // If we can't parse URL, allow it (fail open rather than crash)
            if (debugMode) {
                println("NetworkBlockerContext: Failed to parse URL: $url, allowing connection")
                e.printStackTrace()
            }
        }
    }
}
