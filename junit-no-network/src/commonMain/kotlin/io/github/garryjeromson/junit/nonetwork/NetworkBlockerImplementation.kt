package io.github.garryjeromson.junit.nonetwork

/**
 * Enum representing different network blocking implementation strategies.
 *
 * ⚠️ IMPORTANT: Only SECURITY_MANAGER actually works. See individual implementations for details.
 */
enum class NetworkBlockerImplementation {
    /**
     * Byte Buddy bytecode instrumentation implementation.
     *
     * ⚠️ WARNING: This implementation DOES NOT WORK and will NOT block network requests.
     *
     * ## Why It Doesn't Work
     * Socket constructors call native code directly, which cannot be intercepted by Byte Buddy.
     * Only SecurityManager can intercept native calls.
     *
     * ## Why It Exists
     * Kept as a stub for API compatibility and experimentation.
     *
     * Cons:
     * - Does not actually block network requests
     * - Adds ~3MB dependency (Byte Buddy) for no benefit
     * - Only useful for testing that the API doesn't crash
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
     * Automatically detect the best implementation.
     *
     * Priority: SECURITY_MANAGER → SECURITY_POLICY → BYTE_BUDDY
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
                "auto" -> AUTO
                null -> default()
                else ->
                    throw IllegalArgumentException(
                        "Unknown implementation: $value. " +
                            "Valid values: bytebuddy, securitymanager, securitypolicy, auto",
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
