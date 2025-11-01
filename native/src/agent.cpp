/**
 * JVMTI Agent for junit-airgap
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
#include <unistd.h>  // for usleep()

// Global state
jvmtiEnv *g_jvmti = nullptr;
JavaVM *g_jvm = nullptr;
bool g_debug_mode = false;

// Original function storage
std::map<std::string, void*> g_original_functions;
std::mutex g_functions_mutex;

// Cached NetworkBlockerContext class and method references
jclass g_network_blocker_context_class = nullptr;
jmethodID g_check_connection_method = nullptr;
jmethodID g_is_explicitly_blocked_method = nullptr;
jmethodID g_has_active_configuration_method = nullptr;
std::mutex g_context_mutex;

// Cached string constants (initialized during VM_INIT)
jstring g_caller_agent_string = nullptr;
jstring g_caller_dns_string = nullptr;
std::mutex g_strings_mutex;

// VM initialization state
bool g_vm_init_complete = false;

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
        fprintf(stderr, "[junit-airgap:native] ERROR: Failed to add capabilities: %d\n", error);
        return false;
    }

    DEBUG_LOG("JVMTI capabilities added successfully");

    // Register event callbacks
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.NativeMethodBind = &NativeMethodBindCallback;
    callbacks.VMInit = &VMInitCallback;

    error = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "[junit-airgap:native] ERROR: Failed to set event callbacks: %d\n", error);
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
        fprintf(stderr, "[junit-airgap:native] ERROR: Failed to enable native method bind events: %d\n", error);
        return false;
    }

    DEBUG_LOG("JVMTI native method bind events enabled");

    // Enable VM_INIT event to initialize string constants when JVM is fully initialized
    error = jvmti->SetEventNotificationMode(
        JVMTI_ENABLE,
        JVMTI_EVENT_VM_INIT,
        nullptr
    );

    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "[junit-airgap:native] ERROR: Failed to enable VM_INIT event: %d\n", error);
        return false;
    }

    DEBUG_LOG("JVMTI VM_INIT event enabled");

    return true;
}

/**
 * JVMTI callback for VM initialization.
 *
 * This is called when the JVM is fully initialized and ready to run Java code.
 * We use this to initialize string constants that require platform encoding,
 * which may not be available during earlier initialization phases.
 *
 * This fixes "platform encoding not initialized" errors when running tests
 * via IntelliJ IDEA, which loads classes earlier than command-line Gradle.
 *
 * @param jvmti_env JVMTI environment
 * @param jni_env JNI environment
 * @param thread Current thread
 */
