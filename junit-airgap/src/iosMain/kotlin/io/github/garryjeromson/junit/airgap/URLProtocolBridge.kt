package io.github.garryjeromson.junit.airgap

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.toKString
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.setObject
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
 * Set the configuration for the URLProtocol.
 *
 * @param config NetworkConfiguration to apply
 * @return true if configuration was set successfully
 */
@OptIn(ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
fun setURLProtocolConfiguration(config: NetworkConfiguration): Boolean {
    return try {
        // Convert Kotlin NetworkConfiguration to NSDictionary
        val nsConfig = NSMutableDictionary()

        // Determine blockByDefault based on whether allowedHosts is empty
        // If no allowed hosts specified, block everything by default
        val blockByDefault = config.allowedHosts.isEmpty()
        nsConfig.setObject(blockByDefault as Any, forKey = NSString.create(string = "blockByDefault"))

        // Convert Set<String> to NSArray
        nsConfig.setObject(config.allowedHosts.toList() as Any, forKey = NSString.create(string = "allowedHosts"))
        nsConfig.setObject(config.blockedHosts.toList() as Any, forKey = NSString.create(string = "blockedHosts"))

        AirgapURLProtocol.setConfiguration(nsConfig as Map<Any?, *>)
        true
    } catch (e: Exception) {
        DebugLogger.log("Failed to set URLProtocol configuration: ${e.message}")
        false
    }
}

/**
 * Exported function called from Objective-C to check if a host should be blocked.
 *
 * This function is called from AirgapURLProtocol.m when a request is intercepted.
 * It reads the current NetworkConfiguration and determines if the host should be blocked.
 *
 * @param hostPtr C string pointer to the hostname
 * @return true if the host should be blocked, false if allowed
 */
@CName("airgap_should_block_host")
@OptIn(ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
fun shouldBlockHost(hostPtr: CPointer<ByteVar>?): Boolean {
    if (hostPtr == null) {
        DebugLogger.log("shouldBlockHost called with null host pointer")
        return false
    }

    val host = hostPtr.toKString()
    DebugLogger.log("Checking if host should be blocked: $host")

    val config = NetworkBlocker.getSharedConfiguration()
    if (config == null) {
        DebugLogger.log("No configuration set, allowing request to $host")
        return false
    }

    // isAllowed() returns true if the host is allowed, we need to return true if BLOCKED
    val isAllowed = config.isAllowed(host)
    val shouldBlock = !isAllowed

    DebugLogger.log("Host $host: allowed=$isAllowed, shouldBlock=$shouldBlock")
    return shouldBlock
}
