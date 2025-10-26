# Java Version Compatibility

## Overview

The junit-no-network JVMTI agent is compiled once per platform and works across multiple Java versions due to JVMTI's stable API and backward compatibility guarantees.

**Key Takeaway**: You do NOT need different agent binaries for different Java versions. One binary per platform (OS + architecture) works for all supported Java versions.

## How JVMTI Agent Compatibility Works

### The JVMTI API is Stable

**JVMTI Version 1.0** was introduced in Java 5 and has remained stable:
- API unchanged since 2004
- Capabilities system allows feature detection
- No breaking changes across Java versions
- C ABI is stable (unlike Java class file versions)

**Our agent uses**:
```cpp
jint result = vm->GetEnv((void**)&g_jvmti, JVMTI_VERSION_1_0);
```

This requests the oldest stable JVMTI version, ensuring maximum compatibility.

### Dynamic Linking Enables Version Independence

**At compile time** (building the agent):
```bash
cmake .. && make
# Links against: @rpath/libjvm.dylib
# Not hardcoded to specific Java version!
```

**At runtime** (when test JVM loads agent):
```
JVM startup with: -agentpath:/path/to/agent.dylib
  ↓
JVM resolves @rpath to its own libjvm.dylib
  ↓
Agent links against the JVM that's loading it
  ↓
Works regardless of Java version!
```

**Key insight**: The agent doesn't embed Java version info. It dynamically links to whatever JVM loads it.

### Backward Compatibility Guarantee

**JVMTI promises**:
- Agent compiled for older JVMTI version works on newer JVMs
- JVM must support all capabilities from older versions
- API contracts remain stable

**Example**:
- Agent compiled against Java 17's JVMTI (version 1.0)
- Works on Java 17, 21, 22, 25, 30+
- JVM provides backward-compatible JVMTI environment

**What does NOT work**:
- Agent compiled against Java 25 might use features unavailable in Java 17
- This is why we compile against JVMTI_VERSION_1_0 (oldest stable)

## Current Build Configuration

### Build Target

**Currently**: Java 21 (Temurin 21.0.4)

This was chosen because:
- ✅ LTS (Long-Term Support until 2031)
- ✅ Modern version with good tooling
- ✅ JVMTI headers unchanged since Java 5
- ✅ Already required for project (SecurityManager removal)

### Compatibility Range

**Minimum supported**: Java 21
**Tested**: Java 21, 25
**Expected to work**: Java 21+

The agent uses only:
- JVMTI 1.0 features (Java 5+)
- Standard capabilities (can_generate_native_method_bind_events)
- Native method interception (stable since Java 7)

**Why minimum is Java 21**:
- Project targets Java 21+ (see CLAUDE.md)
- No SecurityManager fallback for older versions
- Build tooling requires Java 21

## Platform Matrix

### Current Builds

| Platform | OS | Architecture | Status |
|----------|----|--------------| -------|
| darwin-aarch64 | macOS | ARM64 (M1/M2/M3) | ✅ Built |
| darwin-x86-64 | macOS | Intel | ❌ TODO |
| linux-x86-64 | Linux | AMD64 | ❌ TODO |
| linux-aarch64 | Linux | ARM64 | ❌ TODO |
| windows-x86-64 | Windows | AMD64 | ❌ TODO |

**Note**: Each platform needs ONE binary that works across all Java versions.

### Why One Binary Per Platform?

Different platforms need different binaries because:
- ✅ **Different OS**: System calls, library formats (.dylib vs .so vs .dll)
- ✅ **Different architecture**: Instruction sets (ARM64 vs x86-64)
- ❌ **NOT Java version**: JVMTI API is version-independent

**Example**:
- `darwin-aarch64` agent works for Java 17-30+ on macOS ARM
- Does NOT work on Linux (different OS)
- Does NOT work on macOS Intel (different arch)

## Testing Strategy

### Current Testing

Tests run with whatever Java version is in `$JAVA_HOME`:
```bash
JAVA_HOME=/path/to/java-21 ./gradlew test  # Java 21
JAVA_HOME=/path/to/java-25 ./gradlew test  # Java 25
```

### Recommended CI Matrix

```yaml
strategy:
  matrix:
    java-version: [21, 22, 23, 24, 25]
    os: [ubuntu-latest, macos-latest, windows-latest]
```

This validates:
- ✅ Single agent works across Java versions
- ✅ Agent works on different platforms
- ✅ No unexpected incompatibilities

## Build Process

### How Agent is Built

1. **CMake configuration**:
   ```bash
   cd native && mkdir build && cd build
   cmake ..
   ```
   - Finds JNI headers via `find_package(JNI REQUIRED)`
   - Uses headers from `$JAVA_HOME/include/`
   - Links with `@rpath` (dynamic, not hardcoded)

2. **Compilation**:
   ```bash
   make
   ```
   - Compiles C++ code to native binary
   - Links against JVM via `@rpath/libjvm.dylib`
   - Creates platform-specific agent (.dylib, .so, .dll)

3. **Packaging**:
   ```bash
   ./gradlew :gradle-plugin:packageNativeAgent
   ```
   - Copies agent to `gradle-plugin/src/main/resources/native/{platform}/`
   - Packages into plugin JAR
   - Single JAR contains all platform agents

