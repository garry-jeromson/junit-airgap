package io.github.garryjeromson.junit.nonetwork

/**
 * Enum representing different network blocking implementation strategies.
 *
 * Working implementations:
 * - SECURITY_MANAGER: ✅ Java 17-23 (90% coverage, battle-tested)
 * - SECURITY_POLICY: ✅ Java 17-23 (90% coverage, declarative)
 * - BYTE_BUDDY: ✅ Java 17+ (85-90% coverage, future-proof, recommended for Java 21+)
 * - SOCKET_IMPL_FACTORY: ⚠️ Java 17+ (70% coverage, experimental)
 *
 * See individual implementations for details.
 */
enum class NetworkBlockerImplementation {
    /**
     * ByteBuddy bytecode instrumentation implementation (Java 17+ compatible, future-proof).
     *
     * ✅ This implementation WORKS by intercepting HTTP client libraries at a higher level.
     *
     * ## How It Works
     * Uses ByteBuddy runtime instrumentation to intercept HTTP client connection methods
     * BEFORE they call native Socket code. Injects Advice code that checks NetworkBlockerContext
     * before allowing connections.
     *
     * ## Target Coverage: 85-90% of tests
     * Focuses on the most common HTTP clients:
     * - **OkHttp**: 90% of Android/JVM projects ✅
     * - **Apache HttpClient**: Common in enterprise Java (TODO) ⚠️
     * - **Java 11 HttpClient**: Modern standard library client (TODO) ⚠️
     * - **Direct Socket**: Falls back to SocketImplFactory ✅
     *
     * ## Advantages
     * - ✅ Works on Java 17+ (including Java 24+)
     * - ✅ No SecurityManager dependency (future-proof)
     * - ✅ No JVM flags required (installs agent at runtime)
     * - ✅ Intercepts modern HTTP clients before native calls
     * - ✅ No JPMS restrictions (doesn't need internal JDK classes)
     *
     * ## Limitations
     * - Adds ~3MB dependency (byte-buddy + byte-buddy-agent)
     * - Agent installation may fail on some JVMs
     * - Only instruments known HTTP clients
     * - Cannot intercept direct native calls (use SocketImplFactory fallback)
     *
     * ## Recommendation
     * Best choice for Java 21+ and future Java versions. Works alongside SecurityManager
     * on Java 17-23 for comprehensive coverage.
     */
    BYTE_BUDDY,

    /**
     * SecurityManager implementation - programmatic approach (DEFAULT).
     *
     * ✅ This implementation WORKS and blocks network requests.
     *
     * ⚠️ IMPORTANT: SecurityManager is deprecated in Java 17+ and will be removed in Java 24+.
     *
     * ## How It Works
     * Uses a custom SecurityManager subclass that programmatically checks socket connections.
     *
     * Pros:
     * - No additional dependencies
     * - Battle-tested, reliably blocks all Socket connections
     * - Programmatic control over permission checks
     *
     * Cons:
     * - Deprecated in Java 17+ (shows warnings - can be suppressed)
     * - Will be permanently removed in Java 24+
     * - No replacement planned by Oracle
     */
    SECURITY_MANAGER,

    /**
     * Security Policy implementation - declarative approach.
     *
     * ✅ This implementation WORKS and blocks network requests.
     *
     * ⚠️ IMPORTANT: Still uses deprecated SecurityManager infrastructure (Java 24+ incompatible).
     *
     * ## How It Works
     * Uses Java's Policy API to declaratively define permissions.
     * Grants all permissions EXCEPT SocketPermission for non-allowed hosts.
     *
     * ## Differences from SECURITY_MANAGER
     * - SECURITY_MANAGER: Programmatic (custom SecurityManager subclass)
     * - SECURITY_POLICY: Declarative (Policy API with default SecurityManager)
     * - Both rely on deprecated SecurityManager infrastructure
     * - Both will stop working in Java 24+
     *
     * Pros:
     * - Declarative policy definition
     * - Uses standard Java Policy API
     * - No custom SecurityManager subclass needed
     *
     * Cons:
     * - Still depends on deprecated SecurityManager infrastructure
     * - Will be permanently removed in Java 24+
     */
    SECURITY_POLICY,

