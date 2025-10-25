package io.github.garryjeromson.junit.nonetwork

/**
 * Enum representing the network blocking implementation strategy.
 *
 * This library now exclusively uses JVMTI for network blocking.
 */
enum class NetworkBlockerImplementation {
    /**
     * JVMTI agent implementation - native interception (Java 7+ compatible).
     *
     * ✅ This implementation blocks network connections by intercepting at the JVM-native boundary.
     *
     * ⚠️ REQUIRES -agentpath: The JVMTI agent MUST be loaded at JVM startup.
     *
     * ## How It Works
     * Uses a JVMTI native agent to intercept `sun.nio.ch.Net.connect0()`, the single
     * native method used by ALL modern Java socket implementations.
     *
     * ## Target Coverage: 95%+ of tests
     * Intercepts EVERYTHING because ALL Java network code eventually calls Net.connect0():
     * - **Direct Socket**: java.net.Socket ✅
     * - **HttpURLConnection**: Standard library HTTP ✅
     * - **OkHttp**: Modern HTTP client ✅
     * - **Apache HttpClient**: Enterprise HTTP client ✅
     * - **Java 11 HttpClient**: java.net.http.HttpClient ✅
     * - **Reactor Netty**: Reactive framework ✅
     * - **Ktor**: Kotlin HTTP client ✅
     * - **Any NIO client**: SocketChannel, AsynchronousSocketChannel ✅
     *
     * ## Advantages
     * - ✅ Works on Java 7+ forever (JVMTI is stable JVM spec)
     * - ✅ No SecurityManager dependency (future-proof for Java 24+)
     * - ✅ Intercepts ALL socket connections (95%+ coverage)
     * - ✅ Zero runtime overhead (function pointer replacement)
     * - ✅ No dependencies (pure native code)
     * - ✅ No JPMS restrictions
     * - ✅ Most comprehensive solution
     *
     * ## Requirements
     * - Native agent must be built (CMake + C++ compiler)
     * - Agent must be loaded at JVM startup: `-agentpath:/path/to/agent.dylib`
     * - Gradle plugin handles this automatically
     *
     * ## Limitations
     * - Requires -agentpath at JVM startup (cannot load at runtime)
     * - Requires native build (CMake + C++)
     * - Platform-specific binaries (.dylib/.so/.dll)
     *
     * ## Migration from Other Strategies
     * This library previously supported SecurityManager, ByteBuddy, and other strategies.
     * Those have been removed in favor of the JVMTI approach, which provides:
     * - Better future compatibility (no deprecated APIs)
     * - Higher coverage (intercepts everything)
     * - Lower runtime overhead
     */
    JVMTI,
    ;

    companion object {
        /**
         * Parse a string to NetworkBlockerImplementation.
         *
         * Supported values (case-insensitive):
         * - "jvmti" -> JVMTI
         * - null -> default()
         *
         * @param value String representation of the implementation
         * @return NetworkBlockerImplementation
         * @throws IllegalArgumentException if value is not recognized
         */
        fun fromString(value: String?): NetworkBlockerImplementation =
            when (value?.lowercase()?.replace("-", "")) {
                "jvmti" -> JVMTI
                null -> default()
                else ->
                    throw IllegalArgumentException(
                        "Unknown implementation: $value. Only 'jvmti' is supported.",
                    )
            }

        /**
         * Get the default implementation.
         *
         * Returns JVMTI - the only implementation in this library.
         */
        fun default(): NetworkBlockerImplementation = JVMTI
    }
}