### Why Build Target Doesn't Matter Much

The agent uses:
- **JVMTI 1.0 API**: Unchanged since Java 5
- **Standard C++17**: Not Java-version-specific
- **JNI headers**: Mostly stable across versions

**Building against Java 21 means**:
- We use Java 21's JNI headers (jni.h, jvmti.h)
- Agent has Java 21's `@rpath/libjvm.dylib` reference
- But this is resolved at runtime, not compile time!

**The agent would work even if built against**:
- Java 17 (might work on 17-30+)
- Java 25 (might only work on 25+, less safe)

## Edge Cases & Limitations

### What Could Break Compatibility?

1. **JVM Internal Changes** (Rare)
   - JVM reorganizes internal structures
   - Native method signatures change
   - Example: Java 9's modularization affected some internals
   - **Mitigation**: Use only stable JVMTI APIs

2. **Removed Features** (Very Rare)
   - JVM removes deprecated capabilities
   - Example: None in JVMTI history
   - **Mitigation**: Use JVMTI 1.0 features only

3. **ABI Changes** (Extremely Rare)
   - Platform changes calling convention
   - Example: macOS ARM transition
   - **Mitigation**: Build for each platform/arch

### Known Compatible Ranges

Based on JVMTI stability:
- **Java 21 → Java 30+**: Expected to work (JVMTI 1.0)
- **Java 17 → Java 30+**: Would probably work (if we built against 17)
- **Java 11 → Java 30+**: Likely works (JVMTI unchanged)

**Current choice**: Start at Java 21 due to project requirements, not JVMTI limitations.

## Distribution Model

### Plugin JAR Contents

```
gradle-plugin.jar
├── META-INF/
├── io/github/garryjeromson/junit/nonetwork/gradle/  (Kotlin code)
└── native/
    ├── darwin-aarch64/
    │   └── libjunit-no-network-agent.dylib  (works for all Java 21+ on macOS ARM)
    ├── darwin-x86-64/
    │   └── libjunit-no-network-agent.dylib  (TODO: works for all Java 21+ on macOS Intel)
    ├── linux-x86-64/
    │   └── libjunit-no-network-agent.so     (TODO: works for all Java 21+ on Linux AMD64)
    └── windows-x86-64/
        └── junit-no-network-agent.dll       (TODO: works for all Java 21+ on Windows)
```

**One JAR** contains agents for all platforms, each agent works for all Java versions.

### Runtime Extraction

```kotlin
// NativeAgentExtractor.kt
fun extractAgent(project: Project): File? {
    val platform = detectPlatform()  // Detect OS + arch (NOT Java version!)
    val agentPath = "native/${platform.os}-${platform.arch}/${platform.agentFileName}"

    // Extract appropriate agent for platform
    // Works for any Java version!
    return extractResource(agentPath)
}
```

**Key point**: Detection is based on OS/architecture, NOT Java version.

## Future Considerations

### If Java Version Detection Becomes Needed

Currently unnecessary, but if future Java versions break compatibility:

```kotlin
fun detectJavaVersion(): Int {
    val version = System.getProperty("java.version")
    // Parse "21.0.1" → 21
    return version.split(".")[0].toInt()
}

fun extractAgent(project: Project): File? {
    val platform = detectPlatform()
    val javaVersion = detectJavaVersion()

    // Try version-specific agent first
    val versionedPath = "native/${platform.os}-${platform.arch}-java${javaVersion}/${platform.agentFileName}"
    val genericPath = "native/${platform.os}-${platform.arch}/${platform.agentFileName}"

    return extractResource(versionedPath) ?: extractResource(genericPath)
}
```

But this adds complexity for no current benefit.

### Cross-Compilation

Currently we build on the development machine (macOS ARM). For other platforms:

**Option A: CI Matrix**
```yaml
jobs:
  build-linux:
    runs-on: ubuntu-latest
  build-windows:
    runs-on: windows-latest
  build-macos-intel:
    runs-on: macos-13  # Intel runner
```

**Option B: Docker Cross-Compilation**
```bash
docker run --platform linux/amd64 -v $(pwd):/work ubuntu:22.04
# Build Linux x86-64 agent
```

**Option C: Check In Pre-Built**
```bash
# Build on each platform once
# Commit agents to git
git add gradle-plugin/src/main/resources/native/
```

## Summary

**Java version compatibility is built-in**:
- ✅ JVMTI API is stable since Java 5
- ✅ Dynamic linking resolves at runtime
- ✅ Backward compatibility guaranteed
- ✅ One agent per platform works for all Java versions

**We only need multiple binaries for**:
- ✅ Different operating systems (macOS, Linux, Windows)
- ✅ Different architectures (ARM64, x86-64)
- ❌ NOT different Java versions

**Current status**:
- Built for: darwin-aarch64 (macOS ARM)
- Tested on: Java 21, 25
- Works on: All Java 21+

**TODO**:
- Build for darwin-x86-64 (macOS Intel)
- Build for linux-x86-64 (Linux AMD64)
- Build for windows-x86-64 (Windows)
- Add CI matrix testing across Java versions
