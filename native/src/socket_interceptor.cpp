/**
 * Socket Interception Logic for junit-airgapJVMTI Agent
 *
 * This file contains the wrapper functions for Socket and SocketChannel native methods.
 *
 * ## Interception Strategy
 *
 * 1. Store original function pointers when NativeMethodBindCallback is called
 * 2. Replace with our wrapper functions
 * 3. Wrapper functions:
 *    - Get NetworkConfiguration from thread-local storage (via JNI call to Kotlin)
 *    - Check if connection is allowed
 *    - If blocked: throw NetworkRequestAttemptedException
 *    - If allowed: call original native function
 *
 * ## Target Method: sun.nio.ch.Net.connect0()
 *
 * Signature: static native int connect0(boolean preferIPv6, FileDescriptor fd,
 *                                        InetAddress remote, int remotePort)
 *
 * This method is used by ALL modern Java socket implementations (Socket, SocketChannel,
 * HttpURLConnection, OkHttp, Apache HttpClient, Java 11 HttpClient, etc.)
 */

#include "agent.h"
#include <cstring>

// Function pointer type for sun.nio.ch.Net.connect0()
// Signature: (ZLjava/io/FileDescriptor;Ljava/net/InetAddress;I)I
typedef jint (*NetConnect0Func)(JNIEnv*, jclass, jboolean, jobject, jobject, jint);

// Storage for original function pointer
static NetConnect0Func original_Net_connect0 = nullptr;

/**
 * Check if a host (by IP or hostname) is explicitly blocked.
 *
 * @param env JNI environment
 * @param contextClass NetworkBlockerContext class
 * @param isExplicitlyBlockedMethod isExplicitlyBlocked method ID
 * @param hostString The host to check (IP or hostname)
 * @param hostCStr C string representation for logging
 * @return true if host is explicitly blocked, false otherwise
 */
static bool isHostExplicitlyBlocked(
    JNIEnv* env,
    jclass contextClass,
    jmethodID isExplicitlyBlockedMethod,
    jstring hostString,
    const char* hostCStr
) {
    if (hostString == nullptr || hostCStr == nullptr) {
        return false;
    }

    bool blocked = env->CallStaticBooleanMethod(contextClass, isExplicitlyBlockedMethod, hostString);
    if (blocked) {
        DEBUG_LOGF("Host %s is explicitly blocked", hostCStr);
    }
    return blocked;
}

/**
 * Try to allow connection by checking IP address first, then hostname as fallback.
 *
 * @param env JNI environment
 * @param contextClass NetworkBlockerContext class
 * @param checkConnectionMethod checkConnection method ID
 * @param ipAddressString IP address to check (may be null)
 * @param ipAddressCStr C string for logging
 * @param hostnameString Hostname to check (may be null)
 * @param hostnameCStr C string for logging
 * @param remotePort Port number
 * @param callerString Caller info for exception messages
 * @return true if connection is blocked, false if allowed
 */
static bool tryAllowConnection(
    JNIEnv* env,
    jclass contextClass,
    jmethodID checkConnectionMethod,
    jstring ipAddressString,
    const char* ipAddressCStr,
    jstring hostnameString,
    const char* hostnameCStr,
    jint remotePort,
    jstring callerString
) {
    // Try IP address first (prioritize actual connection target over reverse DNS)
    if (ipAddressString != nullptr && ipAddressCStr != nullptr) {
        DEBUG_LOGF("Checking IP address: %s", ipAddressCStr);
        env->CallStaticVoidMethod(contextClass, checkConnectionMethod, ipAddressString, remotePort, callerString);

        if (!env->ExceptionCheck()) {
            DEBUG_LOG("IP address allowed");
            return false; // Connection allowed
        }

        // IP not allowed, clear exception and try hostname
        DEBUG_LOGF("IP address %s not allowed, trying hostname", ipAddressCStr);
        env->ExceptionClear();

        // Try with hostname (if available)
        if (hostnameString != nullptr && hostnameCStr != nullptr) {
            DEBUG_LOGF("Checking hostname: %s", hostnameCStr);
            env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostnameString, remotePort, callerString);

            if (!env->ExceptionCheck()) {
                DEBUG_LOG("Hostname allowed (IP address was not)");
                return false; // Connection allowed
            }

            DEBUG_LOG("Both IP address and hostname not allowed");
            return true; // Connection blocked
        }

        // No hostname to check, connection is blocked
        // Re-throw the exception from IP check
        env->CallStaticVoidMethod(contextClass, checkConnectionMethod, ipAddressString, remotePort, callerString);
        return true; // Connection blocked
    }

    // No IP address available, check hostname only
    if (hostnameString != nullptr && hostnameCStr != nullptr) {
        DEBUG_LOGF("Checking hostname only: %s", hostnameCStr);
        env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostnameString, remotePort, callerString);

        if (env->ExceptionCheck()) {
            DEBUG_LOG("Hostname not allowed");
            return true; // Connection blocked
        }
        return false; // Connection allowed
    }

    // No host information available - allow by default
    return false;
}

