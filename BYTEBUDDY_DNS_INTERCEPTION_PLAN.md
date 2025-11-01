# ByteBuddy DNS Interception Implementation Plan

## Overview

Implement ByteBuddy-based Java-layer DNS interception as a fallback for when JVMTI native interception fails (due to DNS classes being loaded before agent initialization).

## Goals

1. **100% Reliability**: DNS interception works regardless of JVM initialization timing
2. **Zero Configuration**: Users don't need to change test code
3. **Performance**: Minimal overhead (ByteBuddy advice is JIT-inlined)
4. **Compatibility**: Works with existing JVMTI agent (both can coexist)

## Architecture

### Two-Layer Interception Strategy

```
Layer 1 (Java): ByteBuddy Transformer
  ↓
  Intercepts: InetAddress.getAllByName()
  Location: Java method call, before native invocation
  Timing: Always works (transforms class at load time)

Layer 2 (Native): JVMTI Wrapper
  ↓
  Intercepts: Inet6AddressImpl.lookupAllHostAddr() (native)
  Location: Native method binding
  Timing: Only works if bound after agent loads
```

Both layers call the same `NetworkBlockerContext.checkConnection()` logic.

## Implementation Steps

### Phase 1: ByteBuddy Agent Infrastructure

#### 1.1 Create ByteBuddy Agent Entry Point

**File**: `junit-airgap/src/jvmMain/kotlin/io/github/garryjeromson/junit/airgap/bytebuddy/InetAddressBytebuddyAgent.kt`

```kotlin
package io.github.garryjeromson.junit.airgap.bytebuddy

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.utility.JavaModule
import java.lang.instrument.Instrumentation

/**
 * ByteBuddy Java agent for intercepting DNS resolution at the Java API layer.
 *
 * This agent transforms java.net.InetAddress to intercept DNS lookups before
 * they reach the native layer. This ensures interception works even when:
 * - DNS classes are loaded before JVMTI agent initialization
 * - Native methods are bound before NativeMethodBindCallback fires
 * - Multiple JVM instances are spawned (e.g., IDE test runners)
 */
object InetAddressBytebuddyAgent {
    /**
     * Java agent entry point called by JVM.
     *
     * Usage: -javaagent:path/to/agent.jar
     */
    @JvmStatic
    fun premain(agentArgs: String?, instrumentation: Instrumentation) {
        DebugLogger.log("ByteBuddy DNS agent starting...")

        try {
            AgentBuilder.Default()
                .disableClassFormatChanges()  // Don't modify class format
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)  // Support retransformation
                .with(createListener())  // Add logging listener
                .type(ElementMatchers.named("java.net.InetAddress"))  // Target InetAddress
                .transform(::transformInetAddress)  // Apply transformer
                .installOn(instrumentation)

            DebugLogger.log("ByteBuddy DNS agent installed successfully")
        } catch (e: Exception) {
            System.err.println("[junit-airgap] ERROR: Failed to install ByteBuddy DNS agent: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Transform InetAddress class to inject DNS interception advice.
     */
    private fun transformInetAddress(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?,
    ): DynamicType.Builder<*> {
        DebugLogger.log("Transforming java.net.InetAddress")

        return builder
            // Intercept getAllByName(String)
            .visit(
                Advice
                    .to(InetAddressGetAllByNameAdvice::class.java)
                    .on(
                        ElementMatchers
                            .named("getAllByName")
                            .and(ElementMatchers.takesArguments(String::class.java)),
                    ),
            )
            // Intercept getByName(String)
            .visit(
                Advice
                    .to(InetAddressGetByNameAdvice::class.java)
                    .on(
                        ElementMatchers
                            .named("getByName")
                            .and(ElementMatchers.takesArguments(String::class.java)),
                    ),
            )
    }

    /**
     * Create listener for logging transformation events (debug mode only).
     */
    private fun createListener(): AgentBuilder.Listener {
        return if (DebugLogger.isEnabled()) {
            object : AgentBuilder.Listener {
                override fun onDiscovery(
                    typeName: String,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean,
                ) {
                    if (typeName == "java.net.InetAddress") {
                        DebugLogger.log("Discovered InetAddress class (loaded=$loaded)")
                    }
                }

                override fun onTransformation(
                    typeDescription: TypeDescription,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean,
                    dynamicType: DynamicType,
                ) {
                    DebugLogger.log("Transformed ${typeDescription.name}")
                }

                override fun onIgnored(
                    typeDescription: TypeDescription,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean,
                ) {
                    // Ignore
                }

                override fun onError(
                    typeName: String,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean,
                    throwable: Throwable,
                ) {
                    System.err.println("[junit-airgap] ERROR transforming $typeName: ${throwable.message}")
                }

                override fun onComplete(
                    typeName: String,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean,
                ) {
                    if (typeName == "java.net.InetAddress") {
                        DebugLogger.log("Transformation complete for InetAddress")
                    }
                }
            }
        } else {
            AgentBuilder.Listener.NoOp()
        }
    }
}
```

