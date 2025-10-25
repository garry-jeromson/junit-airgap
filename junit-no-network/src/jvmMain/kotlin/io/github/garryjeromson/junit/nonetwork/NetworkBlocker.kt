package io.github.garryjeromson.junit.nonetwork

/**
 * JVM implementation of NetworkBlocker using JVMTI.
 *
 * This implementation uses a JVMTI native agent to intercept network connections
 * at the JVM-native boundary, providing comprehensive coverage (95%+) across all
 * Java networking APIs.
 *
 * ## Requirements
 * - JVMTI agent must be loaded at JVM startup via `-agentpath`
 * - Gradle plugin handles this automatically for tests
 *
 * ## Coverage
 * Intercepts ALL Java network code because everything eventually calls `sun.nio.ch.Net.connect0()`:
 * - Direct Socket
 * - HttpURLConnection
 * - OkHttp, Apache HttpClient, Java 11 HttpClient
 * - Reactor Netty, Ktor
 * - Any NIO client (SocketChannel, AsynchronousSocketChannel)
 *
 * @see JvmtiNetworkBlocker
 */
actual class NetworkBlocker actual constructor(
    private val configuration: NetworkConfiguration,
) {
    private val strategy: NetworkBlockerStrategy = JvmtiNetworkBlocker(configuration)

    /**
     * Installs the network blocker using the JVMTI strategy.
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
}
