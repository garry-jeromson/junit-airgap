package io.github.garryjeromson.junit.airgap

/**
 * Core component that intercepts and blocks network requests based on configuration.
 *
 * ## Platform Implementations
 *
 * ### JVM
 * Uses JVMTI native agent that intercepts `sun.nio.ch.Net.connect0()` at the JNI layer.
 * Works with all modern Java networking libraries (HttpURLConnection, OkHttp, Apache HttpClient, etc.)
 *
 * ### Android
 * - **Unit Tests (Robolectric)**: Uses JVMTI agent (Robolectric runs on JVM, not ART)
 * - **Instrumented Tests (Device/Emulator)**: No-op/graceful degradation (JVMTI doesn't work on ART)
 *
 * The Gradle plugin automatically configures the JVMTI agent for unit tests.
 */
expect class NetworkBlocker(
    configuration: NetworkConfiguration,
) {
    /**
     * Installs the network blocker.
     * After installation, network connections will be checked against the configuration.
     */
    fun install()

    /**
     * Uninstalls the network blocker.
     * After uninstallation, network requests will work normally again.
     */
    fun uninstall()
}
