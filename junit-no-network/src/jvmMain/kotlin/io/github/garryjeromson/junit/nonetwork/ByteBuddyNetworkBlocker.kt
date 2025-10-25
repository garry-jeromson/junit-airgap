package io.github.garryjeromson.junit.nonetwork

/**
 * Byte Buddy-based network blocker (EXPERIMENTAL - DOES NOT WORK).
 *
 * ⚠️ WARNING: This implementation does NOT actually block network requests.
 *
 * ## Why Byte Buddy Cannot Work
 *
 * Socket constructors like `Socket("example.com", 80)` connect immediately by calling
 * native code directly, bypassing Java bytecode. Byte Buddy can only intercept Java
 * bytecode, not native method calls.
 *
 * The only way to intercept these connections is via SecurityManager, which is called
 * by the JVM from native code. SecurityManager is deprecated in Java 17+ and will be
 * removed in Java 24+.
 *
 * ## Why This Class Exists
 *
 * This stub implementation is kept for API compatibility and to allow experimentation.
 * The implementation selection framework is in place, but only SECURITY_MANAGER actually works.
 *
 * ## Alternative Approaches Considered
 *
 * - JavaAgent with -javaagent flag: Still can't intercept native calls
 * - Proxy pattern: Would require users to change their code
 * - Network-level interception: Requires actual network binding
 * - OS-level sandboxing: Not suitable for unit tests
 *
 * ## Recommendation
 *
 * Use SECURITY_MANAGER implementation (with deprecation warnings suppressed) until Java 24.
 * After Java 24, this entire library will stop working as there is no replacement for
 * SecurityManager's interception capabilities.
 */
internal class ByteBuddyNetworkBlocker(
    private val configuration: NetworkConfiguration,
) : NetworkBlockerStrategy {
    private var isInstalled: Boolean = false

    /**
     * Stub install method - does nothing.
     * This implementation does not actually block network requests.
     */
    @Synchronized
    override fun install() {
        if (isInstalled) {
            return // Already installed, idempotent
        }

        // Mark as installed but don't actually do anything
        // Byte Buddy cannot intercept Socket constructors reliably
        isInstalled = true
    }

    /**
     * Stub uninstall method - does nothing.
     */
    @Synchronized
    override fun uninstall() {
        if (!isInstalled) {
            return // Not installed, idempotent
        }

        isInstalled = false
    }

    /**
     * Check if Byte Buddy strategy is available.
     * Returns true if Byte Buddy classes are on the classpath.
     *
     * Note: Even if available, this implementation does not actually work.
     */
    override fun isAvailable(): Boolean =
        try {
            // Try to load Byte Buddy classes
            Class.forName("net.bytebuddy.agent.ByteBuddyAgent")
            Class.forName("net.bytebuddy.agent.builder.AgentBuilder")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

    override fun getImplementation(): NetworkBlockerImplementation = NetworkBlockerImplementation.BYTE_BUDDY
}
