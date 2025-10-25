/**
 * JVMTI Agent for junit-no-network
 *
 * This agent intercepts network connections at the JVM-native boundary
 * by replacing native method implementations for Socket and SocketChannel.
 *
 * ## How It Works
 *
 * 1. Agent_OnLoad() is called when JVM loads the agent
 * 2. We register for JVMTI_EVENT_NATIVE_METHOD_BIND events
 * 3. When native methods are bound, NativeMethodBindCallback() is called
 * 4. We replace Socket.connect0() and SocketChannel.connect0() with our wrappers
 * 5. Our wrappers check thread-local configuration before calling original
 *
 * ## Architecture
 *
 * ```
 * Java Code: socket.connect(...)
 *     ↓
 * JNI Bridge: Socket.socketConnect0() native method
 *     ↓
 * JVMTI Callback: NativeMethodBindCallback()
 *     ↓
 * Our Wrapper: wrapped_Socket_connect0()
 *     ↓ (if allowed)
 * Original Native: socket_connect0_original()
 * ```
 *
 * ## Thread Safety
 *
 * - Configuration is ThreadLocal (managed by Kotlin NetworkBlockerContext)
 * - Native method replacement is atomic (JVMTI guarantee)
 * - Original function pointers are stored in thread-safe map
 *
 * ## Test-First Approach
 *
 * Test 1 (AgentLoadTest.java): Verify agent loads successfully
 * Test 2 (SocketInterceptTest.java): Verify Socket.connect() is intercepted
 * Test 3 (NIOInterceptTest.java): Verify SocketChannel.connect() is intercepted
 */

#include "agent.h"
#include <cstdio>
#include <cstring>

// Global state
jvmtiEnv *g_jvmti = nullptr;
JavaVM *g_jvm = nullptr;
bool g_debug_mode = false;

// Original function storage
std::map<std::string, void*> g_original_functions;
std::mutex g_functions_mutex;

/**
 * Store original function pointer for later use.
 *
 * @param key Unique identifier (e.g., "java.net.Socket.socketConnect0")
 * @param address Original function pointer
 */
void StoreOriginalFunction(const std::string& key, void* address) {
    std::lock_guard<std::mutex> lock(g_functions_mutex);
    g_original_functions[key] = address;
    DEBUG_LOGF("Stored original function: %s -> %p", key.c_str(), address);
}

/**
 * Get original function pointer.
 *
 * @param key Unique identifier
 * @return Original function pointer, or nullptr if not found
 */
void* GetOriginalFunction(const std::string& key) {
    std::lock_guard<std::mutex> lock(g_functions_mutex);
    auto it = g_original_functions.find(key);
    if (it != g_original_functions.end()) {
        return it->second;
    }
    return nullptr;
}

/**
 * Get JNI environment for current thread.
 *
 * @return JNIEnv* for current thread, or nullptr on error
 */
JNIEnv* GetJNIEnv() {
    if (g_jvm == nullptr) {
        return nullptr;
    }

    JNIEnv* env = nullptr;
    jint result = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_8);

    if (result == JNI_EDETACHED) {
        // Thread not attached, attach it
        result = g_jvm->AttachCurrentThread((void**)&env, nullptr);
        if (result != JNI_OK) {
            DEBUG_LOG("Failed to attach current thread");
            return nullptr;
        }
    }

    return env;
}

/**
 * Initialize JVMTI capabilities and callbacks.
 *
 * @param jvmti JVMTI environment
 * @return true if successful, false otherwise
 */
bool InitializeJVMTI(jvmtiEnv *jvmti) {
    DEBUG_LOG("Initializing JVMTI capabilities...");

    // Request JVMTI capabilities
    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(capabilities));
    capabilities.can_generate_native_method_bind_events = 1;

    jvmtiError error = jvmti->AddCapabilities(&capabilities);
    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "[JVMTI-Agent] ERROR: Failed to add capabilities: %d\n", error);
        return false;
    }

    DEBUG_LOG("JVMTI capabilities added successfully");

    // Register event callbacks
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.NativeMethodBind = &NativeMethodBindCallback;

    error = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "[JVMTI-Agent] ERROR: Failed to set event callbacks: %d\n", error);
        return false;
    }

    DEBUG_LOG("JVMTI event callbacks set successfully");

    // Enable native method bind events
    error = jvmti->SetEventNotificationMode(
        JVMTI_ENABLE,
        JVMTI_EVENT_NATIVE_METHOD_BIND,
        nullptr
    );

    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "[JVMTI-Agent] ERROR: Failed to enable native method bind events: %d\n", error);
        return false;
    }

    DEBUG_LOG("JVMTI native method bind events enabled");

    return true;
}