void JNICALL VMInitCallback(
    jvmtiEnv *jvmti_env,
    JNIEnv* jni_env,
    jthread thread
) {
    DEBUG_LOG("VM_INIT callback - initializing cached string constants");

    std::lock_guard<std::mutex> lock(g_strings_mutex);

    // Create "Native-Agent" string constant (caller identifier for NetworkBlockerContext)
    if (g_caller_agent_string == nullptr) {
        jstring local_agent = jni_env->NewStringUTF("Native-Agent");
        if (local_agent != nullptr) {
            g_caller_agent_string = (jstring)jni_env->NewGlobalRef(local_agent);
            jni_env->DeleteLocalRef(local_agent);
            DEBUG_LOG("Cached caller agent string: Native-Agent");
        } else {
            fprintf(stderr, "[junit-airgap:native] ERROR: Failed to create caller agent string\n");
        }
    }

    // Create "Native-DNS" string constant
    if (g_caller_dns_string == nullptr) {
        jstring local_dns = jni_env->NewStringUTF("Native-DNS");
        if (local_dns != nullptr) {
            g_caller_dns_string = (jstring)jni_env->NewGlobalRef(local_dns);
            jni_env->DeleteLocalRef(local_dns);
            DEBUG_LOG("Cached caller DNS string: Native-DNS");
        } else {
            fprintf(stderr, "[junit-airgap:native] ERROR: Failed to create caller DNS string\n");
        }
    }

    DEBUG_LOG("String constants initialized successfully");

    // Eagerly initialize platform encoding for string extraction (GetStringUTFChars)
    // This ensures platform encoding is fully ready before we allow any interception
    //
    // IMPORTANT: In Android Studio's test runner, platform encoding may not be fully
    // ready immediately after VM_INIT. We need to poll/retry until it's actually working.
    // Without this, tests with @AllowNetworkRequests fail with "platform encoding not initialized"
    // because even the JVM's own DNS code can't run yet.
    bool encoding_ready = false;
    int max_attempts = 50;  // Try for ~500ms (50 attempts * 10ms sleep)

    for (int attempt = 0; attempt < max_attempts && !encoding_ready; attempt++) {
        if (g_caller_agent_string != nullptr) {
            const char* test_str = jni_env->GetStringUTFChars(g_caller_agent_string, nullptr);
            if (test_str != nullptr) {
                if (attempt > 0) {
                    DEBUG_LOGF("Platform encoding ready after %d attempts", attempt + 1);
                }
                jni_env->ReleaseStringUTFChars(g_caller_agent_string, test_str);
                encoding_ready = true;
            } else {
                // Clear any exception from the failed GetStringUTFChars
                if (jni_env->ExceptionCheck()) {
                    jni_env->ExceptionClear();
                }

                if (attempt < max_attempts - 1) {
                    // Sleep for 10ms before retrying (usleep takes microseconds)
                    usleep(10000);
                } else {
                    fprintf(stderr, "[junit-airgap:native] WARNING: Platform encoding still not ready after %d attempts\n", max_attempts);
                }
            }
        }
    }

    if (!encoding_ready) {
        fprintf(stderr, "[junit-airgap:native] WARNING: Proceeding without confirmed platform encoding readiness\n");
        fprintf(stderr, "[junit-airgap:native] WARNING: String operations may fail with 'platform encoding not initialized' errors\n");
    }

    // Mark VM as fully initialized - platform encoding is now ready (or we've waited long enough)
    // After this point, all JNI string operations (GetStringUTFChars, etc.) should be safe
    g_vm_init_complete = true;
    DEBUG_LOG("VM initialization complete - all JNI operations now safe");

    // CRITICAL FIX: Check if DNS native methods were bound before agent initialization
    // If they were, we have a fundamental limitation: JVMTI can't rebind native methods
    // after they're already bound. The only solution is to use ByteBuddy at the Java layer.
    DEBUG_LOG("Checking DNS native method interception status...");

    // Check if we successfully intercepted DNS methods during Agent_OnLoad
    bool hasInet6Wrapper = (GetOriginalFunction("java.net.Inet6AddressImpl.lookupAllHostAddr") != nullptr);
    bool hasInet4Wrapper = (GetOriginalFunction("java.net.Inet4AddressImpl.lookupAllHostAddr") != nullptr);

    if (hasInet6Wrapper) {
        DEBUG_LOG("Inet6AddressImpl.lookupAllHostAddr() successfully intercepted");
    } else {
        DEBUG_LOG("Inet6AddressImpl.lookupAllHostAddr() was not intercepted (DNS methods bound before agent initialization)");
        DEBUG_LOG("ByteBuddy agent will handle DNS interception as fallback");
    }

    if (hasInet4Wrapper) {
        DEBUG_LOG("Inet4AddressImpl.lookupAllHostAddr() successfully intercepted");
    }

    DEBUG_LOG("DNS native method interception check complete");
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

    // Check if this is sun.nio.ch.Net.connect0() - used by modern Socket implementation
    if (strcmp(class_signature, "Lsun/nio/ch/Net;") == 0 &&
        strcmp(method_name, "connect0") == 0) {

        DEBUG_LOG("Intercepted sun.nio.ch.Net.connect0() binding");

        // Store original function pointer
        std::string key = "sun.nio.ch.Net.connect0";
        StoreOriginalFunction(key, address);

        // Replace with wrapper function
        void* wrapper_address = InstallNetConnect0Wrapper(address);
        *new_address_ptr = wrapper_address;

        DEBUG_LOGF("Replaced Net.connect0() with wrapper at %p", wrapper_address);
    }

    // Check if this is Socket.socketConnect0() (legacy, pre-Java 7)
    if (strcmp(class_signature, "Ljava/net/Socket;") == 0 &&
        strcmp(method_name, "socketConnect0") == 0) {

        DEBUG_LOG("Intercepted Socket.socketConnect0() binding");

        // Store original function pointer
        std::string key = "java.net.Socket.socketConnect0";
        StoreOriginalFunction(key, address);
    }

    // Check if this is SocketChannelImpl.connect0()
    if (strcmp(class_signature, "Lsun/nio/ch/SocketChannelImpl;") == 0 &&
        strcmp(method_name, "connect0") == 0) {

        DEBUG_LOG("Intercepted SocketChannel.connect0() binding");

        // Store original function pointer
        std::string key = "sun.nio.ch.SocketChannelImpl.connect0";
        StoreOriginalFunction(key, address);
    }

    // Check if this is Inet6AddressImpl.lookupAllHostAddr() - DNS resolution
    if (strcmp(class_signature, "Ljava/net/Inet6AddressImpl;") == 0 &&
        strcmp(method_name, "lookupAllHostAddr") == 0) {

        DEBUG_LOG("Intercepted Inet6AddressImpl.lookupAllHostAddr() binding");

        // Store original function pointer
        std::string key = "java.net.Inet6AddressImpl.lookupAllHostAddr";
        StoreOriginalFunction(key, address);

        // Replace with wrapper function
        void* wrapper_address = InstallInet6LookupWrapper(address);
        *new_address_ptr = wrapper_address;

        DEBUG_LOGF("Replaced Inet6AddressImpl.lookupAllHostAddr() with wrapper at %p", wrapper_address);
    }

    // Check if this is Inet4AddressImpl.lookupAllHostAddr() - DNS resolution
    if (strcmp(class_signature, "Ljava/net/Inet4AddressImpl;") == 0 &&
        strcmp(method_name, "lookupAllHostAddr") == 0) {

        DEBUG_LOG("Intercepted Inet4AddressImpl.lookupAllHostAddr() binding");

        // Store original function pointer
        std::string key = "java.net.Inet4AddressImpl.lookupAllHostAddr";
        StoreOriginalFunction(key, address);

        // Replace with wrapper function
        void* wrapper_address = InstallInet4LookupWrapper(address);
        *new_address_ptr = wrapper_address;

        DEBUG_LOGF("Replaced Inet4AddressImpl.lookupAllHostAddr() with wrapper at %p", wrapper_address);
    }

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

    // Display version banner (only in debug mode)
    if (g_debug_mode) {
        fprintf(stderr, "\n");
        fprintf(stderr, "================================================================================\n");
        fprintf(stderr, "[junit-airgap:native] junit-airgap Native Agent\n");
        fprintf(stderr, "[junit-airgap:native] Version: 2024-10-31 (platform encoding fix)\n");
        fprintf(stderr, "[junit-airgap:native] Build time: %s %s\n", __DATE__, __TIME__);
        fprintf(stderr, "================================================================================\n");
        fprintf(stderr, "\n");
    }

    DEBUG_LOG("JVMTI Agent loading...");

    // Get JVMTI environment
    jint result = vm->GetEnv((void**)&g_jvmti, JVMTI_VERSION_1_0);
    if (result != JNI_OK || g_jvmti == nullptr) {
        fprintf(stderr, "[junit-airgap:native] ERROR: Failed to get JVMTI environment\n");
        return JNI_ERR;
    }

    DEBUG_LOG("JVMTI environment obtained");

    // Initialize JVMTI
    if (!InitializeJVMTI(g_jvmti)) {
        fprintf(stderr, "[junit-airgap:native] ERROR: Failed to initialize JVMTI\n");
        return JNI_ERR;
    }

    DEBUG_LOG("JVMTI Agent loaded successfully");
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

    // Clean up global references
    if (g_network_blocker_context_class != nullptr) {
        JNIEnv* env = GetJNIEnv();
        if (env != nullptr) {
            env->DeleteGlobalRef(g_network_blocker_context_class);
            g_network_blocker_context_class = nullptr;
        }
    }

    g_jvmti = nullptr;
    g_jvm = nullptr;
}

