package io.github.garryjeromson.junit.nonetwork.bytebuddy

import net.bytebuddy.asm.Advice

/**
 * ByteBuddy Advice for intercepting OkHttp connection attempts.
 *
 * Intercepts: `okhttp3.internal.connection.RealConnection.connect()`
 *
 * This is the central connection point in OkHttp where all HTTP/HTTPS connections
 * are established, regardless of the HTTP method (GET, POST, etc.).
 *
 * ## Why This Method?
 * - Called for all OkHttp connection attempts
 * - Has access to connection parameters (connectTimeout, route, etc.)
 * - Occurs before actual socket connection
 * - Single interception point covers all OkHttp usage patterns
 *
 * ## OkHttp Internal Structure
 * ```
 * OkHttpClient
 *   -> RealCall
 *     -> RealConnection.connect()  <-- We intercept here
 *       -> socket.connect()
 * ```
 */
object OkHttpAdvice {
    /**
     * Intercept before RealConnection.connect() executes.
     *
     * The connect() method signature in OkHttp 4.x:
     * ```kotlin
     * fun connect(
     *     connectTimeout: Int,
     *     readTimeout: Int,
     *     writeTimeout: Int,
     *     pingIntervalMillis: Int,
     *     connectionRetryEnabled: Boolean,
     *     call: Call,
     *     eventListener: EventListener
     * )
     * ```
     *
     * We extract the route from the RealConnection instance (via `@Advice.This`)
     * to determine the target host and port.
     *
     * @param thiz The RealConnection instance
     */
    @Advice.OnMethodEnter
    @JvmStatic
    fun enter(
        @Advice.This thiz: Any,
    ) {
        try {
            // Extract route from RealConnection
            // RealConnection has a `route` field of type Route
            // Route has an `address` field of type Address
            // Address has `url` field of type HttpUrl
            val routeField = thiz.javaClass.getDeclaredField("route")
            routeField.isAccessible = true
            val route = routeField.get(thiz)

            if (route != null) {
                // Get address from route
                val addressField = route.javaClass.getDeclaredField("address")
                addressField.isAccessible = true
                val address = addressField.get(route)

                if (address != null) {
                    // Get HttpUrl from address
                    val urlField = address.javaClass.getDeclaredField("url")
                    urlField.isAccessible = true
                    val httpUrl = urlField.get(address)

                    if (httpUrl != null) {
                        // HttpUrl has host() and port() methods
                        val hostMethod = httpUrl.javaClass.getMethod("host")
                        val portMethod = httpUrl.javaClass.getMethod("port")

                        val host = hostMethod.invoke(httpUrl) as String
                        val port = portMethod.invoke(httpUrl) as Int

                        // Check if connection should be blocked
                        NetworkBlockerContext.checkConnection(host, port, "OkHttp")
                    }
                }
            }
        } catch (e: Exception) {
            // If we fail to extract connection details, allow the connection
            // (fail open to avoid breaking OkHttp)
            if (System.getProperty("junit.nonetwork.debug") == "true") {
                println("OkHttpAdvice: Failed to extract connection details, allowing connection")
                e.printStackTrace()
            }
        }
    }
}
