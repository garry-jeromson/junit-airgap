package io.github.garryjeromson.junit.airgap

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.toKString
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import airgap.AirgapURLProtocol

/**
 * Bridge between Kotlin/Native and Objective-C URLProtocol implementation.
 *
 * This file provides:
 * 1. Kotlin functions that call into Objective-C (registerURLProtocol, etc.)
 * 2. Kotlin functions exported to Objective-C (@CName) for callbacks
 */

/**
 * Register the AirgapURLProtocol with NSURLProtocol.
 * After this call, all URLSession requests will be intercepted.
 *
 * @return true if registration succeeded, false otherwise
 */
@OptIn(ExperimentalForeignApi::class)
fun registerURLProtocol(): Boolean {
    return try {
        AirgapURLProtocol.registerAirgapProtocol()
        true
    } catch (e: Exception) {
        DebugLogger.log("Failed to register URLProtocol: ${e.message}")
        false
    }
}

/**
 * Unregister the AirgapURLProtocol from NSURLProtocol.
 * After this call, requests will no longer be intercepted.
 *
 * @return true if unregistration succeeded, false otherwise
 */
@OptIn(ExperimentalForeignApi::class)
fun unregisterURLProtocol(): Boolean {
    return try {
        AirgapURLProtocol.unregisterAirgapProtocol()
        true
    } catch (e: Exception) {
        DebugLogger.log("Failed to unregister URLProtocol: ${e.message}")
        false
    }
}

/**
 * Static C function that will be called from Objective-C to check if a host should be blocked.
 * This is created once and passed to Objective-C as a function pointer.
 */
@OptIn(ExperimentalForeignApi::class)
private val hostBlockingCallback = staticCFunction<CPointer<ByteVar>?, Boolean> { hostPtr ->
    if (hostPtr == null) {
        DebugLogger.log("Callback called with null host pointer")
        return@staticCFunction false
    }

    val host = hostPtr.toKString()
    DebugLogger.log("Checking if host should be blocked: $host")

    val config = NetworkBlocker.getSharedConfiguration()
    if (config == null) {
        DebugLogger.log("No configuration set, allowing request to $host")
        return@staticCFunction false
    }

    // isAllowed() returns true if the host is allowed, we need to return true if BLOCKED
    val isAllowed = config.isAllowed(host)
    val shouldBlock = !isAllowed

    DebugLogger.log("Host $host: allowed=$isAllowed, shouldBlock=$shouldBlock")
    shouldBlock
}

/**
 * Set the configuration for the URLProtocol.
 *
 * @param config NetworkConfiguration to apply
 * @return true if configuration was set successfully
 */
@OptIn(ExperimentalForeignApi::class)
fun setURLProtocolConfiguration(config: NetworkConfiguration): Boolean {
    return try {
        // Determine blockByDefault based on whether allowedHosts is empty
        // If no allowed hosts specified, block everything by default
        val blockByDefault = config.allowedHosts.isEmpty()

        // Convert Kotlin Sets to Objective-C NSArrays
        val allowedHostsList = config.allowedHosts.toList()
        val blockedHostsList = config.blockedHosts.toList()

        // Call the Objective-C API with primitive parameters and callback
        AirgapURLProtocol.setConfigurationWithBlockByDefault(
            blockByDefault = blockByDefault,
            allowedHosts = allowedHostsList,
            blockedHosts = blockedHostsList,
            callback = hostBlockingCallback
        )
        true
    } catch (e: Exception) {
        DebugLogger.log("Failed to set URLProtocol configuration: ${e.message}")
        false
    }
}
