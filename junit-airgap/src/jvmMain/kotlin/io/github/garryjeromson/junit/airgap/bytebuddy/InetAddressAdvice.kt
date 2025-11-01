package io.github.garryjeromson.junit.airgap.bytebuddy

import net.bytebuddy.asm.Advice

/**
 * ByteBuddy advice for InetAddress.getAllByName(String).
 *
 * This advice is injected into the method and executes BEFORE the original method body.
 * It provides a fallback for DNS interception when JVMTI native interception fails
 * (e.g., when DNS classes are loaded before the JVMTI agent completes initialization).
 */
class InetAddressGetAllByNameAdvice {
    companion object {
        /**
         * Advice method executed before InetAddress.getAllByName(String).
         *
         * @param host Hostname to resolve (null means localhost)
         * @throws NetworkRequestAttemptedException if DNS lookup is blocked
         */
        @Advice.OnMethodEnter
        @JvmStatic
        fun enter(
            @Advice.Argument(0) host: String?,
        ) {
            // Null hostname means "localhost" - always allow
            if (host == null) {
                return
            }

            // Debug logging (controlled by system property)
            if (System.getProperty("junit.airgap.debug") == "true") {
                System.err.println("[junit-airgap-bytebuddy] Intercepting getAllByName($host)")
            }

            // Check if this DNS lookup is allowed
            // Port -1 indicates this is a DNS lookup, not a socket connection
            NetworkBlockerContext.checkConnection(
                host = host,
                port = -1,
                caller = "ByteBuddy-DNS",
            )
        }
    }
}

/**
 * ByteBuddy advice for InetAddress.getByName(String).
 *
 * This method delegates to getAllByName(), but we intercept it separately
 * to catch direct calls and provide clearer stack traces.
 */
class InetAddressGetByNameAdvice {
    companion object {
        /**
         * Advice method executed before InetAddress.getByName(String).
         *
         * @param host Hostname to resolve (null means localhost)
         * @throws NetworkRequestAttemptedException if DNS lookup is blocked
         */
        @Advice.OnMethodEnter
        @JvmStatic
        fun enter(
            @Advice.Argument(0) host: String?,
        ) {
            // Null hostname means "localhost" - always allow
            if (host == null) {
                return
            }

            // Debug logging (controlled by system property)
            if (System.getProperty("junit.airgap.debug") == "true") {
                System.err.println("[junit-airgap-bytebuddy] Intercepting getByName($host)")
            }

            NetworkBlockerContext.checkConnection(
                host = host,
                port = -1,
                caller = "ByteBuddy-DNS",
            )
        }
    }
}
