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
 * ## Target Methods
 *
 * - java.net.Socket.socketConnect0(InetAddress, int, int)
 * - sun.nio.ch.SocketChannelImpl.connect0(FileDescriptor, InetAddress, int)
 *
 * ## Implementation Status
 *
 * TODO: Implement in next step (Test 2)
 */

#include "agent.h"

// Placeholder - will be implemented when we work on Test 2