/**
 * Get cached NetworkBlockerContext class reference.
 *
 * @return Cached class reference, or nullptr if not registered
 */
jclass GetNetworkBlockerContextClass() {
    std::lock_guard<std::mutex> lock(g_context_mutex);
    return g_network_blocker_context_class;
}

/**
 * Get cached checkConnection method ID.
 *
 * @return Cached method ID, or nullptr if not registered
 */
jmethodID GetCheckConnectionMethod() {
    std::lock_guard<std::mutex> lock(g_context_mutex);
    return g_check_connection_method;
}

/**
 * Get cached isExplicitlyBlocked method ID.
 *
 * @return Cached method ID, or nullptr if not registered
 */
jmethodID GetIsExplicitlyBlockedMethod() {
    std::lock_guard<std::mutex> lock(g_context_mutex);
    return g_is_explicitly_blocked_method;
}

/**
 * Get cached hasActiveConfiguration method.
 *
 * @return Cached method ID, or nullptr if not registered
 */
jmethodID GetHasActiveConfigurationMethod() {
    std::lock_guard<std::mutex> lock(g_context_mutex);
    return g_has_active_configuration_method;
}

/**
 * Get cached caller agent string constant.
 *
 * @return Cached "Native-Agent" string, or nullptr if not initialized
 */
