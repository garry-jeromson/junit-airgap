package io.github.garryjeromson.junit.airgap.bytebuddy

/**
 * Documentation for ByteBuddy-based DNS interception.
 *
 * This file documents the ByteBuddy DNS interception strategy.
 * The actual implementation is in:
 * - InetAddressBytebuddyAgent.kt - Java agent entry point
 * - InetAddressAdvice.kt - ByteBuddy advice classes
 *
 * ## Why ByteBuddy?
 *
 * ByteBuddy provides a fallback for DNS interception when JVMTI native interception fails:
 *
 * 1. JVMTI native method binding only works for methods bound AFTER the agent loads
 * 2. In some JVM configurations (esp. IDE test runners), DNS classes are loaded
 *    BEFORE the JVMTI agent finishes initializing
 * 3. Once a native method is bound, JVMTI cannot retroactively intercept it
 *
 * By intercepting at the Java layer, we catch ALL DNS lookups regardless of when
 * the native methods were bound.
 *
 * ## Interception Points
 *
 * We intercept:
 * - `InetAddress.getAllByName(String)` - Primary DNS lookup method
 * - `InetAddress.getByName(String)` - Convenience method (calls getAllByName)
 *
 * ## Why Not Intercept Native Methods?
 *
 * We could try to intercept `Inet6AddressImpl.lookupAllHostAddr()` and
 * `Inet4AddressImpl.lookupAllHostAddr()`, but these are:
 * 1. Private implementation details (may change between Java versions)
 * 2. Already bound by the time we can inject ByteBuddy advice
 * 3. Harder to intercept with ByteBuddy (private, internal classes)
 *
 * Intercepting the public API is more reliable and portable.
 *
 * ## Two-Layer Architecture
 *
 * ```
 * User Code
 *   ↓
 * InetAddress.getAllByName()
 *   ↓
 * ByteBuddy Advice (Layer 1) ← Always works
 *   ↓
 * Inet6AddressImpl.lookupAllHostAddr() (native)
 *   ↓
 * JVMTI Wrapper (Layer 2) ← Fast path when DNS classes loaded after agent
 *   ↓
 * Original Native Implementation
 * ```
 *
 * Both layers call NetworkBlockerContext.checkConnection() with the same logic.
 *
 * @see InetAddressBytebuddyAgent
 * @see InetAddressAdvice
 * @see io.github.garryjeromson.junit.airgap.bytebuddy.NetworkBlockerContext
 */
object InetAddressInterceptor {
    // Implementation moved to InetAddressAdvice.kt
    // This object is kept for documentation purposes only
}
