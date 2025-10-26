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
     * Uses InheritableThreadLocal so that configuration is inherited by child threads
     * (e.g., coroutine workers, HTTP client threads).
     */
    private val configurationThreadLocal = InheritableThreadLocal<NetworkConfiguration?>()

    /**
     * Global generation counter to invalidate stale configurations in inherited threads.
     * Incremented each time clearConfiguration() is called.
     */
    @Volatile
    private var currentGeneration = 0L

    /**
     * Global reference to the currently active configuration.
     * Used to provide fresh configuration to worker threads.
     */
    @Volatile
    private var globalConfiguration: NetworkConfiguration? = null

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
        // Create a new configuration with the current generation
        val configWithGeneration = configuration.copy(generation = currentGeneration)

        if (debugMode) {
            println("NetworkBlockerContext: Setting configuration for thread ${Thread.currentThread().name}")
            println("  allowedHosts: ${configWithGeneration.allowedHosts}")
            println("  blockedHosts: ${configWithGeneration.blockedHosts}")
            println("  generation: ${configWithGeneration.generation}")
        }
        globalConfiguration = configWithGeneration
        configurationThreadLocal.set(configWithGeneration)
    }

    /**
     * Clear the configuration for the current thread.
     * Also increments the generation counter to invalidate any stale configurations
     * in child threads (worker threads, coroutine threads, etc.).
     */
    @JvmStatic
    fun clearConfiguration() {
        if (debugMode) {
            println("NetworkBlockerContext: Clearing configuration for thread ${Thread.currentThread().name}")
            println("  Incrementing generation: $currentGeneration -> ${currentGeneration + 1}")
        }
        globalConfiguration = null
        configurationThreadLocal.remove()
        currentGeneration++  // Invalidate all inherited configurations
    }

    /**
     * Get the current thread's configuration.
     * If the thread-local config is stale, returns the global config instead.
     *
     * @return Current configuration, or null if not set
     */
    @JvmStatic
    fun getConfiguration(): NetworkConfiguration? {
        val config = configurationThreadLocal.get()

        if (debugMode) {
            println("NetworkBlockerContext.getConfiguration() on thread ${Thread.currentThread().name}")
            println("  ThreadLocal config: $config")
            println("  Current generation: $currentGeneration")
            println("  Global config: $globalConfiguration")
        }

        // If we have a config and it matches current generation, use it
        if (config != null && config.generation == currentGeneration) {
            if (debugMode) println("  Using thread-local config (generation matches)")
            return config
        }

        // Otherwise, use the global configuration (for worker threads)
        val global = globalConfiguration
        if (global != null && debugMode) {
            println("  Using global configuration (thread-local was stale or missing)")
        } else if (debugMode) {
            println("  No configuration available!")
        }
        return global
    }

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

    /**
     * Check if a host is explicitly in the blockedHosts list.
     *
     * This is used by the JVMTI agent to determine if a hostname/IP should be blocked
     * regardless of whether an alternative identifier (hostname vs IP) is allowed.
     *
     * @param host Hostname or IP address to check
     * @return true if host is explicitly blocked, false otherwise
     */
    @JvmStatic
    fun isExplicitlyBlocked(host: String): Boolean {
        val configuration = getConfiguration()

        if (debugMode) {
            println("NetworkBlockerContext.isExplicitlyBlocked: host=$host")
            println("  Configuration: $configuration")
        }

        // No configuration = not blocked
        if (configuration == null) {
            if (debugMode) {
                println("  No configuration set, not blocked")
            }
            return false
        }

        // Check if host matches any pattern in blockedHosts
        val normalizedHost = host.lowercase()
        val blocked = configuration.blockedHosts.any { pattern ->
            val matches = matchesPattern(normalizedHost, pattern.lowercase())
            if (debugMode && matches) {
                println("  Host $host matches blocked pattern: $pattern")
            }
            matches
        }

        if (debugMode) {
            println("  isExplicitlyBlocked($host) = $blocked")
        }

        return blocked
    }

    /**
     * Check if a host matches a wildcard pattern.
     * This mirrors the logic in NetworkConfiguration.matchesPattern().
     *
     * @param host Normalized (lowercase) hostname
     * @param pattern Normalized (lowercase) pattern with wildcards
     * @return true if host matches pattern
     */
    private fun matchesPattern(
        host: String,
        pattern: String,
    ): Boolean {
        // Handle wildcard for "allow all"
        if (pattern == "*") {
            return true
        }

        // Convert wildcard pattern to regex
        // Escape special regex characters except *
        val regexPattern =
            pattern
                .replace(".", "\\.")
                .replace("*", ".*")

        return host.matches(Regex("^$regexPattern$"))
    }
}
