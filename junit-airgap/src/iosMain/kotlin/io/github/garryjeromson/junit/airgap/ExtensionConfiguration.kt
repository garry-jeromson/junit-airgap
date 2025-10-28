package io.github.garryjeromson.junit.airgap

/**
 * iOS implementation of getSystemProperty.
 *
 * Note: iOS doesn't have a direct equivalent of Java system properties,
 * so this implementation always returns the defaultValue. iOS network blocking is
 * not fully implemented (see NetworkBlocker documentation).
 */
internal actual fun getSystemProperty(
    key: String,
    defaultValue: String,
): String = defaultValue
