package io.github.garryjeromson.junit.nonetwork

/**
 * Android implementation of ExtensionConfiguration.
 */
internal actual object ExtensionConfiguration {
    actual val APPLY_TO_ALL_TESTS_PROPERTY: String = "junit.nonetwork.applyToAllTests"
    actual val ALLOWED_HOSTS_PROPERTY: String = "junit.nonetwork.allowedHosts"
    actual val BLOCKED_HOSTS_PROPERTY: String = "junit.nonetwork.blockedHosts"

    /**
     * Checks if network blocking should be applied to all tests based on system property.
     *
     * @return true if the system property "junit.nonetwork.applyToAllTests" is set to "true", false otherwise
     */
    actual fun isApplyToAllTestsEnabled(): Boolean =
        System.getProperty(APPLY_TO_ALL_TESTS_PROPERTY, "false").toBoolean()

    /**
     * Retrieves the list of globally allowed hosts from system property.
     *
     * @return Set of allowed host patterns, empty if not configured
     */
    actual fun getAllowedHosts(): Set<String> {
        val property = System.getProperty(ALLOWED_HOSTS_PROPERTY) ?: return emptySet()
        return property
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /**
     * Retrieves the list of globally blocked hosts from system property.
     *
     * @return Set of blocked host patterns, empty if not configured
     */
    actual fun getBlockedHosts(): Set<String> {
        val property = System.getProperty(BLOCKED_HOSTS_PROPERTY) ?: return emptySet()
        return property
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