jstring GetCallerAgentString() {
    std::lock_guard<std::mutex> lock(g_strings_mutex);
    return g_caller_agent_string;
}

/**
 * Get cached caller DNS string constant.
 *
 * @return Cached "Native-DNS" string, or nullptr if not initialized
 */
jstring GetCallerDnsString() {
    std::lock_guard<std::mutex> lock(g_strings_mutex);
    return g_caller_dns_string;
}

/**
 * Ensure platform encoding is ready for the current thread.
 *
 * Platform encoding initialization is per-thread in the JVM. When a new thread
 * (like Android Studio's "Test worker") is created, platform encoding may not be
 * ready immediately, even if VM_INIT has completed and NetworkBlockerContext is registered.
 *
 * This function triggers platform encoding initialization by attempting a simple
 * string operation (GetStringUTFChars) and retrying if it fails.
 *
 * @param env JNI environment for current thread
 * @return true if platform encoding is ready, false if failed after retries
 */
bool EnsurePlatformEncodingReady(JNIEnv* env) {
    // Try to use a cached string to trigger platform encoding initialization
    jstring test_string = GetCallerAgentString();
    if (test_string == nullptr) {
        return false;
    }

    int max_attempts = 100;  // Up to 5 seconds (100 attempts * 50ms)
    for (int attempt = 0; attempt < max_attempts; attempt++) {
        const char* test_chars = env->GetStringUTFChars(test_string, nullptr);

        if (test_chars != nullptr) {
            // Success! Platform encoding is ready
            if (attempt > 0) {
                DEBUG_LOGF("Platform encoding ready after %d attempt(s)", attempt + 1);
            }
            env->ReleaseStringUTFChars(test_string, test_chars);
            return true;
        }

        // Failed - check if it's a platform encoding error
        if (env->ExceptionCheck()) {
            jthrowable exception = env->ExceptionOccurred();
            env->ExceptionClear();

            // Check if it's InternalError (platform encoding error)
            jclass errorClass = env->FindClass("java/lang/InternalError");
            if (errorClass != nullptr && env->IsInstanceOf(exception, errorClass)) {
                if (attempt < max_attempts - 1) {
                    usleep(50000);  // Sleep 50ms
                    continue;
                } else {
                    DEBUG_LOG("Platform encoding still not ready after retries");
                    return false;
                }
            } else {
                // Different error - not a platform encoding issue
                env->Throw(exception);  // Re-throw
                return false;
            }
        }
    }

    return false;
}

