package io.github.garryjeromson.junit.nonetwork

/**
 * iOS implementation of ExtensionConfiguration.
 *
 * Note: iOS doesn't have a direct equivalent of Java system properties,
 * so this implementation always returns false. iOS network blocking is
 * not fully implemented (see NetworkBlocker documentation).
 */
internal actual object ExtensionConfiguration {
    actual val APPLY_TO_ALL_TESTS_PROPERTY: String = "junit.nonetwork.applyToAllTests"

    /**
     * Always returns false on iOS as system properties are not available.
     *
     * @return false
     */
    actual fun isApplyToAllTestsEnabled(): Boolean = false
}
