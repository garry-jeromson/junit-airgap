package io.github.garryjeromson.junit.nonetwork

/**
 * JVM implementation of NetworkBlocker that supports multiple blocking strategies.
 *
 * Supported implementations:
 * - SECURITY_MANAGER: ✅ Java 17-23 (90% coverage, DEFAULT, deprecated, removed in Java 24+)
 * - SECURITY_POLICY: ✅ Java 17-23 (90% coverage, declarative, removed in Java 24+)
 * - BYTE_BUDDY: ✅ Java 17+ (85-90% coverage, future-proof, recommended for Java 21+)
 * - SOCKET_IMPL_FACTORY: ⚠️ Java 17+ (70% coverage, experimental)
 * - AUTO: Selects best available implementation based on Java version
 *
 * Implementation can be selected via:
 * - System property: junit.nonetwork.implementation
 * - Environment variable: JUNIT_NONETWORK_IMPLEMENTATION
 *
 * Valid values: bytebuddy, securitymanager, securitypolicy, socketimplfactory, auto
 */
actual class NetworkBlocker actual constructor(
    private val configuration: NetworkConfiguration,
) {
    private val strategy: NetworkBlockerStrategy = selectStrategy(configuration)

    /**
     * Installs the network blocker using the selected strategy.
     * After installation, all socket connections will be checked against the configuration.
     */
    @Synchronized
    actual fun install() {
        strategy.install()
    }

    /**
     * Uninstalls the network blocker and restores normal network behavior.
     * After uninstallation, network requests will work normally again.
     */
    @Synchronized
    actual fun uninstall() {
        strategy.uninstall()
    }

    companion object {
        private const val SYSTEM_PROPERTY = "junit.nonetwork.implementation"
        private const val ENV_VAR = "JUNIT_NONETWORK_IMPLEMENTATION"
        private const val DEBUG_PROPERTY = "junit.nonetwork.debug"

        /**
         * Select the appropriate blocking strategy based on configuration.
         *
         * Priority:
         * 1. System property: junit.nonetwork.implementation
         * 2. Environment variable: JUNIT_NONETWORK_IMPLEMENTATION
         * 3. Default: SECURITY_MANAGER (most reliable implementation)
         *
         * In AUTO mode (Java version-dependent selection):
         * 1. SECURITY_MANAGER (Java 17-23, 90% coverage, battle-tested)
         * 2. SECURITY_POLICY (Java 17-23, 90% coverage, declarative)
         * 3. BYTE_BUDDY (Java 17+, 85-90% coverage, future-proof)
         * 4. SOCKET_IMPL_FACTORY (Java 17+, 70% coverage, experimental)
         *
         * On Java 24+, only BYTE_BUDDY and SOCKET_IMPL_FACTORY are available.
         */
        private fun selectStrategy(configuration: NetworkConfiguration): NetworkBlockerStrategy {
            // Read implementation preference from system property or env var
            val implementationStr =
                System.getProperty(SYSTEM_PROPERTY)
                    ?: System.getenv(ENV_VAR)

            val requestedImpl = NetworkBlockerImplementation.fromString(implementationStr)
            val debug = System.getProperty(DEBUG_PROPERTY)?.toBoolean() == true

            return when (requestedImpl) {
                NetworkBlockerImplementation.BYTE_BUDDY -> {
                    val strategy = ByteBuddyNetworkBlocker(configuration)
                    if (!strategy.isAvailable()) {
                        throw IllegalStateException(
                            "BYTE_BUDDY implementation requested but ByteBuddy is not available. " +
                                "Add net.bytebuddy:byte-buddy and net.bytebuddy:byte-buddy-agent to your dependencies.",
                        )
                    }
                    if (debug) println("NetworkBlocker: Using BYTE_BUDDY implementation (85-90% coverage)")
                    strategy
                }

                NetworkBlockerImplementation.SECURITY_MANAGER -> {
                    val strategy = SecurityManagerNetworkBlocker(configuration)
                    if (!strategy.isAvailable()) {
                        throw IllegalStateException(
                            "SECURITY_MANAGER implementation requested but SecurityManager is not available. " +
                                "This may occur on Java 24+ where SecurityManager has been removed.",
                        )
                    }
                    if (debug) println("NetworkBlocker: Using SECURITY_MANAGER implementation")
                    strategy
                }

                NetworkBlockerImplementation.SECURITY_POLICY -> {
                    val strategy = SecurityPolicyNetworkBlocker(configuration)
                    if (!strategy.isAvailable()) {
                        throw IllegalStateException(
                            "SECURITY_POLICY implementation requested but Policy/SecurityManager is not available. " +
                                "This may occur on Java 24+ where SecurityManager has been removed.",
                        )
                    }
                    if (debug) println("NetworkBlocker: Using SECURITY_POLICY implementation")
                    strategy
                }

                NetworkBlockerImplementation.SOCKET_IMPL_FACTORY -> {
                    val strategy = SocketImplFactoryNetworkBlocker(configuration)
                    if (!strategy.isAvailable()) {
                        throw IllegalStateException(
                            "SOCKET_IMPL_FACTORY implementation requested but SocketImplFactory is not available. " +
                                "This may occur if another library has already installed a SocketImplFactory.",
                        )
                    }
                    if (debug) println("NetworkBlocker: Using SOCKET_IMPL_FACTORY implementation (experimental)")
                    strategy
                }

                NetworkBlockerImplementation.JVMTI -> {
                    val strategy = JvmtiNetworkBlocker(configuration)
                    if (!strategy.isAvailable()) {
                        throw IllegalStateException(
                            "JVMTI implementation requested but JVMTI agent is not loaded. " +
                                "Ensure the agent is loaded at JVM startup via -agentpath:/path/to/libjunit-no-network-agent.dylib",
                        )
                    }
                    if (debug) println("NetworkBlocker: Using JVMTI implementation")
                    strategy
                }

                NetworkBlockerImplementation.AUTO -> {
                    // Try SecurityManager first (battle-tested, 90% coverage)
                    val securityManagerStrategy = SecurityManagerNetworkBlocker(configuration)
                    if (securityManagerStrategy.isAvailable()) {
                        if (debug) println("NetworkBlocker: AUTO mode selected SECURITY_MANAGER (90% coverage)")
                        return securityManagerStrategy
                    }

                    // Try Security Policy second (declarative, 90% coverage)
                    val securityPolicyStrategy = SecurityPolicyNetworkBlocker(configuration)
                    if (securityPolicyStrategy.isAvailable()) {
                        if (debug) println("NetworkBlocker: AUTO mode selected SECURITY_POLICY (90% coverage)")
                        return securityPolicyStrategy
                    }

                    // Try ByteBuddy third (future-proof, 85-90% coverage)
                    val byteBuddyStrategy = ByteBuddyNetworkBlocker(configuration)
                    if (byteBuddyStrategy.isAvailable()) {
                        if (debug) {
                            println(
                                "NetworkBlocker: AUTO mode selected BYTE_BUDDY (85-90% coverage, future-proof)",
                            )
                        }
                        return byteBuddyStrategy
                    }

                    // Try SocketImplFactory fourth (Java 24+ compatible, 70% coverage)
                    val socketImplFactoryStrategy = SocketImplFactoryNetworkBlocker(configuration)
                    if (socketImplFactoryStrategy.isAvailable()) {
                        if (debug) {
                            println(
                                "NetworkBlocker: AUTO mode selected SOCKET_IMPL_FACTORY (70% coverage, experimental)",
                            )
                        }
                        return socketImplFactoryStrategy
                    }

                    // No implementation is available
                    throw IllegalStateException(
                        "No network blocking implementation is available. " +
                            "Ensure SecurityManager is available, or add ByteBuddy dependencies (net.bytebuddy:byte-buddy, net.bytebuddy:byte-buddy-agent).",
                    )
                }
            }
        }
    }
}