/**
 * Registration function called from Java to cache class and method references.
 *
 * This is called from NetworkBlockerContext's static initializer to register
 * itself with the JVMTI agent. We cache global references to avoid FindClass
 * issues from native method contexts.
 *
 * Java signature: private external fun registerWithAgent()
 * JNI signature: ()V
 *
 * @param env JNI environment
 * @param clazz NetworkBlockerContext class
 */
JNIEXPORT void JNICALL Java_io_github_garryjeromson_junit_airgap_bytebuddy_NetworkBlockerContext_registerWithAgent(
    JNIEnv* env,
    jclass clazz
) {
    std::lock_guard<std::mutex> lock(g_context_mutex);

    DEBUG_LOG("Registering NetworkBlockerContext with JVMTI agent...");

    // Create global reference to the class
    // (local reference will be invalid after this function returns)
    g_network_blocker_context_class = (jclass)env->NewGlobalRef(clazz);

    if (g_network_blocker_context_class == nullptr) {
        fprintf(stderr, "[junit-airgap:native] ERROR: Failed to create global reference to NetworkBlockerContext\n");
        return;
    }

    // Get checkConnection method
    g_check_connection_method = env->GetStaticMethodID(
        g_network_blocker_context_class,
        "checkConnection",
        "(Ljava/lang/String;ILjava/lang/String;)V"
    );

    if (g_check_connection_method == nullptr) {
        fprintf(stderr, "[junit-airgap:native] ERROR: Failed to find checkConnection method\n");
        if (env->ExceptionCheck()) {
            fprintf(stderr, "[junit-airgap:native] JNI Exception occurred:\n");
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        env->DeleteGlobalRef(g_network_blocker_context_class);
        g_network_blocker_context_class = nullptr;
        return;
    }

    // Get isExplicitlyBlocked method
    g_is_explicitly_blocked_method = env->GetStaticMethodID(
        g_network_blocker_context_class,
        "isExplicitlyBlocked",
        "(Ljava/lang/String;)Z"
    );

    if (g_is_explicitly_blocked_method == nullptr) {
        fprintf(stderr, "[junit-airgap:native] ERROR: Failed to find isExplicitlyBlocked method\n");
        if (env->ExceptionCheck()) {
            fprintf(stderr, "[junit-airgap:native] JNI Exception occurred:\n");
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        env->DeleteGlobalRef(g_network_blocker_context_class);
        g_network_blocker_context_class = nullptr;
        g_check_connection_method = nullptr;
        return;
    }

    // Get hasActiveConfiguration method
    g_has_active_configuration_method = env->GetStaticMethodID(
        g_network_blocker_context_class,
        "hasActiveConfiguration",
        "()Z"
    );

    if (g_has_active_configuration_method == nullptr) {
        fprintf(stderr, "[junit-airgap:native] ERROR: Failed to find hasActiveConfiguration method\n");
        // Check if there's a pending JNI exception that explains the failure
        if (env->ExceptionCheck()) {
            fprintf(stderr, "[junit-airgap:native] JNI Exception occurred:\n");
            env->ExceptionDescribe();  // Prints the exception to stderr
            env->ExceptionClear();     // Clear the exception
        }
        env->DeleteGlobalRef(g_network_blocker_context_class);
        g_network_blocker_context_class = nullptr;
        g_check_connection_method = nullptr;
        g_is_explicitly_blocked_method = nullptr;
        return;
    }

    DEBUG_LOG("NetworkBlockerContext registered - network blocking enabled");
}
