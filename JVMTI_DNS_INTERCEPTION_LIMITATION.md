# JVMTI DNS Interception Limitation

## Problem

Tests with `@AllowNetworkRequests` fail in Android Studio with:

```
platform encoding not initialized
java.lang.InternalError: platform encoding not initialized
	at java.base/java.net.Inet6AddressImpl.lookupAllHostAddr(Native Method)
```

## Root Cause

The JVMTI `NativeMethodBindCallback` has a fundamental limitation:

1. **Callback Timing**: The callback is only triggered when a native method is bound **for the first time** AND **after `Agent_OnLoad` completes**

2. **Early Class Loading**: In Android Studio's test runner (and some JVM configurations), DNS implementation classes (`Inet6AddressImpl`, `Inet4AddressImpl`) are loaded during the JVM's early initialization phase, **BEFORE** the JVMTI agent finishes loading

3. **No Retroactive Interception**: Once a native method is bound to its implementation, JVMTI cannot retroactively wrap it. The binding is permanent for that JVM instance.

4. **Bypass**: DNS calls bypass our JVMTI wrapper entirely → hit unwrapped native function → platform encoding not ready yet → `InternalError`

## Evidence from Logs

### First JVM (Command Line - Working)
```
Line 48:  [JVMTI-Agent] Intercepted Inet6AddressImpl.lookupAllHostAddr() binding
Line 52:  [JVMTI-Agent] [WRAPPER-ENTRY] wrapped_lookupAllHostAddr(Inet6AddressImpl) called
Line 60:  [JVMTI-Agent] Platform encoding ready - calling original DNS function
Line 272: [JVMTI-Agent] VM_INIT callback - initializing cached string constants
```
✅ DNS class bound AFTER agent loaded → Wrapper installed → Works

### Second JVM (Android Studio - Failing)
```
Line 74:  [JVMTI-Agent] JVMTI Agent loading...
Line 81:  [JVMTI-Agent] JVMTI Agent loaded successfully
Line 272: [JVMTI-Agent] VM_INIT callback - initializing cached string constants
Line 278: [JVMTI-Agent] Force-loading DNS implementation classes...
Line 281: [JVMTI-Agent] Inet6AddressImpl loaded successfully
Line 392: [junit-airgap] NetworkBlockerContext.hasActiveConfiguration() = false
Line 394: platform encoding not initialized
```
❌ NO "Intercepted" message → DNS class already bound → No wrapper → Error
❌ NO "[WRAPPER-ENTRY]" message before error → Wrapper not called

## Why Command-Line Tests Pass

Gradle's test executor reuses JVMs by default:
- **First JVM**: DNS classes loaded after agent → Wrappers installed ✅
- **Subsequent tests**: Reuse same JVM with working wrappers ✅

Android Studio spawns fresh JVMs for each test suite, hitting the timing issue every time.

## Why Force-Loading Doesn't Work

The fix attempted in `VMInitCallback`:
```cpp
jclass inet6Class = jni_env->FindClass("java/net/Inet6AddressImpl");
```

This finds the **already-loaded** class but doesn't trigger native method binding because:
1. The class was loaded before VM_INIT
2. `FindClass` doesn't re-bind native methods
3. Native methods are bound exactly once per JVM lifetime

## Attempted Solutions (Failed)

### 1. Force-Loading in VM_INIT
**Problem**: Classes already loaded by this point, native methods already bound

### 2. Retry/Polling Platform Encoding
**Problem**: Wrapper never called, so retry logic never executes

### 3. Early Warning Detection
**Implemented**: Detects the issue but can't fix it
```cpp
bool hasInet6Wrapper = (GetOriginalFunction("java.net.Inet6AddressImpl.lookupAllHostAddr") != nullptr);
if (!hasInet6Wrapper) {
    fprintf(stderr, "[JVMTI-Agent] ERROR: DNS Interception Failed\n");
}
```

## The Only Real Solution

Implement **ByteBuddy-based Java-layer interception** as a fallback mechanism:

### Architecture