#### 1.2 Create ByteBuddy Advice Classes

**File**: `junit-airgap/src/jvmMain/kotlin/io/github/garryjeromson/junit/airgap/bytebuddy/InetAddressAdvice.kt`

```kotlin
package io.github.garryjeromson.junit.airgap.bytebuddy

import net.bytebuddy.asm.Advice

/**
 * ByteBuddy advice for InetAddress.getAllByName(String).
 *
 * This advice is injected into the method and executes BEFORE the original method body.
 */
class InetAddressGetAllByNameAdvice {
    companion object {
        @Advice.OnMethodEnter
        @JvmStatic
        fun enter(@Advice.Argument(0) host: String?) {
            // Null hostname means "localhost" - always allow
            if (host == null) {
                return
            }

            DebugLogger.log("ByteBuddy intercepting getAllByName($host)")

            // Check if this DNS lookup is allowed
            // Port -1 indicates this is a DNS lookup, not a socket connection
            try {
                NetworkBlockerContext.checkConnection(
                    host = host,
                    port = -1,
                    caller = "ByteBuddy-DNS",
                )
            } catch (e: Exception) {
                // Re-throw to propagate to caller
                throw e
            }
        }
    }
}

/**
 * ByteBuddy advice for InetAddress.getByName(String).
 */
class InetAddressGetByNameAdvice {
    companion object {
        @Advice.OnMethodEnter
        @JvmStatic
        fun enter(@Advice.Argument(0) host: String?) {
            // Null hostname means "localhost" - always allow
            if (host == null) {
                return
            }

            DebugLogger.log("ByteBuddy intercepting getByName($host)")

            try {
                NetworkBlockerContext.checkConnection(
                    host = host,
                    port = -1,
                    caller = "ByteBuddy-DNS",
                )
            } catch (e: Exception) {
                throw e
            }
        }
    }
}
```

#### 1.3 Update InetAddressInterceptor.kt

**File**: Already created, but remove unnecessary code since we're using Advice instead:

```kotlin
package io.github.garryjeromson.junit.airgap.bytebuddy

/**
 * ByteBuddy interceptor for InetAddress DNS resolution methods.
 *
 * NOTE: This file is kept for documentation purposes.
 * The actual interception is done via ByteBuddy Advice in InetAddressAdvice.kt
 */
object InetAddressInterceptor {
    // Implementation moved to InetAddressAdvice.kt
    // See InetAddressGetAllByNameAdvice and InetAddressGetByNameAdvice
}
```

### Phase 2: Build ByteBuddy Agent JAR

#### 2.1 Add ByteBuddy Dependencies

**File**: `junit-airgap/build.gradle.kts`

