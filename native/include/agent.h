#ifndef JUNIT_NO_NETWORK_AGENT_H
#define JUNIT_NO_NETWORK_AGENT_H

#include <jvmti.h>
#include <jni.h>
#include <string>
#include <map>
#include <mutex>

// Debug logging
extern bool g_debug_mode;
#define DEBUG_LOG(msg) if (g_debug_mode) fprintf(stderr, "[JVMTI-Agent] %s\n", msg)
#define DEBUG_LOGF(fmt, ...) if (g_debug_mode) fprintf(stderr, "[JVMTI-Agent] " fmt "\n", __VA_ARGS__)

// Agent entry point
extern "C" {
    JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved);
    JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm);
}

// Global state
extern jvmtiEnv *g_jvmti;
extern JavaVM *g_jvm;

// Callback for native method binding
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

// Cached NetworkBlockerContext class and method references
// These are set by registerWithAgent() called from Java
extern jclass g_network_blocker_context_class;
extern jmethodID g_check_connection_method;
extern std::mutex g_context_mutex;

// Get cached context class and method (thread-safe)
jclass GetNetworkBlockerContextClass();
jmethodID GetCheckConnectionMethod();

// Registration function called from Java to cache class/method references
extern "C" {
    JNIEXPORT void JNICALL Java_io_github_garryjeromson_junit_nonetwork_bytebuddy_NetworkBlockerContext_registerWithAgent(
        JNIEnv* env,
        jclass clazz
    );
}

#endif // JUNIT_NO_NETWORK_AGENT_H