```
┌─────────────────────────────────────────┐
│  Application Code                       │
│  InetAddress.getAllByName("example.com")│
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  ByteBuddy Interceptor (Java Layer)     │  ◄─── New Fallback
│  InetAddressInterceptor.interceptCall() │
│  - Always works                          │
│  - Intercepts before native call         │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  Native Method Binding                   │
│  Inet6AddressImpl.lookupAllHostAddr()    │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  JVMTI Wrapper (Native Layer)            │  ◄─── Current Approach
│  wrapped_lookupAllHostAddr()             │       (Only works if bound after agent loads)
│  - Only works if bound after agent loads │
│  - Cannot retroactively intercept        │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  Original Native Implementation          │
│  (JVM's DNS resolution code)             │
└─────────────────────────────────────────┘
```

### Implementation Steps

1. **Create ByteBuddy Transformer**
   - File already created: `InetAddressInterceptor.kt`
   - Intercepts `InetAddress.getAllByName(String)`
   - Intercepts `InetAddress.getByName(String)`

2. **Set Up ByteBuddy Java Agent**
   - Create `InetAddressByteB uddyAgent.kt`
   - Register transformer with ByteBuddy's `AgentBuilder`
   - Use `Advice.OnMethodEnter` to inject interceptor call

3. **Load ByteBuddy Agent**
   - Add `-javaagent` alongside existing `-agentpath`
   - Gradle plugin needs to extract and load ByteBuddy agent JAR
   - Configure agent to transform `java.net.InetAddress` class

4. **Fallback Logic**
   - JVMTI wrapper: Fast path (direct native interception)
   - ByteBuddy wrapper: Fallback (works when JVMTI misses)
   - Both check `NetworkBlockerContext` before allowing DNS

### Why ByteBuddy Works

1. **Class Transform Time**: ByteBuddy transforms classes when they're loaded, BEFORE native methods are resolved
2. **Java Layer**: Intercepts public API calls, not native method bindings
3. **No Timing Dependency**: Works regardless of when DNS classes are loaded
4. **Platform Independent**: Pure Java, no JVM internal dependencies

### Example ByteBuddy Advice

```kotlin
object InetAddressAdvice {
    @Advice.OnMethodEnter
    @JvmStatic
    fun interceptGetAllByName(
        @Advice.Argument(0) host: String?
    ) {
        if (host != null) {
            NetworkBlockerContext.checkConnection(
                host = host,
                port = -1,  // DNS lookup, not socket
                caller = "ByteBuddy-DNS"
            )
        }
    }
}
```

### Complexity Assessment

- **Code Changes**: Moderate (new ByteBuddy agent infrastructure)
- **Testing**: Significant (ensure no conflicts with JVMTI agent)
- **Performance**: Minimal overhead (advice is inlined by JIT)
- **Compatibility**: Excellent (works on all Java versions)

## Temporary Workarounds

### For Users

1. **Disable Test Forking** in Android Studio
   - Reuses JVMs → JVMTI wrappers work
   - Not always possible/desirable

2. **Use Command Line**
   ```bash
   ./gradlew test  # Works due to JVM reuse
   ```

3. **Accept Limitation**
   - Document that `@AllowNetworkRequests` may fail in IDE test runners
   - Not acceptable for production library

### For Developers

1. **Detect and Warn**
   - Already implemented in `VMInitCallback`
   - Helps diagnose the issue but doesn't fix it

2. **Recommend Alternative Approaches**
   - Use `@BlockNetworkRequests` with explicit allowlists
   - Avoids the platform encoding issue

## Timeline

### Completed
- ✅ Root cause analysis
- ✅ JVMTI limitation documented
- ✅ Detection logic implemented
- ✅ InetAddressInterceptor.kt created

### Required for Fix
- ⏳ ByteBuddy agent infrastructure
- ⏳ Agent registration and loading
- ⏳ Integration testing
- ⏳ Performance testing
- ⏳ Documentation updates

## References

- JVMTI Spec: https://docs.oracle.com/en/java/javase/21/docs/specs/jvmti.html
- NativeMethodBind Event: Only fires when method is first bound after agent loads
- ByteBuddy Docs: https://bytebuddy.net/
- Related Issues:
  - Similar problems in other JVMTI-based tools (JaCoCo, etc.)
  - Common workaround: Combine JVMTI + ByteBuddy

## Decision

**Recommendation**: Implement ByteBuddy fallback for DNS interception

**Priority**: High - Blocks Android Studio testing workflow

**Effort**: 2-3 days for implementation + testing

**Alternative**: Document limitation and provide workarounds (Not recommended - poor UX)