Add to dependencies:
```kotlin
jvmMain {
    dependencies {
        // ByteBuddy for runtime agent
        implementation("net.bytebuddy:byte-buddy:1.14.10")
        implementation("net.bytebuddy:byte-buddy-agent:1.14.10")
    }
}
```

#### 2.2 Create Agent JAR Task

**File**: `junit-airgap/build.gradle.kts`

```kotlin
// Task to create ByteBuddy agent JAR with manifest
val createBytebuddyAgentJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Create ByteBuddy agent JAR for DNS interception"

    archiveBaseName.set("junit-airgap-bytebuddy-agent")
    archiveClassifier.set("agent")

    // Include compiled Kotlin classes
    from(tasks.named("compileKotlinJvm").get().outputs)

    // Include ByteBuddy dependencies
    from(configurations.named("jvmRuntimeClasspath").get().map {
        if (it.isDirectory) it else zipTree(it)
    })

    // Set manifest for Java agent
    manifest {
        attributes(
            "Premain-Class" to "io.github.garryjeromson.junit.airgap.bytebuddy.InetAddressBytebuddyAgent",
            "Can-Retransform-Classes" to "true",
            "Can-Redefine-Classes" to "true"
        )
    }

    // Exclude duplicates and signature files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// Make agent JAR part of build
tasks.named("assemble") {
    dependsOn(createBytebuddyAgentJar)
}
```

### Phase 3: Integrate with Gradle Plugin

#### 3.1 Extract ByteBuddy Agent

**File**: `gradle-plugin/src/main/kotlin/io/github/garryjeromson/junit/airgap/gradle/BytebuddyAgentExtractor.kt`

```kotlin
package io.github.garryjeromson.junit.airgap.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import java.io.File
import java.io.FileOutputStream

/**
 * Extracts the ByteBuddy agent JAR from the junit-airgap library.
 *
 * Similar to NativeAgentExtractor, but for the ByteBuddy agent JAR.
 */
object BytebuddyAgentExtractor {
    private const val AGENT_RESOURCE_PATH = "/bytebuddy-agent/junit-airgap-bytebuddy-agent.jar"

    fun getAgentPath(
        buildDirectory: DirectoryProperty,
        logger: Logger,
        debug: Boolean,
    ): String? {
        try {
            val agentDir = buildDirectory.dir("junit-airgap/bytebuddy-agent").get().asFile
            agentDir.mkdirs()

            val agentFile = File(agentDir, "junit-airgap-bytebuddy-agent.jar")

            // Extract agent JAR if not already present
            if (!agentFile.exists()) {
                logger.debug("Extracting ByteBuddy agent to: ${agentFile.absolutePath}")

                val inputStream =
                    BytebuddyAgentExtractor::class.java.getResourceAsStream(AGENT_RESOURCE_PATH)
                        ?: run {
                            logger.error("ByteBuddy agent JAR not found in resources: $AGENT_RESOURCE_PATH")
                            return null
                        }

                inputStream.use { input ->
                    FileOutputStream(agentFile).use { output ->
                        input.copyTo(output)
                    }
                }

                logger.debug("ByteBuddy agent extracted successfully")
            } else if (debug) {
                logger.debug("ByteBuddy agent already exists: ${agentFile.absolutePath}")
            }

            return agentFile.absolutePath
        } catch (e: Exception) {
            logger.error("Failed to extract ByteBuddy agent: ${e.message}", e)
            return null
        }
    }
}
```

#### 3.2 Update Plugin to Load ByteBuddy Agent

**File**: `gradle-plugin/src/main/kotlin/io/github/garryjeromson/junit/airgap/gradle/JunitAirgapPlugin.kt`

Modify `configureJvmTestTask()`:

