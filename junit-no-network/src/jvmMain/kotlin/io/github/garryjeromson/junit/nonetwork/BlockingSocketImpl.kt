package io.github.garryjeromson.junit.nonetwork

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.SocketAddress
import java.net.SocketImpl

/**
 * Custom SocketImpl that checks network configuration before allowing connections.
 *
 * This implementation intercepts socket connection attempts and validates them
 * against the configured network policy before delegating to the platform default.
 *
 * ⚠️ EXPERIMENTAL - Partial support due to Java Module System (JPMS) restrictions.
 *
 * ## How It Works
 * 1. Socket.setSocketImplFactory() installs our custom factory
 * 2. All Socket instances use this SocketImpl
 * 3. connect() methods check configuration before connecting
 * 4. Blocked connections throw NetworkRequestAttemptedException
 * 5. Allowed connections delegate to platform default SocketImpl
 *
 * ## Critical Issue: Platform Delegate Unavailable
 * **JPMS Restriction**: Cannot obtain `sun.nio.ch.NioSocketImpl` via reflection:
 * ```
 * InaccessibleObjectException: module java.base does not "exports sun.nio.ch"
 * ```
 *
 * **Impact**:
 * - `platformDelegate` will be `null` in most cases
 * - `create()` fails silently when delegate is null
 * - `connect()` is never called on broken sockets
 * - Result: Connections neither blocked nor allowed - they just fail
 *
 * ## What Actually Works
 * - ✅ **HttpURLConnection**: Somehow tolerates missing delegate
 * - ✅ **Simple Socket operations**: Basic connect/disconnect
 * - ❌ **Modern HTTP clients**: Fail due to incomplete Socket initialization
 * - ❌ **NIO-based clients**: Bypass Socket entirely
 *
 * ## Why Tests Pass Without Delegate
 * Some Socket operations have fallback behavior when `create()` fails:
 * - HttpURLConnection uses internal error handling
 * - Simple Socket usage may have retry logic
 * - But most modern clients expect fully-functional Sockets
 *
 * ## Limitations
 * - **Cannot obtain platform delegate**: JPMS prevents reflection
 * - **Cannot intercept already-connected sockets**: Too late
 * - **Cannot intercept NIO channels**: Different API path
 * - **May not work with all socket types**: DatagramSocket, ServerSocket need separate factories
 *
 * ## See Also
 * - `SocketImplFactoryNetworkBlocker` for factory installation and full limitations
 */
