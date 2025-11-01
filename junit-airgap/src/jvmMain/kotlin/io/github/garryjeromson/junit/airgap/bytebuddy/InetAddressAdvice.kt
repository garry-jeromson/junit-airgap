package io.github.garryjeromson.junit.airgap.bytebuddy

import net.bytebuddy.asm.Advice

/**
 * ByteBuddy advice for InetAddress.getAllByName(String).
 *
 * This advice is injected into the method and executes BEFORE the original method body.
 * It provides a fallback for DNS interception when JVMTI native interception fails
 * (e.g., when DNS classes are loaded before the JVMTI agent completes initialization).
 *
 * Uses reflection to avoid classloader conflicts when ByteBuddy calls this method
 * from bootstrap classes like InetAddress.
 */
object InetAddressGetAllByNameAdvice {
    /**
     * Advice method executed before InetAddress.getAllByName(String).
     *
     * Uses reflection to find NetworkBlockerContext. This code will be inlined
     * into InetAddress by ByteBuddy, so it must only use JDK classes.
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
        if (host != null) {
            try {
                // Use reflection - only JDK classes, safe to inline into bootstrap class
                val contextClass = Class.forName("io.github.garryjeromson.junit.airgap.bytebuddy.NetworkBlockerContext")
                val checkConnectionMethod =
                    contextClass.getDeclaredMethod(
                        "checkConnection",
                        String::class.java,
                        Integer.TYPE,
                        String::class.java,
                    )
                checkConnectionMethod.invoke(null, host, Integer.valueOf(-1), "ByteBuddy-DNS")
            } catch (e: ClassNotFoundException) {
                // NetworkBlockerContext not available - allow request
            } catch (e: NoSuchMethodException) {
                // Method not found - allow request
            } catch (e: java.lang.reflect.InvocationTargetException) {
                // Rethrow the wrapped exception
                val cause = e.cause
                if (cause is RuntimeException) {
                    throw cause
                } else if (cause is Error) {
                    throw cause
                }
            }
        }
    }
}

/**
 * ByteBuddy advice for InetAddress.getByName(String).
 *
 * This method delegates to getAllByName(), but we intercept it separately
 * to catch direct calls and provide clearer stack traces.
 *
 * Uses reflection to avoid classloader conflicts when ByteBuddy calls this method
 * from bootstrap classes like InetAddress.
 */
object InetAddressGetByNameAdvice {
    /**
     * Advice method executed before InetAddress.getByName(String).
     *
     * Uses reflection to find NetworkBlockerContext. This code will be inlined
     * into InetAddress by ByteBuddy, so it must only use JDK classes.
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
        if (host != null) {
            try {
                // Use reflection - only JDK classes, safe to inline into bootstrap class
                val contextClass = Class.forName("io.github.garryjeromson.junit.airgap.bytebuddy.NetworkBlockerContext")
                val checkConnectionMethod =
                    contextClass.getDeclaredMethod(
                        "checkConnection",
                        String::class.java,
                        Integer.TYPE,
                        String::class.java,
                    )
                checkConnectionMethod.invoke(null, host, Integer.valueOf(-1), "ByteBuddy-DNS")
            } catch (e: ClassNotFoundException) {
                // NetworkBlockerContext not available - allow request
            } catch (e: NoSuchMethodException) {
                // Method not found - allow request
            } catch (e: java.lang.reflect.InvocationTargetException) {
                // Rethrow the wrapped exception
                val cause = e.cause
                if (cause is RuntimeException) {
                    throw cause
                } else if (cause is Error) {
                    throw cause
                }
            }
        }
    }
}
