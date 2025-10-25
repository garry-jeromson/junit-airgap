package io.github.garryjeromson.junit.nonetwork

/**
 * Android implementation of NetworkBlocker.
 *
 * ## Android Unit Tests (Robolectric)
 * Uses JVMTI agent since Robolectric runs on the JVM (HotSpot/OpenJDK), not Android Runtime.
 * The JVMTI agent intercepts network calls at the native layer via sun.nio.ch.Net.connect0().
 *
 * ## Android Instrumented Tests (On Device/Emulator)
 * ⚠️ JVMTI does not work on Android Runtime (ART) - only on JVM.
 * Instrumented tests that run on actual devices/emulators will not block network requests.
 * This is acceptable since instrumented tests typically require network access anyway.
 *
 * ## How It Works
 * - Robolectric tests: JVMTI agent (loaded by Gradle plugin) intercepts at native layer
 * - Instrumented tests: No-op (graceful degradation)
 * - Configuration is stored thread-locally via NetworkBlockerContext (when available at runtime)
 * - The Gradle plugin automatically loads the agent for unit tests
 *
 * ## Technical Details
 * This implementation uses reflection to access NetworkBlockerContext at runtime.
 * NetworkBlockerContext is in jvmMain (not visible at compile time), but is available
 * at runtime when Robolectric runs tests on the JVM. The JVMTI agent reads configuration
 * from NetworkBlockerContext to determine which connections to block.
 */
actual class NetworkBlocker actual constructor(
    private val configuration: NetworkConfiguration,
) {
    private var isInstalled: Boolean = false

    /**
     * Install the network blocker.
     *
     * For Robolectric tests (running on JVM), this stores configuration in NetworkBlockerContext
     * that the JVMTI agent will check when intercepting socket connections.
     * For instrumented tests (running on ART), this is a no-op.
     */
    @Synchronized
    actual fun install() {
        if (isInstalled) {
            return
        }

        try {
            // Try to access NetworkBlockerContext (available at runtime in Robolectric/JVM)
            // Use reflection since NetworkBlockerContext is in jvmMain and not visible at compile time
            val contextClass = Class.forName("io.github.garryjeromson.junit.nonetwork.bytebuddy.NetworkBlockerContext")
            val setConfigMethod =
                contextClass.getDeclaredMethod(
                    "setConfiguration",
                    NetworkConfiguration::class.java,
                )
            setConfigMethod.invoke(null, configuration)
            isInstalled = true
        } catch (e: ClassNotFoundException) {
            // NetworkBlockerContext not available - running on ART (instrumented test)
            // This is fine, graceful degradation (no blocking on ART)
            isInstalled = true
        } catch (e: Exception) {
            // Other reflection errors - log but don't fail
            println("Warning: Failed to configure network blocking for Android: ${e.message}")
            isInstalled = true
        }
    }

    /**
     * Uninstall the network blocker.
     *
     * For Robolectric tests, this clears the configuration from NetworkBlockerContext.
     * For instrumented tests, this is a no-op.
     */
    @Synchronized
    actual fun uninstall() {
        if (!isInstalled) {
            return
        }

        try {
            // Clear configuration from NetworkBlockerContext (if available)
            val contextClass = Class.forName("io.github.garryjeromson.junit.nonetwork.bytebuddy.NetworkBlockerContext")
            val clearConfigMethod = contextClass.getDeclaredMethod("clearConfiguration")
            clearConfigMethod.invoke(null)
        } catch (e: ClassNotFoundException) {
            // NetworkBlockerContext not available - running on ART
        } catch (e: Exception) {
            // Other reflection errors - ignore
        }

        isInstalled = false
    }
}
