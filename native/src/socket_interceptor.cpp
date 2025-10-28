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
                    hostAddressCStr = env->GetStringUTFChars(hostAddressString, nullptr);
                }
            }

            // Get hostname (may return cached hostname or do reverse DNS)
            // Note: This might trigger reverse DNS lookup (e.g., "127.0.0.1" -> "localhost")
            // but that's okay because we're already past DNS resolution at this point
            jmethodID getHostNameMethod = env->GetMethodID(
                inetAddressClass,
                "getHostName",
                "()Ljava/lang/String;"
            );

            if (getHostNameMethod != nullptr) {
                hostNameString = (jstring)env->CallObjectMethod(remote, getHostNameMethod);
                if (hostNameString != nullptr) {
                    hostNameCStr = env->GetStringUTFChars(hostNameString, nullptr);
                }
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
            // Create caller string
            jstring callerString = env->NewStringUTF("JVMTI-Agent");

            // First, check if hostname or IP are explicitly blocked
            bool hostnameExplicitlyBlocked = false;
            bool ipExplicitlyBlocked = false;

            if (hostNameString != nullptr && hostNameCStr != nullptr) {
                hostnameExplicitlyBlocked = env->CallStaticBooleanMethod(contextClass, isExplicitlyBlockedMethod, hostNameString);
                if (hostnameExplicitlyBlocked) {
                    DEBUG_LOGF("Hostname %s is explicitly blocked", hostNameCStr);
                }
            }

            if (hostAddressString != nullptr && hostAddressCStr != nullptr) {
                ipExplicitlyBlocked = env->CallStaticBooleanMethod(contextClass, isExplicitlyBlockedMethod, hostAddressString);
                if (ipExplicitlyBlocked) {
                    DEBUG_LOGF("IP address %s is explicitly blocked", hostAddressCStr);
                }
            }

            // If either hostname or IP is explicitly blocked, block the connection
            if (hostnameExplicitlyBlocked || ipExplicitlyBlocked) {
                DEBUG_LOG("Connection blocked - host explicitly in blockedHosts");
                connectionBlocked = true;
                // Call checkConnection to throw the exception with proper details
                if (hostNameString != nullptr) {
                    env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostNameString, remotePort, callerString);
                } else if (hostAddressString != nullptr) {
                    env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostAddressString, remotePort, callerString);
                }
            } else {
                // Not explicitly blocked, try checking if allowed
                // Try hostname first (if available)
                if (hostNameString != nullptr && hostNameCStr != nullptr) {
                    DEBUG_LOGF("Checking hostname: %s", hostNameCStr);
                    env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostNameString, remotePort, callerString);

                    if (env->ExceptionCheck()) {
                        // Hostname not allowed, clear exception and try IP address
                        DEBUG_LOGF("Hostname %s not allowed, trying IP address", hostNameCStr);
                        env->ExceptionClear();

                        // Now try with IP address
                        if (hostAddressString != nullptr && hostAddressCStr != nullptr) {
                            DEBUG_LOGF("Checking IP address: %s", hostAddressCStr);
                            env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostAddressString, remotePort, callerString);

                            if (env->ExceptionCheck()) {
                                // Neither hostname nor IP are allowed
                                DEBUG_LOG("Both hostname and IP address not allowed");
                                connectionBlocked = true;
                            } else {
                                DEBUG_LOG("IP address allowed (hostname was not)");
                            }
                        } else {
                            // No IP address to check, connection is blocked
                            connectionBlocked = true;
                            // Re-throw the exception from hostname check
                            env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostNameString, remotePort, callerString);
                        }
                    } else {
                        DEBUG_LOG("Hostname allowed");
                    }
                } else if (hostAddressString != nullptr && hostAddressCStr != nullptr) {
                    // No hostname, only check IP address
                    DEBUG_LOGF("Checking IP address only: %s", hostAddressCStr);
                    env->CallStaticVoidMethod(contextClass, checkConnectionMethod, hostAddressString, remotePort, callerString);

                    if (env->ExceptionCheck()) {
                        DEBUG_LOG("IP address not allowed");
                        connectionBlocked = true;
                    }
                }
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
