package io.github.garryjeromson.junit.nonetwork

/**
 * Configuration helper for the NoNetwork extension.
 * This class centralizes configuration logic for determining whether network blocking
 * should be applied by default and which hosts should be allowed/blocked.
 */
internal expect object ExtensionConfiguration {
    /**
     * System property key for enabling network blocking by default for all tests.
     * Set to "true" to enable: -Djunit.nonetwork.applyToAllTests=true
     */
    val APPLY_TO_ALL_TESTS_PROPERTY: String

    /**
     * System property key for configuring globally allowed hosts.
     * Comma-separated list: -Djunit.nonetwork.allowedHosts=localhost,127.0.0.1,*.local
     */
    val ALLOWED_HOSTS_PROPERTY: String

    /**
     * System property key for configuring globally blocked hosts.
     * Comma-separated list: -Djunit.nonetwork.blockedHosts=evil.com,*.tracking.com
     */
    val BLOCKED_HOSTS_PROPERTY: String

    /**
     * Checks if network blocking should be applied to all tests based on system property.
     * This is a platform-agnostic expect function that will be implemented by each platform.
     *
     * @return true if the system property is set to "true", false otherwise
     */
    fun isApplyToAllTestsEnabled(): Boolean

    /**
     * Retrieves the list of globally allowed hosts from system property.
     *
     * @return Set of allowed host patterns (e.g., "localhost", "*.local"), empty if not configured
     */
    fun getAllowedHosts(): Set<String>

    /**
     * Retrieves the list of globally blocked hosts from system property.
     *
     * @return Set of blocked host patterns (e.g., "evil.com", "*.tracking.com"), empty if not configured
     */
    fun getBlockedHosts(): Set<String>
}
