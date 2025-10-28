@file:JvmName("ExtensionConfigurationAndroidKt")

package io.github.garryjeromson.junit.airgap

/**
 * Android implementation of getSystemProperty.
 * Delegates to Java's System.getProperty().
 */
internal actual fun getSystemProperty(
    key: String,
    defaultValue: String,
): String = System.getProperty(key) ?: defaultValue
