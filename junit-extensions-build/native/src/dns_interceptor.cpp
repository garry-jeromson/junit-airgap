/**
 * DNS Resolution Interception Logic for junit-no-network JVMTI Agent
 *
 * This file contains the wrapper functions for DNS resolution native methods.
 *
 * ## Interception Strategy
 *
 * 1. Store original function pointers when NativeMethodBindCallback is called
 * 2. Replace with our wrapper functions
 * 3. Wrapper functions:
 *    - Get NetworkConfiguration from thread-local storage (via JNI call to Kotlin)
 *    - Check if DNS lookup is allowed for the hostname
 *    - If blocked: throw NetworkRequestAttemptedException
 *    - If allowed: call original native function
 *
 * ## Target Methods
 *
 * ### java.net.Inet6AddressImpl.lookupAllHostAddr()
 * Signature: private native InetAddress[] lookupAllHostAddr(String hostname)
 * JNI Signature: (Ljava/lang/String;)[Ljava/net/InetAddress;
 *
 * ### java.net.Inet4AddressImpl.lookupAllHostAddr()
 * Signature: private native InetAddress[] lookupAllHostAddr(String hostname)
 * JNI Signature: (Ljava/lang/String;)[Ljava/net/InetAddress;
 *
 * These methods are called by InetAddress.getAllByName() and InetAddress.getByName(),
 * which are used by ALL Java networking libraries before socket connections.
 *
 * ## Why Intercept DNS?
 *
 * DNS resolution happens BEFORE socket connection:
 * 1. DNS: InetAddress.getAllByName("example.com") → lookupAllHostAddr()
 * 2. Socket: socket.connect(inetAddress, port) → Net.connect0()
 *
 * By intercepting DNS, we can:
 * - Block network requests earlier in the flow
 * - Work with fake hostnames that don't resolve
 * - Provide better error messages (hostname-based, not IP-based)
 * - Match real-world behavior (DNS is a network operation)
 */

#include "agent.h"
#include <cstring>

// Function pointer types for lookupAllHostAddr()
// Signature: (Ljava/lang/String;)[Ljava/net/InetAddress;
typedef jobjectArray (*LookupAllHostAddrFunc)(JNIEnv*, jobject, jstring);

// Storage for original function pointers
static LookupAllHostAddrFunc original_Inet6_lookupAllHostAddr = nullptr;
static LookupAllHostAddrFunc original_Inet4_lookupAllHostAddr = nullptr;

/**
 * Wrapper for Inet6AddressImpl.lookupAllHostAddr() and Inet4AddressImpl.lookupAllHostAddr()
 *
 * This wrapper intercepts DNS resolution attempts and checks configuration
 * before allowing the lookup to proceed.
 *
 * @param env JNI environment
 * @param obj InetAddressImpl instance
 * @param hostname Hostname to resolve
 * @param original Original native function to call if allowed
 * @param impl_name Name of implementation (for debug logging)
 * @return Array of InetAddress objects, or nullptr if exception thrown
 */
static jobjectArray wrapped_lookupAllHostAddr(
    JNIEnv* env,
    jobject obj,
    jstring hostname,
    LookupAllHostAddrFunc original,
    const char* impl_name
) {
    DEBUG_LOGF("wrapped_lookupAllHostAddr(%s) called - intercepting DNS resolution", impl_name);

    // Extract hostname string
    const char* hostCStr = nullptr;
    if (hostname != nullptr) {
        hostCStr = env->GetStringUTFChars(hostname, nullptr);
        if (hostCStr != nullptr) {
            DEBUG_LOGF("DNS resolution attempt for hostname: %s", hostCStr);
        }
    }

    // Check NetworkConfiguration via JNI call to Kotlin
    if (hostname != nullptr && hostCStr != nullptr) {
        // Get cached class and method references
        jclass contextClass = GetNetworkBlockerContextClass();
        jmethodID checkConnectionMethod = GetCheckConnectionMethod();

        if (contextClass != nullptr && checkConnectionMethod != nullptr) {
            DEBUG_LOG("Calling NetworkBlockerContext.checkConnection() for DNS");

            // Create caller string
            jstring callerString = env->NewStringUTF("JVMTI-DNS");

            // Call checkConnection with port -1 (DNS doesn't have a port)
            // This will throw NetworkRequestAttemptedException if blocked
            env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostname, -1, callerString);

            // Check if exception was thrown
            if (env->ExceptionCheck()) {
                DEBUG_LOGF("DNS resolution blocked for: %s", hostCStr);
                // Release hostname string before returning
                env->ReleaseStringUTFChars(hostname, hostCStr);
                // Exception will propagate to Java
                return nullptr;
            }

            DEBUG_LOGF("DNS resolution allowed for: %s", hostCStr);
        } else {
            DEBUG_LOG("NetworkBlockerContext not registered - allowing DNS (agent may not be loaded or class not initialized yet)");
        }
    }

    // Release hostname string
    if (hostname != nullptr && hostCStr != nullptr) {
        env->ReleaseStringUTFChars(hostname, hostCStr);
    }

    // Call original function
    if (original != nullptr) {
        DEBUG_LOGF("Calling original %s.lookupAllHostAddr()", impl_name);
        return original(env, obj, hostname);
    } else {
        DEBUG_LOGF("ERROR: Original %s.lookupAllHostAddr() not found!", impl_name);
        // Throw exception if we don't have the original function
        jclass exClass = env->FindClass("java/lang/UnsupportedOperationException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, "JVMTI agent error: Original lookupAllHostAddr not found");
        }
        return nullptr;
    }
}

/**
 * Wrapper for Inet6AddressImpl.lookupAllHostAddr()
 */
jobjectArray JNICALL wrapped_Inet6_lookupAllHostAddr(
    JNIEnv* env,
    jobject obj,
    jstring hostname
) {
    return wrapped_lookupAllHostAddr(env, obj, hostname, original_Inet6_lookupAllHostAddr, "Inet6AddressImpl");
}

/**
 * Wrapper for Inet4AddressImpl.lookupAllHostAddr()
 */
jobjectArray JNICALL wrapped_Inet4_lookupAllHostAddr(
    JNIEnv* env,
    jobject obj,
    jstring hostname
) {
    return wrapped_lookupAllHostAddr(env, obj, hostname, original_Inet4_lookupAllHostAddr, "Inet4AddressImpl");
}

/**
 * Install the wrapper for Inet6AddressImpl.lookupAllHostAddr()
 *
 * This stores the original function pointer and returns the wrapper address.
 *
 * @param original_address Original native function address
 * @return Wrapper function address
 */
void* InstallInet6LookupWrapper(void* original_address) {
    DEBUG_LOG("Installing wrapper for Inet6AddressImpl.lookupAllHostAddr()");
    original_Inet6_lookupAllHostAddr = (LookupAllHostAddrFunc)original_address;
    return (void*)wrapped_Inet6_lookupAllHostAddr;
}

/**
 * Install the wrapper for Inet4AddressImpl.lookupAllHostAddr()
 *
 * This stores the original function pointer and returns the wrapper address.
 *
 * @param original_address Original native function address
 * @return Wrapper function address
 */
void* InstallInet4LookupWrapper(void* original_address) {
    DEBUG_LOG("Installing wrapper for Inet4AddressImpl.lookupAllHostAddr()");
    original_Inet4_lookupAllHostAddr = (LookupAllHostAddrFunc)original_address;
    return (void*)wrapped_Inet4_lookupAllHostAddr;
}
