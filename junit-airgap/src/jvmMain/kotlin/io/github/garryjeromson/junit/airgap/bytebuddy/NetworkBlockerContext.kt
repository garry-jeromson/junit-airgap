package io.github.garryjeromson.junit.airgap.bytebuddy

import io.github.garryjeromson.junit.airgap.DebugLogger
import io.github.garryjeromson.junit.airgap.NetworkConfiguration
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.airgap.NetworkRequestDetails

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
     * Debug logger for troubleshooting network blocking issues.
     */
    private val logger = DebugLogger.instance

    /**
     * Set the configuration for the current thread.
     *
     * @param configuration Network configuration for this test
     */
    @JvmStatic
    fun setConfiguration(configuration: NetworkConfiguration) {
        // Set the generation on the configuration
        configuration.generation = currentGeneration

        logger.debug { "NetworkBlockerContext: Setting configuration for thread ${Thread.currentThread().name}" }
        logger.debug { "  allowedHosts: ${configuration.allowedHosts}" }
        logger.debug { "  blockedHosts: ${configuration.blockedHosts}" }
        logger.debug { "  generation: ${configuration.generation}" }

        globalConfiguration = configuration
        configurationThreadLocal.set(configuration)
    }

    /**
     * Clear the configuration for the current thread.
     * Also increments the generation counter to invalidate any stale configurations
     * in child threads (worker threads, coroutine threads, etc.).
     */
    @JvmStatic
    fun clearConfiguration() {
        logger.debug { "NetworkBlockerContext: Clearing configuration for thread ${Thread.currentThread().name}" }
        logger.debug { "  Incrementing generation: $currentGeneration -> ${currentGeneration + 1}" }

        globalConfiguration = null
        configurationThreadLocal.remove()
        currentGeneration++ // Invalidate all inherited configurations
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
        val threadName = Thread.currentThread().name

        logger.debug { "NetworkBlockerContext.getConfiguration() on thread $threadName" }
        logger.debug { "  ThreadLocal config: $config" }
        logger.debug { "  Current generation: $currentGeneration" }
        logger.debug { "  Global config: $globalConfiguration" }

        // If we have a config and it matches current generation, use it
        if (config != null && config.generation == currentGeneration) {
            logger.debug { "  Using thread-local config (generation matches)" }
            return config
        }

        // Don't use global configuration for Gradle worker threads
        // This prevents the JVMTI agent from blocking Gradle's Maven artifact fetching
        // See: docs/investigation/macos-ci-failures.md
        if (isGradleWorkerThread(threadName)) {
            logger.debug { "  Detected Gradle worker thread, not applying global configuration" }
            return null
        }

        // Otherwise, use the global configuration (for HTTP client worker threads)
        val global = globalConfiguration
        if (global != null) {
            logger.debug { "  Using global configuration (thread-local was stale or missing)" }
        } else {
            logger.debug { "  No configuration available!" }
        }
        return global
    }

    /**
     * Check if the current thread is a Gradle worker thread.
     * Gradle worker threads handle dependency resolution, artifact fetching, etc.
     * We don't want to block these threads as they're part of Gradle's infrastructure.
     *
     * Common Gradle thread names:
     * - "Execution worker" - Gradle task execution workers
     * - "Daemon worker" - Gradle daemon background workers
     * - "Test worker" - Gradle test execution workers (but we WANT to block these!)
     *
     * @param threadName Name of the thread to check
     * @return true if this is a Gradle worker thread (but NOT a test worker)
     */
    private fun isGradleWorkerThread(threadName: String): Boolean {
        // Don't exclude test workers - we want to block network in tests!
        if (threadName.startsWith("Test worker")) {
            return false
        }

        // Exclude Gradle execution/daemon workers (infrastructure threads)
        return threadName.startsWith("Execution worker") ||
            threadName.startsWith("Daemon worker") ||
            threadName.startsWith("daemon worker")
    }

    /**
     * Check if there is an active configuration for the current thread.
     * Called by the JVMTI agent to determine if it should perform string
     * extraction operations (which require platform encoding to be initialized).
     *
     * If no configuration is active, the agent can skip all JNI string operations
     * and immediately allow the connection.
     *
     * @return true if a configuration is set for the current thread, false otherwise
     */
    @JvmStatic
    fun hasActiveConfiguration(): Boolean {
        val config = getConfiguration()
        val hasConfig = config != null
        logger.debug {
            "NetworkBlockerContext.hasActiveConfiguration() = $hasConfig (thread=${Thread.currentThread().name})"
        }
        return hasConfig
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

        logger.debug { "NetworkBlockerContext.checkConnection: host=$host, port=$port, caller=$caller" }
        logger.debug { "  Configuration: $configuration" }

        // No configuration = no blocking
        if (configuration == null) {
            logger.debug { "  No configuration set, allowing connection" }
            return
        }

        // Check if this is Robolectric's MavenArtifactFetcher downloading android-all JARs
        // Robolectric lazily downloads Android framework JARs at test runtime from Maven Central
        // These downloads are infrastructure, not test code, so we should allow them
        if (isRobolectricArtifactDownload()) {
            logger.debug { "  Detected Robolectric artifact download, allowing connection to $host:$port" }
            return
        }

        // Check if allowed
        val allowed = configuration.isAllowed(host)
        logger.debug { "  isAllowed($host) = $allowed" }

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
     * Check if the current call stack indicates a Robolectric artifact download.
     * Robolectric's MavenArtifactFetcher downloads android-all JARs at test runtime.
     *
     * @return true if this is a Robolectric artifact download
     */
    @JvmStatic // Make accessible for testing
    internal fun isRobolectricArtifactDownload(): Boolean {
        val stackTrace = Thread.currentThread().stackTrace

        // DEBUG: Print full stack trace to investigate CI failures
        logger.debug { "=== FULL STACK TRACE FOR ROBOLECTRIC DETECTION ===" }
        stackTrace.forEachIndexed { index, element ->
            logger.debug { "  [$index] ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})" }
        }
        logger.debug { "=== END STACK TRACE ===" }

        return stackTrace.any { element ->
            val className = element.className
            val matches =
                className.contains("org.robolectric.internal.dependency.MavenArtifactFetcher") ||
                    className.contains("org.robolectric.internal.dependency.MavenDependencyResolver")
            if (matches) {
                logger.debug { "Detected Robolectric class in stack: $className" }
            }
            matches
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

        logger.debug { "NetworkBlockerContext.isExplicitlyBlocked: host=$host" }
        logger.debug { "  Configuration: $configuration" }

        // No configuration = not blocked
        if (configuration == null) {
            logger.debug { "  No configuration set, not blocked" }
            return false
        }

        // Check if host matches any pattern in blockedHosts
        val normalizedHost = host.lowercase()
        val blocked =
            configuration.blockedHosts.any { pattern ->
                val matches = matchesPattern(normalizedHost, pattern.lowercase())
                if (matches) {
                    logger.debug { "  Host $host matches blocked pattern: $pattern" }
                }
                matches
            }

        logger.debug { "  isExplicitlyBlocked($host) = $blocked" }

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