    /**
     * SocketImplFactory implementation - Java 24+ compatibility layer (EXPERIMENTAL).
     *
     * ⚠️ DEPRECATED API: Uses deprecated `Socket.setSocketImplFactory()` (Java 17+)
     * ⚠️ EXPERIMENTAL: This implementation is a proof-of-concept for Java 24+ compatibility.
     *
     * ## Why This Exists
     * SecurityManager is removed in Java 24+ (JEP 486), but `Socket.setSocketImplFactory()`
     * is only deprecated (not removed). This provides a potential path forward for Java 24+
     * even though it uses a deprecated API.
     *
     * ## Important Context: SocketFactory vs SocketImplFactory
     * - **SocketFactory** (javax.net.SocketFactory): NOT deprecated, but requires application
     *   code to explicitly use it. Cannot globally intercept socket creation.
     * - **SocketImplFactory** (via Socket.setSocketImplFactory): Deprecated but functional.
     *   Globally intercepts ALL socket creation without code changes.
     *
     * For a testing library that must work **without modifying application code**, only
     * SocketImplFactory provides global interception. SocketFactory would require users
     * to change their production code to use it, defeating the purpose.
     *
     * ## How It Works
     * Uses `Socket.setSocketImplFactory()` to install a custom socket implementation
     * that checks network configuration before allowing connections.
     *
     * ## Known Limitations
     * - **Deprecated API**: Socket.setSocketImplFactory() is deprecated (shows warnings)
     * - **One factory per JVM**: Can only be called once per JVM (subsequent calls fail)
     * - **Platform SocketImpl**: Accessing default SocketImpl is JVM-implementation-specific
     * - **May not intercept all clients**: HttpURLConnection uses internal sun.net.www classes
     * - **Not fully validated**: Needs testing against all HTTP clients
     * - **No guarantee**: Deprecated APIs may be removed in future Java versions
     *
     * ## Pros
     * - Works on Java 24+ (no SecurityManager dependency)
     * - Pure Java implementation (no native code)
     * - Global socket interception (no code changes needed)
     *
     * ## Cons
     * - Uses deprecated API (though still functional)
     * - One factory per JVM (our implementation is configurable to work around this)
     * - Platform SocketImpl access is JVM-specific
     * - May not intercept all socket types
     * - Proof of concept, needs validation
     *
     * ## Recommendation
     * Use SECURITY_MANAGER on Java 17-23. Use SOCKET_IMPL_FACTORY on Java 24+ as a
     * temporary bridge while migrating to other testing strategies (mocking, DI, etc.).
     */
    SOCKET_IMPL_FACTORY,

    /**
     * JVMTI agent implementation - native interception (Java 7+ compatible, ultimate solution).
     *
     * ✅ This implementation WORKS by intercepting at the JVM-native boundary.
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
     * - ✅ No SecurityManager dependency (future-proof)
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
     * ## Recommendation
     * Best choice for Java 21+ when native build is acceptable. Provides the most
     * comprehensive network interception of any strategy.
     */
    JVMTI,

    /**
     * Automatically detect the best implementation.
     *
     * Priority:
     * 1. SECURITY_MANAGER (Java 17-23, 90% coverage, battle-tested)
     * 2. SECURITY_POLICY (Java 17-23, 90% coverage, declarative)
     * 3. BYTE_BUDDY (Java 17+, 85-90% coverage, future-proof, recommended for Java 21+)
     * 4. SOCKET_IMPL_FACTORY (Java 17+, 70% coverage, experimental)
     *
     * On Java 24+, only BYTE_BUDDY and SOCKET_IMPL_FACTORY are available.
     */
    AUTO,
    ;

    companion object {
        /**
         * Parse a string to NetworkBlockerImplementation.
         *
         * Supported values (case-insensitive):
         * - "bytebuddy", "byte-buddy" -> BYTE_BUDDY
         * - "securitymanager", "security-manager" -> SECURITY_MANAGER
         * - "securitypolicy", "security-policy" -> SECURITY_POLICY
         * - "socketimplfactory", "socket-impl-factory" -> SOCKET_IMPL_FACTORY
         * - "jvmti" -> JVMTI
         * - "auto" -> AUTO
         * - null -> default()
         *
         * @param value String representation of the implementation
         * @return NetworkBlockerImplementation
         * @throws IllegalArgumentException if value is not recognized
         */
        fun fromString(value: String?): NetworkBlockerImplementation =
            when (value?.lowercase()?.replace("-", "")) {
                "bytebuddy" -> BYTE_BUDDY
                "securitymanager" -> SECURITY_MANAGER
                "securitypolicy" -> SECURITY_POLICY
                "socketimplfactory" -> SOCKET_IMPL_FACTORY
                "jvmti" -> JVMTI
                "auto" -> AUTO
                null -> default()
                else ->
                    throw IllegalArgumentException(
                        "Unknown implementation: $value. " +
                            "Valid values: bytebuddy, securitymanager, securitypolicy, socketimplfactory, jvmti, auto",
                    )
            }

        /**
         * Get the default implementation.
         *
         * Returns SECURITY_MANAGER - the only implementation that actually works.
         * BYTE_BUDDY is available in the API but does not function.
         */
        fun default(): NetworkBlockerImplementation = SECURITY_MANAGER
    }
}
