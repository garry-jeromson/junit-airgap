#ifndef JUNIT_NO_NETWORK_AGENT_H
#define JUNIT_NO_NETWORK_AGENT_H

#include <jvmti.h>
#include <jni.h>
#include <string>
#include <map>
#include <mutex>

// Debug logging
extern bool g_debug_mode;
#define DEBUG_LOG(msg) if (g_debug_mode) fprintf(stderr, "[junit-airgap:native] %s\n", msg)
#define DEBUG_LOGF(fmt, ...) if (g_debug_mode) fprintf(stderr, "[junit-airgap:native] " fmt "\n", __VA_ARGS__)

// Agent entry point
extern "C" {
    JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved);
    JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm);
}

// Global state
extern jvmtiEnv *g_jvmti;
extern JavaVM *g_jvm;

// Callbacks for JVMTI events
void JNICALL VMInitCallback(
    jvmtiEnv *jvmti_env,
    JNIEnv* jni_env,
    jthread thread
);

void JNICALL NativeMethodBindCallback(
    jvmtiEnv *jvmti_env,
    JNIEnv* jni_env,
    jthread thread,
    jmethodID method,
    void* address,
    void** new_address_ptr
);

// Initialize JVMTI capabilities and callbacks
bool InitializeJVMTI(jvmtiEnv *jvmti);

// Get JNI environment for current thread
JNIEnv* GetJNIEnv();

// Original function storage
extern std::map<std::string, void*> g_original_functions;
extern std::mutex g_functions_mutex;

void* GetOriginalFunction(const std::string& key);
void StoreOriginalFunction(const std::string& key, void* address);

// Socket interception functions
void* InstallNetConnect0Wrapper(void* original_address);

// DNS interception functions
void* InstallInet6LookupWrapper(void* original_address);
void* InstallInet4LookupWrapper(void* original_address);

// Cached NetworkBlockerContext class and method references
// These are set by registerWithAgent() called from Java
extern jclass g_network_blocker_context_class;
extern jmethodID g_check_connection_method;
extern jmethodID g_is_explicitly_blocked_method;
extern jmethodID g_has_active_configuration_method;
extern std::mutex g_context_mutex;

// Cached string constants (initialized during VM_INIT to avoid "platform encoding not initialized" errors)
// See: https://github.com/garry-jeromson/junit-airgap/issues/XXX
extern jstring g_caller_agent_string;  // "Native-Agent"
extern jstring g_caller_dns_string;    // "Native-DNS"
extern std::mutex g_strings_mutex;

// VM initialization state (true after VM_INIT callback completes)
// Used to guard JNI string operations that require platform encoding to be initialized
extern bool g_vm_init_complete;

// Get cached context class and method (thread-safe)
jclass GetNetworkBlockerContextClass();
jmethodID GetCheckConnectionMethod();
jmethodID GetIsExplicitlyBlockedMethod();
jmethodID GetHasActiveConfigurationMethod();

// Get cached string constants (thread-safe)
jstring GetCallerAgentString();
jstring GetCallerDnsString();

// Ensure platform encoding is ready for the current thread
// Returns true if ready, false if failed after retries
bool EnsurePlatformEncodingReady(JNIEnv* env);

// Registration function called from Java to cache class/method references
extern "C" {
    JNIEXPORT void JNICALL Java_io_github_garryjeromson_junit_airgap_bytebuddy_NetworkBlockerContext_registerWithAgent(
        JNIEnv* env,
        jclass clazz
    );
}

#endif // JUNIT_NO_NETWORK_AGENT_H
