package io.github.garryjeromson.junit.nonetwork

/**
 * Configuration for network request blocking behavior.
 *
 * @param allowedHosts Set of host names or patterns that are allowed. Use "*" to allow all hosts.
 *                     Patterns support wildcards (e.g., "*.example.com").
 * @param blockedHosts Set of host names or patterns that are blocked. Blocked hosts take precedence
 *                     over allowed hosts.
 */
data class NetworkConfiguration(
    val allowedHosts: Set<String> = emptySet(),
    val blockedHosts: Set<String> = emptySet(),
) {
    /**
     * Generation counter to invalidate stale configurations in inherited threads.
     * Used internally to handle InheritableThreadLocal persistence across test methods.
     *
     * This is not part of the primary constructor to exclude it from equals(), hashCode(),
     * copy(), and other data class generated methods.
     */
    var generation: Long = 0
        internal set
    /**
     * Checks if a given host is allowed based on the configuration.
     *
     * @param host The host name to check
     * @return true if the host is allowed, false if it's blocked or not in the allow list
     */
    fun isAllowed(host: String): Boolean {
        val normalizedHost = host.lowercase()

        // Check if host is explicitly blocked (blocked hosts take precedence)
        if (matchesAnyPattern(normalizedHost, blockedHosts)) {
            return false
        }

        // Check if host is in allowed list
        if (allowedHosts.isEmpty()) {
            // No allowed hosts means block everything
            return false
        }

        return matchesAnyPattern(normalizedHost, allowedHosts)
    }

    /**
     * Merges this configuration with another configuration.
     * The resulting configuration will have combined allowed and blocked hosts.
     *
     * @param other The other configuration to merge with
     * @return A new NetworkConfiguration with combined settings
     */
    fun merge(other: NetworkConfiguration): NetworkConfiguration =
        NetworkConfiguration(
            allowedHosts = this.allowedHosts + other.allowedHosts,
            blockedHosts = this.blockedHosts + other.blockedHosts,
        )

    private fun matchesAnyPattern(
        host: String,
        patterns: Set<String>,
    ): Boolean =
        patterns.any { pattern ->
            matchesPattern(host, pattern.lowercase())
        }

    private fun matchesPattern(
        host: String,
        pattern: String,
    ): Boolean {
        // Handle wildcard for "allow all"
        if (pattern == "*") {
            return true
        }

        // Convert wildcard pattern to regex
        // Escape special regex characters except *
        val regexPattern =
            pattern
                .replace(".", "\\.")
                .replace("*", ".*")

        return host.matches(Regex("^$regexPattern$"))
    }

    companion object {
        /**
         * Creates a configuration from annotations on a test method or class.
         *
         * @param annotations Collection of annotations to process
         * @return NetworkConfiguration based on the annotations
         */
        fun fromAnnotations(annotations: Collection<Annotation>): NetworkConfiguration {
            val allowedHosts =
                annotations
                    .filterIsInstance<AllowRequestsToHosts>()
                    .flatMap { it.hosts.toList() }
                    .toSet()

            val blockedHosts =
                annotations
                    .filterIsInstance<BlockRequestsToHosts>()
                    .flatMap { it.hosts.toList() }
                    .toSet()

            return NetworkConfiguration(
                allowedHosts = allowedHosts,
                blockedHosts = blockedHosts,
            )
        }
    }
}
