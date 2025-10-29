package io.github.garryjeromson.junit.airgap

/**
 * System property key for enabling/disabling the plugin entirely.
 * Set to "false" to disable: -Djunit.airgap.enabled=false
 */
internal const val ENABLED_PROPERTY: String = "junit.airgap.enabled"

/**
 * System property key for enabling network blocking by default for all tests.
 * Set to "true" to enable: -Djunit.airgap.applyToAllTests=true
 */
internal const val APPLY_TO_ALL_TESTS_PROPERTY: String = "junit.airgap.applyToAllTests"

/**
 * System property key for configuring globally allowed hosts.
 * Comma-separated list: -Djunit.airgap.allowedHosts=localhost,127.0.0.1,*.local
 */
internal const val ALLOWED_HOSTS_PROPERTY: String = "junit.airgap.allowedHosts"

/**
 * System property key for configuring globally blocked hosts.
 * Comma-separated list: -Djunit.airgap.blockedHosts=evil.com,*.tracking.com
 */
internal const val BLOCKED_HOSTS_PROPERTY: String = "junit.airgap.blockedHosts"

/**
 * Platform-specific system property accessor.
 * On JVM/Android: delegates to System.getProperty()
 * On iOS: returns defaultValue (system properties not available)
 *
 * @param key The property key to read
 * @param defaultValue The default value if property is not set
 * @return The property value or defaultValue
 */
internal expect fun getSystemProperty(
    key: String,
    defaultValue: String = "",
): String

/**
 * Configuration helper for the NoNetwork extension.
 * This object centralizes configuration logic for determining whether network blocking
 * should be applied by default and which hosts should be allowed/blocked.
 *
 * All parsing logic is in commonMain, with only system property reading delegated to platforms.
 */
internal object ExtensionConfiguration {
    /**
     * Checks if network blocking should be applied to all tests based on system property.
     *
     * @return true if the system property is set to "true", false otherwise
     */
    fun isApplyToAllTestsEnabled(): Boolean = getSystemProperty(APPLY_TO_ALL_TESTS_PROPERTY, "false").toBoolean()

    /**
     * Retrieves the list of globally allowed hosts from system property.
     *
     * @return Set of allowed host patterns (e.g., "localhost", "*.local"), empty if not configured
     */
    fun getAllowedHosts(): Set<String> = parseHostList(getSystemProperty(ALLOWED_HOSTS_PROPERTY))

    /**
     * Retrieves the list of globally blocked hosts from system property.
     *
     * @return Set of blocked host patterns (e.g., "evil.com", "*.tracking.com"), empty if not configured
     */
    fun getBlockedHosts(): Set<String> = parseHostList(getSystemProperty(BLOCKED_HOSTS_PROPERTY))

    /**
     * Parses a comma-separated list of host patterns.
     *
     * @param hostsStr Comma-separated host patterns
     * @return Set of trimmed, non-empty host patterns
     */
    private fun parseHostList(hostsStr: String): Set<String> =
        hostsStr
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
}
