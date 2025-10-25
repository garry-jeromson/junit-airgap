package io.github.garryjeromson.junit.nonetwork

/**
 * JVM implementation of NetworkBlocker that supports multiple blocking strategies.
 *
 * ⚠️ IMPORTANT: SECURITY_MANAGER and SECURITY_POLICY work. SOCKET_IMPL_FACTORY is experimental. BYTE_BUDDY is a non-functional stub.
 *
 * Supported implementations:
 * - SECURITY_MANAGER: ✅ Works reliably (DEFAULT, deprecated in Java 17+, removed in Java 24+)
 * - SECURITY_POLICY: ✅ Works reliably (declarative approach, deprecated in Java 17+, removed in Java 24+)
 * - SOCKET_IMPL_FACTORY: ⚠️ Experimental (Java 24+ compatible proof-of-concept)
 * - BYTE_BUDDY: ❌ Does not work (stub kept for API compatibility only)
 * - AUTO: Selects SECURITY_MANAGER, SECURITY_POLICY, or SOCKET_IMPL_FACTORY
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
         * In AUTO mode:
         * - Prefers SECURITY_MANAGER (battle-tested, reliable)
         * - Falls back to SECURITY_POLICY (declarative approach)
         * - Falls back to SOCKET_IMPL_FACTORY (Java 24+ compatible, experimental)
         * - Falls back to BYTE_BUDDY only if all others unavailable
         * - BYTE_BUDDY is a stub and will not block requests
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
                            "BYTE_BUDDY implementation requested but Byte Buddy is not available. " +
                                "Add net.bytebuddy:byte-buddy and net.bytebuddy:byte-buddy-agent to your dependencies.",
                        )
                    }
                    if (debug) {
                        System.err.println(
                            "WARNING: BYTE_BUDDY implementation selected but it does NOT work. " +
                                "Use SECURITY_MANAGER or SECURITY_POLICY for actual network blocking.",
                        )
                    }
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

                NetworkBlockerImplementation.AUTO -> {
                    // Try SecurityManager first (battle-tested, reliable)
                    val securityManagerStrategy = SecurityManagerNetworkBlocker(configuration)
                    if (securityManagerStrategy.isAvailable()) {
                        if (debug) println("NetworkBlocker: AUTO mode selected SECURITY_MANAGER implementation")
                        return securityManagerStrategy
                    }

                    // Try Security Policy second
                    val securityPolicyStrategy = SecurityPolicyNetworkBlocker(configuration)
                    if (securityPolicyStrategy.isAvailable()) {
                        if (debug) println("NetworkBlocker: AUTO mode selected SECURITY_POLICY implementation")
                        return securityPolicyStrategy
                    }

                    // Try SocketImplFactory third (Java 24+ compatible)
                    val socketImplFactoryStrategy = SocketImplFactoryNetworkBlocker(configuration)
                    if (socketImplFactoryStrategy.isAvailable()) {
                        if (debug) {
                            println(
                                "NetworkBlocker: AUTO mode selected SOCKET_IMPL_FACTORY implementation (experimental)",
                            )
                        }
                        return socketImplFactoryStrategy
                    }

                    // Fall back to Byte Buddy
                    val byteBuddyStrategy = ByteBuddyNetworkBlocker(configuration)
                    if (byteBuddyStrategy.isAvailable()) {
                        if (debug) {
                            println(
                                "NetworkBlocker: AUTO mode selected BYTE_BUDDY implementation (fallback, non-functional)",
                            )
                        }
                        return byteBuddyStrategy
                    }

                    // No implementation is available
                    throw IllegalStateException(
                        "No network blocking implementation is available. " +
                            "Either ensure SecurityManager is available, or add Byte Buddy dependencies, or check if SocketImplFactory is already installed.",
                    )
                }
            }
        }
    }
}
