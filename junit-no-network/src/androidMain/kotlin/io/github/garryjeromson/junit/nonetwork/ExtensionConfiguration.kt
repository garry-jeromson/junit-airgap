package io.github.garryjeromson.junit.nonetwork

/**
 * Android implementation of ExtensionConfiguration.
 */
internal actual object ExtensionConfiguration {
    actual val APPLY_TO_ALL_TESTS_PROPERTY: String = "junit.nonetwork.applyToAllTests"

    /**
     * Checks if network blocking should be applied to all tests based on system property.
     *
     * @return true if the system property "junit.nonetwork.applyToAllTests" is set to "true", false otherwise
     */
    actual fun isApplyToAllTestsEnabled(): Boolean =
        System.getProperty(APPLY_TO_ALL_TESTS_PROPERTY, "false").toBoolean()
}
