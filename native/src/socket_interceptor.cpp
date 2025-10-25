/**
 * Socket Interception Logic for junit-no-network JVMTI Agent
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

    // Extract hostname from InetAddress for logging
    if (remote != nullptr) {
        jclass inetAddressClass = env->FindClass("java/net/InetAddress");
        if (inetAddressClass != nullptr) {
            jmethodID getHostAddressMethod = env->GetMethodID(
                inetAddressClass,
                "getHostAddress",
                "()Ljava/lang/String;"
            );

            if (getHostAddressMethod != nullptr) {
                jstring hostString = (jstring)env->CallObjectMethod(remote, getHostAddressMethod);
                if (hostString != nullptr) {
                    const char* hostCStr = env->GetStringUTFChars(hostString, nullptr);
                    DEBUG_LOGF("Connection attempt to %s:%d", hostCStr, remotePort);
                    env->ReleaseStringUTFChars(hostString, hostCStr);
                }
            }
        }
    }

    // TODO: Check NetworkConfiguration via JNI call to Kotlin
    // For now, just log and allow all connections

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