```kotlin
private fun configureJvmTestTask(
    testTask: Test,
    extension: JunitAirgapExtension,
    buildDirectory: DirectoryProperty,
) {
    testTask.doFirst {
        // ... existing code ...

        // 1. Add JVMTI agent (existing)
        val jvmtiAgentPath = NativeAgentExtractor.getAgentPath(buildDirectory, logger, extension.debug.get())
        if (jvmtiAgentPath != null) {
            val jvmtiAgentArg =
                if (extension.debug.get()) {
                    "-agentpath:$jvmtiAgentPath=debug"
                } else {
                    "-agentpath:$jvmtiAgentPath"
                }
            jvmArgs(jvmtiAgentArg)
            logger.debug("Added JVMTI agent: $jvmtiAgentArg")
        }

        // 2. Add ByteBuddy agent (new)
        val bytebuddyAgentPath = BytebuddyAgentExtractor.getAgentPath(buildDirectory, logger, extension.debug.get())
        if (bytebuddyAgentPath != null) {
            jvmArgs("-javaagent:$bytebuddyAgentPath")
            logger.debug("Added ByteBuddy agent: -javaagent:$bytebuddyAgentPath")
        } else {
            logger.warn("ByteBuddy agent not available. DNS interception may not work in all scenarios.")
        }
    }
}
```

### Phase 4: Package Agent JAR in Resources

#### 4.1 Copy Agent JAR to Resources

**File**: `junit-airgap/build.gradle.kts`

```kotlin
// Task to copy agent JAR into resources
val copyBytebuddyAgentToResources by tasks.registering(Copy::class) {
    group = "build"
    description = "Copy ByteBuddy agent JAR to resources"

    from(createBytebuddyAgentJar)
    into(layout.buildDirectory.dir("resources/jvmMain/bytebuddy-agent"))

    rename { "junit-airgap-bytebuddy-agent.jar" }
}

// Run before processResources
tasks.named("jvmProcessResources") {
    dependsOn(copyBytebuddyAgentToResources)
}
```

### Phase 5: Testing

#### 5.1 Unit Tests

**File**: `junit-airgap/src/jvmTest/kotlin/io/github/garryjeromson/junit/airgap/bytebuddy/InetAddressInterceptionTest.kt`

```kotlin
package io.github.garryjeromson.junit.airgap.bytebuddy

import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.airgap.annotations.BlockNetworkRequests
import org.junit.jupiter.api.Test
import java.net.InetAddress
import kotlin.test.assertFailsWith

class InetAddressInterceptionTest {
    @Test
    @BlockNetworkRequests
    fun `getAllByName should be intercepted`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            InetAddress.getAllByName("example.com")
        }
    }

    @Test
    @BlockNetworkRequests
    fun `getByName should be intercepted`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            InetAddress.getByName("example.com")
        }
    }

    @Test
    @BlockNetworkRequests(allowedHosts = ["localhost"])
    fun `localhost should always be allowed`() {
        // Should not throw
        InetAddress.getByName(null)  // null = localhost
        InetAddress.getByName("localhost")
        InetAddress.getByName("127.0.0.1")
    }
}
```

#### 5.2 Integration Tests

Run existing integration tests in Android Studio to verify fix:
- `AsyncHttpClientTest.asyncHttpClientIsAllowedWithAllowNetwork`
- All other `@AllowNetworkRequests` tests

#### 5.3 Performance Tests

Measure overhead of ByteBuddy interception:
```kotlin
@Test
fun `ByteBuddy interception overhead`() {
    val iterations = 10_000
    val start = System.nanoTime()

    repeat(iterations) {
        try {
            InetAddress.getAllByName("example.com")
        } catch (e: NetworkRequestAttemptedException) {
            // Expected
        }
    }

    val duration = (System.nanoTime() - start) / 1_000_000  // ms
    val perCall = duration.toDouble() / iterations

    println("ByteBuddy DNS interception: $perCall ms per call")
    // Acceptable: < 0.01 ms per call
}
```

### Phase 6: Documentation

#### 6.1 Update README

