package io.github.garryjeromson.junit.nonetwork

/**
 * Configuration helper for the NoNetwork extension.
 * This class centralizes configuration logic for determining whether network blocking
 * should be applied by default.
 */
internal expect object ExtensionConfiguration {
    /**
     * System property key for enabling network blocking by default for all tests.
     * Set to "true" to enable: -Djunit.nonetwork.applyToAllTests=true
     */
    val APPLY_TO_ALL_TESTS_PROPERTY: String

    /**
     * Checks if network blocking should be applied to all tests based on system property.
     * This is a platform-agnostic expect function that will be implemented by each platform.
     *
     * @return true if the system property is set to "true", false otherwise
     */
    fun isApplyToAllTestsEnabled(): Boolean
}
