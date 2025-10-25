package io.github.garryjeromson.junit.nonetwork.gradle

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * DSL extension for configuring the JUnit No-Network plugin.
 *
 * Example usage:
 * ```kotlin
 * junitNoNetwork {
 *     enabled = true
 *     applyToAllTests = true
 *     allowedHosts = listOf("localhost", "*.test.local")
 *     blockedHosts = listOf("evil.com")
 * }
 * ```
 */
abstract class JunitNoNetworkExtension {
    /**
     * Whether the plugin is enabled. Default: true
     */
    abstract val enabled: Property<Boolean>

    /**
     * Whether to apply network blocking to all tests by default.
     * When true, tests block network unless annotated with @AllowNetwork.
     * When false, tests only block when annotated with @NoNetworkTest.
     * Default: false (opt-in)
     */
    abstract val applyToAllTests: Property<Boolean>

    /**
     * Version of the junit-no-network library to use.
     * Default: matches the plugin version
     */
    abstract val libraryVersion: Property<String>

    /**
     * List of allowed host patterns (e.g., "localhost", "*.test.local").
     * Supports wildcards.
     */
    abstract val allowedHosts: ListProperty<String>

    /**
     * List of blocked host patterns (e.g., "evil.com", "*.tracking.com").
     * Blocked hosts take precedence over allowed hosts.
     */
    abstract val blockedHosts: ListProperty<String>

    /**
     * Enable debug logging for the network blocker.
     * Default: false
     */
    abstract val debug: Property<Boolean>

    /**
     * Enable automatic @Rule injection for JUnit 4 test classes via bytecode enhancement.
     * When true, the plugin will automatically inject a NoNetworkRule field into JUnit 4 test classes,
     * providing zero-configuration network blocking without requiring manual @Rule setup.
     * Default: true
     */
    abstract val injectJUnit4Rule: Property<Boolean>

    init {
        // Set default values
        enabled.convention(true)
        applyToAllTests.convention(false)
        libraryVersion.convention("0.1.0-SNAPSHOT") // Will be updated to match plugin version
        debug.convention(false)
        injectJUnit4Rule.convention(true)
    }
}
