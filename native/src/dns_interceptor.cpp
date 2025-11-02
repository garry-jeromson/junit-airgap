/**
 * DNS Resolution Interception Logic for junit-airgapJVMTI Agent
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
#include <unistd.h>  // for usleep()

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
    DEBUG_LOGF("wrapped_lookupAllHostAddr(%s) called", impl_name);

    // IMPORTANT: Platform encoding initialization happens AFTER VM_INIT
    // Even though VM_INIT completes, platform encoding may not be ready yet
    // NetworkBlockerContext registration happens after platform encoding is ready
    // So we use NetworkBlockerContext registration as a signal that everything is ready

    // Check 1: VM_INIT must be complete (basic JVM initialization)
    if (!g_vm_init_complete) {
        DEBUG_LOG("VM_INIT not complete - allowing DNS without interception");
        if (original != nullptr) {
            return original(env, obj, hostname);
        }
        return nullptr;
    }

    // Check 2: NetworkBlockerContext must be registered (platform encoding ready)
    jclass contextClass = GetNetworkBlockerContextClass();
    if (contextClass == nullptr) {
        DEBUG_LOG("NetworkBlockerContext not registered - allowing DNS without interception (platform encoding may not be ready)");
        if (original != nullptr) {
            return original(env, obj, hostname);
        }
        return nullptr;
    }

    // Both VM_INIT and NetworkBlockerContext ready - safe to proceed
    DEBUG_LOG("VM_INIT complete and NetworkBlockerContext registered - proceeding with DNS interception");

    // Check 3: Is there an active configuration? (optimization to skip string extraction)
    // If no configuration is set (e.g., @AllowNetworkRequests tests), we can skip all
    // JNI string operations and immediately allow the DNS lookup. This avoids platform
    // encoding issues in edge cases where VM_INIT is complete but platform encoding
    // might not be fully ready for all string operations.
    //
    // NOTE: hasActiveConfiguration method may be nullptr on some platforms (Linux) due to
    // class initialization timing. If it's not available, we skip this optimization and
    // always proceed with full interception logic (less efficient but still correct).
    jmethodID hasActiveConfigMethod = GetHasActiveConfigurationMethod();
    if (hasActiveConfigMethod != nullptr) {
        jboolean hasConfig = env->CallStaticBooleanMethod(contextClass, hasActiveConfigMethod);
        if (!hasConfig) {
            DEBUG_LOG("No active configuration - allowing DNS without interception");

            // IMPORTANT: Ensure platform encoding is ready for the current thread before calling original function.
            // The original Java DNS function requires platform encoding, which is initialized per-thread.
            // In Android Studio's test runner, the "Test worker" thread may not have platform encoding ready
            // immediately, even though VM_INIT completed and NetworkBlockerContext is registered.
            // By proactively ensuring it's ready, we avoid "platform encoding not initialized" errors.
            if (!EnsurePlatformEncodingReady(env)) {
                DEBUG_LOG("Failed to ensure platform encoding ready - cannot call original DNS function");
                // Throw exception to indicate the issue
                jclass exClass = env->FindClass("java/lang/InternalError");
                if (exClass != nullptr) {
                    env->ThrowNew(exClass, "Platform encoding not ready for DNS resolution");
                }
                return nullptr;
            }

            // Platform encoding is ready - safe to call original function
            if (original != nullptr) {
                DEBUG_LOG("Platform encoding ready - calling original DNS function");
                return original(env, obj, hostname);
            }
            return nullptr;
        }
        DEBUG_LOG("Active configuration detected - proceeding with interception");
    } else {
        DEBUG_LOG("hasActiveConfiguration method not available - proceeding with interception (less efficient)");
    }

    // STEP 1: Extract hostname and check if connection is allowed
    // This will throw NetworkRequestAttemptedException if blocked
    const char* hostCStr = nullptr;
    if (hostname != nullptr) {
        hostCStr = env->GetStringUTFChars(hostname, nullptr);
        if (hostCStr != nullptr) {
            DEBUG_LOGF("DNS resolution attempt for hostname: %s", hostCStr);
        }
    }

    // Check NetworkConfiguration via JNI call to Kotlin
    // IMPORTANT: checkConnection() returns silently if no config (inter-test period)
    // It only throws if there IS a config AND connection is blocked
    if (hostname != nullptr && hostCStr != nullptr) {
        // Get checkConnectionMethod (contextClass already verified above)
        jmethodID checkConnectionMethod = GetCheckConnectionMethod();

        if (checkConnectionMethod != nullptr) {
            DEBUG_LOG("Calling NetworkBlockerContext.checkConnection() for DNS");

            // Get cached caller string (initialized during VM_INIT)
            jstring callerString = GetCallerDnsString();

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
            DEBUG_LOG("checkConnectionMethod not available - allowing DNS");
        }
    }

    // Release hostname string
    if (hostname != nullptr && hostCStr != nullptr) {
        env->ReleaseStringUTFChars(hostname, hostCStr);
    }

    // STEP 2: Connection is allowed - call original DNS resolution
    // If checkConnection() didn't throw, the connection is allowed
    // The VM_INIT and NetworkBlockerContext registration checks above already
    // prevent calling this function during JVM initialization when platform
    // encoding might not be ready
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
