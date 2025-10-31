package io.github.garryjeromson.junit.airgap.integration.fixtures

import java.net.InetAddress

/**
 * Test fixture that simulates Gradle worker initialization.
 *
 * Gradle workers may perform network operations during initialization
 * (e.g., connecting to the Gradle daemon) before test classes are loaded.
 * This simulates that scenario.
 *
 * ## Expected Behavior
 *
 * - No "platform encoding not initialized" errors
 * - Network operations complete successfully (agent not yet blocking)
 * - When NetworkBlocker is installed later, blocking works correctly
 */
fun main() {
    println("\n=== Gradle Worker Initialization Simulation ===\n")

    try {
        println("Simulating worker initialization...")

        // Simulate Gradle worker DNS resolution (happens before tests load)
        println("Resolving localhost...")
        val localhost = InetAddress.getByName("localhost")
        println("  Resolved: ${localhost.hostAddress}")

        // Simulate looking up loopback
        println("Getting loopback address...")
        val loopback = InetAddress.getLoopbackAddress()
        println("  Loopback: ${loopback.hostAddress}")

        println("\nWorker initialization complete (no errors)")
        println("SUCCESS: Worker initialization did not cause platform encoding errors")

        System.exit(0)
    } catch (e: Exception) {
        System.err.println("ERROR during worker initialization: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}