/**
 * JVMTI callback for native method binding.
 *
 * This is called when a native method is about to be bound to its implementation.
 * We can replace the function pointer here to intercept the call.
 *
 * @param jvmti_env JVMTI environment
 * @param jni_env JNI environment
 * @param thread Current thread
 * @param method Method being bound
 * @param address Original native function address
 * @param new_address_ptr Pointer to store replacement address
 */
void JNICALL NativeMethodBindCallback(
    jvmtiEnv *jvmti_env,
    JNIEnv* jni_env,
    jthread thread,
    jmethodID method,
    void* address,
    void** new_address_ptr
) {
    // Get method name and class name
    char* method_name = nullptr;
    char* method_signature = nullptr;
    char* class_signature = nullptr;
    jclass declaring_class = nullptr;

    jvmtiError error;

    // Get method info
    error = jvmti_env->GetMethodName(method, &method_name, &method_signature, nullptr);
    if (error != JVMTI_ERROR_NONE) {
        DEBUG_LOG("Failed to get method name");
        return;
    }

    // Get declaring class
    error = jvmti_env->GetMethodDeclaringClass(method, &declaring_class);
    if (error != JVMTI_ERROR_NONE) {
        DEBUG_LOG("Failed to get declaring class");
        jvmti_env->Deallocate((unsigned char*)method_name);
        jvmti_env->Deallocate((unsigned char*)method_signature);
        return;
    }

    // Get class signature
    error = jvmti_env->GetClassSignature(declaring_class, &class_signature, nullptr);
    if (error != JVMTI_ERROR_NONE) {
        DEBUG_LOG("Failed to get class signature");
        jvmti_env->Deallocate((unsigned char*)method_name);
        jvmti_env->Deallocate((unsigned char*)method_signature);
        return;
    }

    // Log the binding (for debugging)
    DEBUG_LOGF("Native method bind: %s.%s%s -> %p",
               class_signature, method_name, method_signature, address);

    // TODO: Check if this is a method we want to intercept
    // For now, just log it

    // Cleanup
    jvmti_env->Deallocate((unsigned char*)method_name);
    jvmti_env->Deallocate((unsigned char*)method_signature);
    jvmti_env->Deallocate((unsigned char*)class_signature);
}

/**
 * Agent entry point.
 *
 * Called when the agent is loaded via -agentpath or -agentlib.
 *
 * @param vm Java VM instance
 * @param options Agent options string (from -agentpath:path=options)
 * @param reserved Reserved for future use
 * @return JNI_OK on success, JNI_ERR on failure
 */
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    g_jvm = vm;

    // Parse options
    if (options != nullptr && strstr(options, "debug") != nullptr) {
        g_debug_mode = true;
    }

    DEBUG_LOG("JVMTI Agent loading...");

    // Get JVMTI environment
    jint result = vm->GetEnv((void**)&g_jvmti, JVMTI_VERSION_1_0);
    if (result != JNI_OK || g_jvmti == nullptr) {
        fprintf(stderr, "[JVMTI-Agent] ERROR: Failed to get JVMTI environment\n");
        return JNI_ERR;
    }

    DEBUG_LOG("JVMTI environment obtained");

    // Initialize JVMTI
    if (!InitializeJVMTI(g_jvmti)) {
        fprintf(stderr, "[JVMTI-Agent] ERROR: Failed to initialize JVMTI\n");
        return JNI_ERR;
    }

    fprintf(stderr, "[JVMTI-Agent] JVMTI Agent loaded successfully\n");
    return JNI_OK;
}

/**
 * Agent unload callback.
 *
 * Called when the JVM shuts down.
 *
 * @param vm Java VM instance
 */
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
    DEBUG_LOG("JVMTI Agent unloading...");
    g_jvmti = nullptr;
    g_jvm = nullptr;
}