Document the two-layer approach:
```markdown
## How It Works

junit-airgap uses a two-layer interception strategy:

1. **ByteBuddy Java Agent**: Intercepts DNS at the Java API layer
   - Always works, regardless of JVM initialization timing
   - Intercepts `InetAddress.getAllByName()` and similar methods

2. **JVMTI Native Agent**: Intercepts DNS at the native layer (fallback)
   - Lower overhead when it works
   - May miss early DNS calls in some JVM configurations

Both layers check the same network configuration, ensuring comprehensive coverage.
```

#### 6.2 Update JVMTI_DNS_INTERCEPTION_LIMITATION.md

Add "Resolution" section documenting the ByteBuddy solution.

#### 6.3 Add Architecture Documentation

Create `docs/ARCHITECTURE.md` explaining:
- Why two agents are needed
- How they interact
- Performance characteristics
- Troubleshooting guide

## Timeline

### Day 1: Infrastructure
- ✅ Write implementation plan
- ⏳ Create ByteBuddy agent entry point
- ⏳ Create Advice classes
- ⏳ Add ByteBuddy dependencies
- ⏳ Create agent JAR build task

### Day 2: Integration
- ⏳ Implement BytebuddyAgentExtractor
- ⏳ Update Gradle plugin to load both agents
- ⏳ Package agent JAR in resources
- ⏳ Write unit tests

### Day 3: Testing & Polish
- ⏳ Run integration tests in Android Studio
- ⏳ Performance testing
- ⏳ Fix any issues
- ⏳ Update documentation
- ⏳ Code review

## Success Criteria

1. **Functionality**: All `@AllowNetworkRequests` tests pass in Android Studio
2. **Performance**: < 0.01 ms overhead per DNS call
3. **Compatibility**: Works alongside existing JVMTI agent
4. **Testing**: 100% of existing integration tests pass
5. **Documentation**: Clear explanation of two-layer approach

## Risks & Mitigation

### Risk 1: ByteBuddy Conflicts
**Risk**: ByteBuddy agent conflicts with JVMTI agent
**Mitigation**: Test both agents together early; they operate at different layers

### Risk 2: Performance Overhead
**Risk**: ByteBuddy advice adds measurable latency
**Mitigation**: Use `@Advice.OnMethodEnter` (fast); measure with benchmarks

### Risk 3: Class Loading Order
**Risk**: InetAddress loaded before ByteBuddy agent
**Mitigation**: Use `AgentBuilder.RedefinitionStrategy.RETRANSFORMATION`

### Risk 4: JVM Module System
**Risk**: Java 9+ module system prevents transformation
**Mitigation**: Add `--add-opens` flags if needed

## Alternative Approaches Considered

### 1. Pure JVMTI with SetNativeMethodPrefix
**Pros**: Single agent
**Cons**: Complex, requires prefix handling in all native calls

### 2. Pure ByteBuddy
**Pros**: Simpler, Java-only
**Cons**: Higher overhead, can't intercept socket connections at native layer

### 3. ASM-based Transformation
**Pros**: Lower-level control
**Cons**: Much more complex, ByteBuddy is higher-level and easier

**Decision**: Hybrid JVMTI + ByteBuddy provides best of both worlds

## Open Questions

1. **Agent Loading Order**: Should ByteBuddy load before or after JVMTI?
   - **Answer**: After - JVMTI needs to load as early as possible

2. **Dual Interception**: What if both agents intercept the same call?
   - **Answer**: ByteBuddy intercepts first (Java layer), JVMTI never reached

3. **Debug Logging**: How to coordinate logging between agents?
   - **Answer**: Both use `DebugLogger`, controlled by same property

4. **Agent Distribution**: Ship both agents in same JAR?
   - **Answer**: Yes - JVMTI shared library + ByteBuddy JAR both in resources

## Next Steps

1. Review this plan with stakeholders
2. Begin Phase 1 implementation
3. Set up CI to test in Android Studio environment
4. Monitor performance impact in benchmarks
