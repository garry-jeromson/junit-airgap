@file:JvmName("ExtensionConfigurationJvmKt")

package io.github.garryjeromson.junit.airgap

/**
 * JVM implementation of getSystemProperty.
 * Delegates to Java's System.getProperty().
 */
internal actual fun getSystemProperty(
    key: String,
    defaultValue: String,
): String = System.getProperty(key, defaultValue)