/**
 * Wrapper for sun.nio.ch.Net.connect0()
 *
 * This wrapper intercepts all socket connections in modern Java.
 *
 * @param env JNI environment
 * @param cls Class (sun.nio.ch.Net)
 * @param preferIPv6 Whether to prefer IPv6
 * @param fd File descriptor
 * @param remote InetAddress to connect to
 * @param remotePort Port to connect to
 * @return Connection result (0 = success, -1 = in progress, -2 = error)
 */
jint JNICALL wrapped_Net_connect0(
    JNIEnv* env,
    jclass cls,
    jboolean preferIPv6,
    jobject fd,
    jobject remote,
    jint remotePort
) {
    DEBUG_LOG("wrapped_Net_connect0() called - intercepting connection attempt");

    // IMPORTANT: Platform encoding initialization happens AFTER VM_INIT
    // Even though VM_INIT completes, platform encoding may not be ready yet
    // NetworkBlockerContext registration happens after platform encoding is ready
    // So we use NetworkBlockerContext registration as a signal that everything is ready

    // Check 1: VM_INIT must be complete (basic JVM initialization)
    if (!g_vm_init_complete) {
        DEBUG_LOG("VM_INIT not complete - allowing socket connection without interception");
        if (original_Net_connect0 != nullptr) {
            return original_Net_connect0(env, cls, preferIPv6, fd, remote, remotePort);
        }
        return -2; // Error if original function not available
    }

    // Check 2: NetworkBlockerContext must be registered (platform encoding ready)
    jclass contextClass = GetNetworkBlockerContextClass();
    if (contextClass == nullptr) {
        DEBUG_LOG("NetworkBlockerContext not registered - allowing socket connection without interception (platform encoding may not be ready)");
        if (original_Net_connect0 != nullptr) {
            return original_Net_connect0(env, cls, preferIPv6, fd, remote, remotePort);
        }
        return -2; // Error if original function not available
    }

    // Both VM_INIT and NetworkBlockerContext ready - safe to proceed
    DEBUG_LOG("VM_INIT complete and NetworkBlockerContext registered - proceeding with socket interception");

    // Check 3: Is there an active configuration? (optimization to skip string extraction)
    // If no configuration is set (e.g., @AllowNetworkRequests tests), we can skip all
    // JNI string operations and immediately allow the connection. This avoids platform
    // encoding issues in edge cases where VM_INIT is complete but platform encoding
    // might not be fully ready for all string operations.
    jmethodID hasActiveConfigMethod = GetHasActiveConfigurationMethod();
    if (hasActiveConfigMethod == nullptr) {
        // Method not registered yet - assume no configuration and allow
        DEBUG_LOG("hasActiveConfiguration method not registered - allowing socket connection without interception");
        if (original_Net_connect0 != nullptr) {
            return original_Net_connect0(env, cls, preferIPv6, fd, remote, remotePort);
        }
        return -2; // Error if original function not available
    }

    jboolean hasConfig = env->CallStaticBooleanMethod(contextClass, hasActiveConfigMethod);
    if (!hasConfig) {
        DEBUG_LOG("No active configuration - allowing socket connection without interception");
        if (original_Net_connect0 != nullptr) {
            return original_Net_connect0(env, cls, preferIPv6, fd, remote, remotePort);
        }
        return -2; // Error if original function not available
    }
    DEBUG_LOG("Active configuration detected - proceeding with interception");

    // Extract both hostname and IP address from InetAddress
    // We need to check BOTH because:
    // 1. User might allowlist "example.com" (hostname)
    // 2. User might allowlist "127.0.0.1" (IP address)
    // 3. DNS interception checks hostname, socket should check both hostname and IP
    jstring hostNameString = nullptr;
    jstring hostAddressString = nullptr;
    const char* hostNameCStr = nullptr;
    const char* hostAddressCStr = nullptr;

    if (remote != nullptr) {
        jclass inetAddressClass = env->FindClass("java/net/InetAddress");
        if (inetAddressClass != nullptr) {
            // Get IP address (never does reverse DNS)
            jmethodID getHostAddressMethod = env->GetMethodID(
                inetAddressClass,
                "getHostAddress",
                "()Ljava/lang/String;"
            );

            if (getHostAddressMethod != nullptr) {
                hostAddressString = (jstring)env->CallObjectMethod(remote, getHostAddressMethod);
                if (hostAddressString != nullptr) {
                    // Test platform encoding before extracting
                    const char* testStr = env->GetStringUTFChars(hostAddressString, nullptr);
                    if (testStr != nullptr) {
                        hostAddressCStr = testStr;  // Keep the string
                    } else {
                        DEBUG_LOG("Platform encoding not ready - skipping IP address extraction");
                        if (env->ExceptionCheck()) {
                            env->ExceptionClear();
                        }
                    }
                }
            }

            // Get hostname (may return cached hostname or do reverse DNS)
            // IMPORTANT: Only do this if platform encoding is ready (tested above)
            // getHostName() can trigger reverse DNS which requires platform encoding
            if (hostAddressCStr != nullptr) {
                jmethodID getHostNameMethod = env->GetMethodID(
                    inetAddressClass,
                    "getHostName",
                    "()Ljava/lang/String;"
                );

                if (getHostNameMethod != nullptr) {
                    hostNameString = (jstring)env->CallObjectMethod(remote, getHostNameMethod);
                    if (hostNameString != nullptr) {
                        const char* testStr = env->GetStringUTFChars(hostNameString, nullptr);
                        if (testStr != nullptr) {
                            hostNameCStr = testStr;  // Keep the string
                        } else {
                            DEBUG_LOG("Platform encoding not ready - skipping hostname extraction");
                            if (env->ExceptionCheck()) {
                                env->ExceptionClear();
                            }
                        }
                    }
                }
            } else {
                DEBUG_LOG("Skipping hostname extraction - platform encoding not ready");
            }

            DEBUG_LOGF("Connection attempt - hostname: %s, IP: %s, port: %d",
                      hostNameCStr ? hostNameCStr : "(null)",
                      hostAddressCStr ? hostAddressCStr : "(null)",
                      remotePort);
        }
    }

    // Check NetworkConfiguration via JNI call to Kotlin
    // Logic:
    // 1. If hostname is EXPLICITLY in blockedHosts → block (don't check IP)
    // 2. If IP is EXPLICITLY in blockedHosts → block (don't check hostname)
    // 3. If hostname is allowed → allow
    // 4. If IP is allowed → allow
    // 5. Otherwise → block
    bool connectionBlocked = false;
    if (remote != nullptr) {
        // Get cached class and method references
        jclass contextClass = GetNetworkBlockerContextClass();
        jmethodID checkConnectionMethod = GetCheckConnectionMethod();
        jmethodID isExplicitlyBlockedMethod = GetIsExplicitlyBlockedMethod();

        if (contextClass != nullptr && checkConnectionMethod != nullptr && isExplicitlyBlockedMethod != nullptr) {
            // Get cached caller string (initialized during VM_INIT)
            jstring callerString = GetCallerAgentString();

            // Check if hostname or IP are explicitly blocked (highest priority)
            bool hostnameExplicitlyBlocked = isHostExplicitlyBlocked(
                env, contextClass, isExplicitlyBlockedMethod, hostNameString, hostNameCStr
            );
            bool ipExplicitlyBlocked = isHostExplicitlyBlocked(
                env, contextClass, isExplicitlyBlockedMethod, hostAddressString, hostAddressCStr
            );
            bool hostIsExplicitlyBlocked = hostnameExplicitlyBlocked || ipExplicitlyBlocked;

            // If explicitly blocked, block the connection immediately
            if (hostIsExplicitlyBlocked) {
                DEBUG_LOG("Connection blocked - host explicitly in blockedHosts");
                connectionBlocked = true;
                // Call checkConnection to throw the exception with proper details
                if (hostNameString != nullptr) {
                    env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostNameString, remotePort, callerString);
                } else if (hostAddressString != nullptr) {
                    env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostAddressString, remotePort, callerString);
                }
            } else {
                // Not explicitly blocked, check if connection is allowed via allowlist
                connectionBlocked = tryAllowConnection(
                    env, contextClass, checkConnectionMethod,
                    hostAddressString, hostAddressCStr,
                    hostNameString, hostNameCStr,
                    remotePort, callerString
                );
            }
        } else {
            DEBUG_LOG("NetworkBlockerContext not registered - allowing connection (agent may not be loaded or class not initialized yet)");
        }
    }

    // Release strings
    if (hostNameString != nullptr && hostNameCStr != nullptr) {
        env->ReleaseStringUTFChars(hostNameString, hostNameCStr);
    }
    if (hostAddressString != nullptr && hostAddressCStr != nullptr) {
        env->ReleaseStringUTFChars(hostAddressString, hostAddressCStr);
    }

    // If connection is blocked, return error
    if (connectionBlocked && env->ExceptionCheck()) {
        DEBUG_LOG("Connection blocked - NetworkRequestAttemptedException will propagate");
        return -2; // Error code
    }

    // Call original function
    if (original_Net_connect0 != nullptr) {
        DEBUG_LOG("Calling original Net.connect0()");
        return original_Net_connect0(env, cls, preferIPv6, fd, remote, remotePort);
    } else {
        DEBUG_LOG("ERROR: Original Net.connect0() not found!");
        // Return error if we don't have the original function
        return -2;
    }
}

/**
 * Install the wrapper for sun.nio.ch.Net.connect0()
 *
 * This stores the original function pointer and returns the wrapper address.
 *
 * @param original_address Original native function address
 * @return Wrapper function address
 */
void* InstallNetConnect0Wrapper(void* original_address) {
    DEBUG_LOG("Installing wrapper for sun.nio.ch.Net.connect0()");
    original_Net_connect0 = (NetConnect0Func)original_address;
    return (void*)wrapped_Net_connect0;
}
