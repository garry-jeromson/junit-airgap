package io.github.garryjeromson.junit.airgap.integration.fixtures

import io.github.garryjeromson.junit.airgap.NetworkBlocker
import io.github.garryjeromson.junit.airgap.NetworkConfiguration
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import java.net.Socket

/**
 * Test fixture that simulates IntelliJ IDEA's eager class loading behavior.
 *
 * This class forces networking classes to load during static initialization,
 * which happens before VM_INIT in IntelliJ's test runner. This simulates
 * the scenario that caused "platform encoding not initialized" errors.
 *
 * ## Initialization Sequence
 *
 * 1. JVM starts with JVMTI agent loaded
 * 2. This object's static initializer runs (eager loading)
 * 3. Networking classes load: Socket, Net, Inet6AddressImpl, etc.
 * 4. Native methods bind (NativeMethodBind callback fires)
 * 5. Agent stores original function pointers (wrappers NOT installed yet)
 * 6. main() executes
 * 7. VM_INIT callback fires (if not already)
 * 8. Deferred wrappers installed
 * 9. Network operations are blocked correctly
 *
 * ## Expected Behavior
 *
 * - No "platform encoding not initialized" errors
 * - Network blocking works correctly
 * - Debug log shows deferred wrapper installation
 */
object EarlyClassLoadingMain {
    init {
        println("Static initializer: Starting early class loading")

        // Force eager loading of networking classes
        // This simulates IntelliJ's test discovery phase
        try {
            Class.forName("java.net.Socket")
            println("  Loaded: java.net.Socket")

            Class.forName("sun.nio.ch.Net")
            println("  Loaded: sun.nio.ch.Net")

            Class.forName("java.net.Inet6AddressImpl")
            println("  Loaded: java.net.Inet6AddressImpl")

            Class.forName("java.net.Inet4AddressImpl")
            println("  Loaded: java.net.Inet4AddressImpl")

            Class.forName("java.net.InetAddress")
            println("  Loaded: java.net.InetAddress")

            println("Static initializer: Early class loading complete")
        } catch (e: Exception) {
            System.err.println("ERROR during early class loading: ${e.message}")
            e.printStackTrace()
        }
    }
}

/**
 * Main entry point for the early class loading test.
 *
 * By the time this runs, the static initializer has already forced
 * networking classes to load.
 */
fun main() {
    println("\n=== Early Initialization Test ===\n")

    // Install network blocker with no allowed hosts
    val config = NetworkConfiguration(allowedHosts = emptySet())
    val blocker = NetworkBlocker(config)

    try {
        println("Installing network blocker...")
        blocker.install()
        println("Network blocker installed successfully")

        println("\nAttempting network connection (should be blocked)...")

        try {
            // This should be blocked by the JVMTI agent
            Socket("example.com", 80).use {
                println("ERROR: Network request should have been blocked!")
                System.exit(1)
            }
        } catch (e: NetworkRequestAttemptedException) {
            println("SUCCESS: Network request blocked as expected")
            println("  Exception: ${e.message}")
            System.exit(0)
        }
    } catch (e: Exception) {
        System.err.println("ERROR: Unexpected exception: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    } finally {
        try {
            blocker.uninstall()
            println("Network blocker uninstalled")
        } catch (e: Exception) {
            System.err.println("ERROR uninstalling blocker: ${e.message}")
        }
    }
}
