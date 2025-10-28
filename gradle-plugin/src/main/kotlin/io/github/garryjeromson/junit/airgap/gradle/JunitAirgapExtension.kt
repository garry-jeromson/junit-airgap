package io.github.garryjeromson.junit.airgap.gradle

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * DSL extension for configuring the JUnit Airgap plugin.
 *
 * Example usage:
 * ```kotlin
 * junitAirgap {
 *     enabled = true
 *     applyToAllTests = true
 *     allowedHosts = listOf("localhost", "*.test.local")
 *     blockedHosts = listOf("evil.com")
 * }
 * ```
 */
abstract class JunitAirgapExtension {
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
     * Version of the junit-airgaplibrary to use.
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
     * When true, the plugin will automatically inject a AirgapRule field into JUnit 4 test classes,
     * providing zero-configuration network blocking without requiring manual @Rule setup.
     *
     * When not set, the plugin will auto-detect JUnit 4 projects by checking:
     * - If test tasks use useJUnitPlatform() (JUnit 5 indicator)
     * - If junit:junit:4.x dependency is present (JUnit 4 indicator)
     *
     * Set explicitly to override auto-detection:
     * - true: Force enable injection (for edge cases)
     * - false: Force disable injection (if auto-detection incorrect)
     * - Not set: Auto-detect (recommended for most projects)
     *
     * Default: Auto-detect
     */
    abstract val injectJUnit4Rule: Property<Boolean>

    init {
        // Set default values
        enabled.convention(true)
        applyToAllTests.convention(false)
        libraryVersion.convention("0.1.0-beta.1") // Matches the actual library version
        debug.convention(false)
        // injectJUnit4Rule has no convention - null means auto-detect
    }
}