internal class BlockingSocketImpl(
    private val configuration: NetworkConfiguration,
    private val platformDelegate: SocketImpl?,
) : SocketImpl() {
    /**
     * Check if a connection to the given host should be allowed.
     *
     * @param host hostname or IP address
     * @param port port number
     * @throws NetworkRequestAttemptedException if the connection is blocked
     */
    private fun checkConnection(
        host: String,
        port: Int,
    ) {
        // Debug logging
        if (System.getProperty("junit.nonetwork.debug") == "true") {
            println("BlockingSocketImpl.checkConnection: host=$host, port=$port")
            println(
                "  Configuration: allowedHosts=${configuration.allowedHosts}, blockedHosts=${configuration.blockedHosts}",
            )
            println("  isAllowed($host) = ${configuration.isAllowed(host)}")
        }

        // Check configuration - blocked hosts take precedence over allowed hosts
        if (!configuration.isAllowed(host)) {
            val details =
                NetworkRequestDetails(
                    host = host,
                    port = port,
                    url = "$host:$port",
                    stackTrace =
                        Thread
                            .currentThread()
                            .stackTrace
                            .drop(1) // Skip checkConnection
                            .take(10) // Take top 10 frames
                            .joinToString("\n") { "  at $it" },
                )

            throw NetworkRequestAttemptedException(
                "Network request blocked by @BlockNetworkRequests: Attempted to connect to $host:$port",
                requestDetails = details,
            )
        }
    }

    /**
     * Use reflection to call a protected method on the platform delegate.
     * Searches through all methods to find a match by name and parameter count.
     */
    private fun <T> invokeProtected(
        methodName: String,
        vararg args: Any?,
    ): T? =
        try {
            platformDelegate?.let { delegate ->
                // Find method by name and parameter count (not exact types to handle primitives)
                val method =
                    delegate.javaClass.declaredMethods.find { method ->
                        method.name == methodName && method.parameterCount == args.size
                    } ?: throw NoSuchMethodException("$methodName with ${args.size} parameters")

                method.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                method.invoke(delegate, *args) as? T
            }
        } catch (e: Exception) {
            throw IOException("Failed to invoke $methodName on platform SocketImpl", e)
        }

    override fun create(stream: Boolean) {
        // Debug logging
        if (System.getProperty("junit.nonetwork.debug") == "true") {
            println("BlockingSocketImpl.create(stream=$stream) - platformDelegate=$platformDelegate")
        }

        // Delegate to platform impl if available
        invokeProtected<Unit>("create", stream)
    }

    override fun connect(
        host: String,
        port: Int,
    ) {
        // Debug logging
        if (System.getProperty("junit.nonetwork.debug") == "true") {
            println("BlockingSocketImpl.connect(host=$host, port=$port) called")
        }

        // Check if connection is allowed
        checkConnection(host, port)

        // Delegate to platform implementation
        invokeProtected<Unit>("connect", host, port)
            ?: throw IOException("Platform SocketImpl not available for connect(String, int)")
    }

    override fun connect(
        address: InetAddress,
        port: Int,
    ) {
        // Debug logging
        if (System.getProperty("junit.nonetwork.debug") == "true") {
            println("BlockingSocketImpl.connect(address=${address.hostAddress}, port=$port) called")
        }

        // Check if connection is allowed
        checkConnection(address.hostAddress ?: address.hostName, port)

        // Delegate to platform implementation
        invokeProtected<Unit>("connect", address, port)
            ?: throw IOException("Platform SocketImpl not available for connect(InetAddress, int)")
    }

    override fun connect(
        address: SocketAddress,
        timeout: Int,
    ) {
        // Debug logging
        if (System.getProperty("junit.nonetwork.debug") == "true") {
            println("BlockingSocketImpl.connect(address=$address, timeout=$timeout) called")
        }

        // Extract host and port from SocketAddress if it's an InetSocketAddress
        if (address is java.net.InetSocketAddress) {
            val host = address.hostString ?: address.address?.hostAddress ?: address.hostName
            checkConnection(host, address.port)
        }

        // Delegate to platform implementation
        // Note: We check in other connect methods, this is a fallback
        invokeProtected<Unit>("connect", address, timeout)
            ?: throw IOException("Platform SocketImpl not available for connect(SocketAddress, int)")
    }

    override fun bind(
        host: InetAddress,
        port: Int,
    ) {
        invokeProtected<Unit>("bind", host, port)
            ?: throw IOException("Platform SocketImpl not available for bind")
    }

    override fun listen(backlog: Int) {
        invokeProtected<Unit>("listen", backlog)
            ?: throw IOException("Platform SocketImpl not available for listen")
    }

    override fun accept(s: SocketImpl) {
        invokeProtected<Unit>("accept", s)
            ?: throw IOException("Platform SocketImpl not available for accept")
    }

    override fun getInputStream(): InputStream =
        invokeProtected<InputStream>("getInputStream")
            ?: throw IOException("Platform SocketImpl not available for getInputStream")

    override fun getOutputStream(): OutputStream =
        invokeProtected<OutputStream>("getOutputStream")
            ?: throw IOException("Platform SocketImpl not available for getOutputStream")

    override fun available(): Int = invokeProtected<Int>("available") ?: 0

    override fun close() {
        invokeProtected<Unit>("close")
    }

    override fun sendUrgentData(data: Int) {
        invokeProtected<Unit>("sendUrgentData", data)
            ?: throw IOException("Platform SocketImpl not available for sendUrgentData")
    }

    // SocketOptions delegation
    override fun setOption(
        optID: Int,
        value: Any,
    ) {
        invokeProtected<Unit>("setOption", optID, value)
            ?: throw IOException("Platform SocketImpl not available for setOption")
    }

    override fun getOption(optID: Int): Any =
        invokeProtected<Any>("getOption", optID)
            ?: throw IOException("Platform SocketImpl not available for getOption")
}
