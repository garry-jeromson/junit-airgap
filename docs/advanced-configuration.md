# Advanced Configuration Guide

This guide covers advanced configuration options and patterns for the JUnit Airgap Extension.

## Table of Contents

- [Configuration Methods](#configuration-methods)
- [Priority Order](#priority-order)
- [Host Filtering](#host-filtering)
- [Default Blocking Mode](#default-blocking-mode)
- [Implementation Selection](#implementation-selection)
- [Debug Mode](#debug-mode)
- [Per-Test Configuration](#per-test-configuration)
- [Class-Level Configuration](#class-level-configuration)
- [System Properties](#system-properties)
- [Gradle Plugin Configuration](#gradle-plugin-configuration)

## Configuration Methods

The library supports multiple configuration methods that can be combined:

1. **Annotations** (method or class level)
2. **Constructor parameters** (JUnit 5 `@RegisterExtension` or JUnit 4 `@Rule`)
3. **System properties** (JVM arguments or Gradle configuration)
4. **Gradle plugin** (project-wide defaults)

## Priority Order

When multiple configuration options are present, they are evaluated in priority order (highest to lowest):

1. **@AllowNetworkRequests** - Always allows network (highest priority)
2. **Constructor parameter** - `applyToAllTests = true/false` in `AirgapExtension` or `AirgapRule`
3. **System property** - `-Djunit.airgap.applyToAllTests=true`
4. **@NoNetworkByDefault** - Class-level default blocking
5. **@BlockNetworkRequests** - Method/class-level explicit blocking
6. **Default** - No blocking (lowest priority)

### Example: Priority Demonstration

```kotlin
class MyTest {
    @JvmField
    @RegisterExtension
    val extension = AirgapExtension(applyToAllTests = true) // Priority 2

    @Test
    fun test1() {
        // Network BLOCKED (applyToAllTests = true)
    }

    @Test
    @AllowNetworkRequests // Priority 1 (highest)
    fun test2() {
        // Network ALLOWED (@AllowNetworkRequests overrides applyToAllTests)
    }

    @Test
    @BlockNetworkRequests // Priority 5
    @AllowNetworkRequests // Priority 1 (highest)
    fun test3() {
        // Network ALLOWED (@AllowNetworkRequests wins)
    }
}
```

## Host Filtering

### Allow Specific Hosts

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "127.0.0.1", "*.test.local"])
fun testLocalServers() {
    // ✅ localhost - allowed
    // ✅ 127.0.0.1 - allowed
    // ✅ api.test.local - allowed (matches *.test.local)
    // ❌ example.com - blocked
}
```

### Wildcard Patterns

Wildcards support subdomain matching:

```kotlin
@AllowRequestsToHosts(["*.example.com", "*.staging.mycompany.com"])
```

**Matches**:
- ✅ `api.example.com`
- ✅ `www.example.com`
- ✅ `auth.staging.mycompany.com`

**Does NOT match**:
- ❌ `example.com` (root domain, doesn't match `*.example.com`)
- ❌ `api.production.mycompany.com`

### Block Specific Hosts

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*"]) // Allow all
@BlockRequestsToHosts(["evil.com", "*.tracking.com"]) // Except these
fun testBlockList() {
    // ✅ Most hosts - allowed
    // ❌ evil.com - blocked
    // ❌ analytics.tracking.com - blocked (matches *.tracking.com)
}
```

**Important**: Blocked hosts ALWAYS take precedence over allowed hosts.

### IPv6 Support

```kotlin
@AllowRequestsToHosts(["::1", "[0:0:0:0:0:0:0:1]"])
```

### Android Emulator Localhost

The Android emulator uses `10.0.2.2` to access the host machine:

```kotlin
@AllowRequestsToHosts(["localhost", "127.0.0.1", "10.0.2.2"])
```

## Default Blocking Mode

### Method 1: Constructor Parameter

**JUnit 5:**

```kotlin
import org.junit.jupiter.api.extension.RegisterExtension

class MyTest {
    @JvmField
    @RegisterExtension
    val extension = AirgapExtension(applyToAllTests = true)

    @Test
    fun test1() {
        // Network BLOCKED by default
    }

    @Test
    @AllowNetworkRequests
    fun test2() {
        // Network ALLOWED (opt-out)
    }
}
```

**JUnit 4:**

```kotlin
import org.junit.Rule

class MyTest {
    @get:Rule
    val noNetworkRule = AirgapRule(applyToAllTests = true)

    @Test
    fun test1() {
        // Network BLOCKED by default
    }

    @Test
    @AllowNetworkRequests
    fun test2() {
        // Network ALLOWED (opt-out)
    }
}
```

### Method 2: Class-Level Annotation

```kotlin
@ExtendWith(AirgapExtension::class)
@NoNetworkByDefault
class MyTest {
    @Test
    fun test1() {
        // Network BLOCKED by default
    }

    @Test
    @AllowNetworkRequests
    fun test2() {
        // Network ALLOWED (opt-out)
    }
}
```

### Method 3: System Property

```bash
# Gradle
./gradlew test -Djunit.airgap.applyToAllTests=true

# Maven
mvn test -Djunit.airgap.applyToAllTests=true
```

**Gradle configuration:**

```kotlin
tasks.test {
    systemProperty("junit.airgap.applyToAllTests", "true")
}
```

### Method 4: Gradle Plugin

```kotlin
junitAirgap {
    applyToAllTests = true
}
```

## Implementation

This library uses **JVMTI (JVM Tool Interface)** for network blocking on all JVM platforms.

### How It Works

- C++ JVMTI agent intercepts socket and DNS operations at the native level
- Agent automatically packaged with library and extracted at runtime
- Works on Java 21+ (no SecurityManager dependency)
- Single unified implementation for both JVM and Android (Robolectric)

### Platform Support

| Platform | Status | Implementation |
|----------|--------|----------------|
| JVM | ✅ Fully Supported | JVMTI Agent |
| Android (Robolectric) | ✅ Fully Supported | JVMTI Agent |
| iOS | ⚠️ API Structure Only | No-op (Kotlin/Native doesn't support JVMTI) |

## Debug Mode

Enable debug logging to troubleshoot configuration issues.

### Method 1: System Property

```bash
./gradlew test -Djunit.airgap.debug=true
```

### Method 2: Gradle Plugin

```kotlin
junitAirgap {
    debug = true
}
```

### Method 3: Environment Variable

```bash
export JUNIT_NONETWORK_DEBUG=true
./gradlew test
```

### Debug Output Examples

```
NetworkBlocker: Using SECURITY_MANAGER implementation
NetworkBlocker: Installing network blocker
NetworkBlocker: Configuration: allowedHosts=[localhost], blockedHosts=[]
NetworkBlocker: Checking connection to example.com:443
NetworkBlocker: Blocked connection to example.com:443
NetworkBlocker: Uninstalling network blocker
```

## Per-Test Configuration

### Override Global Configuration

```kotlin
junitAirgap {
    applyToAllTests = true // Global default: block all
    allowedHosts = listOf("localhost")
}
```

```kotlin
@Test
@AllowNetworkRequests // Override: allow for this test only
fun testNeedsNetwork() {
    // Network allowed
}

@Test
@AllowRequestsToHosts(["*.example.com"]) // Override: allow specific hosts
fun testSpecificHosts() {
    // ✅ api.example.com - allowed
    // ❌ other.com - blocked
}
```

### Combine Annotations

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
@BlockRequestsToHosts(["evil.com"])
fun testComplexRules() {
    // ✅ localhost - allowed
    // ❌ evil.com - blocked (even if in allowedHosts)
    // ❌ example.com - blocked (not in allowedHosts)
}
```

## Class-Level Configuration

Apply annotations at class level to affect all tests:

```kotlin
@ExtendWith(AirgapExtension::class)
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "*.test.local"])
class MyTest {
    @Test
    fun test1() {
        // Network blocked except localhost and *.test.local
    }

    @Test
    fun test2() {
        // Same configuration
    }

    @Test
    @AllowNetworkRequests // Override class-level configuration
    fun test3() {
        // Network fully allowed
    }
}
```

### JUnit 4 Class-Level Configuration

```kotlin
@RunWith(JUnit4::class)
class MyTest {
    companion object {
        @JvmField
        @ClassRule
        val classRule = AirgapRule(applyToAllTests = true)
    }

    @Test
    fun test1() {
        // Network blocked for all tests
    }
}
```

## System Properties

All system properties can be set via:

1. **Command line**: `-Dproperty=value`
2. **Gradle configuration**: `systemProperty("property", "value")`
3. **Environment variables**: Some properties support env vars

### Available Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `junit.airgap.applyToAllTests` | boolean | false | Block all tests by default |
| `junit.airgap.implementation` | string | securitymanager | Implementation to use |
| `junit.airgap.debug` | boolean | false | Enable debug logging |

### Gradle Configuration

```kotlin
tasks.withType<Test> {
    // Java 21+ requirement
    jvmArgs("-Djava.security.manager=allow")

    // Library configuration
    systemProperty("junit.airgap.applyToAllTests", "true")
    systemProperty("junit.airgap.implementation", "securitymanager")
    systemProperty("junit.airgap.debug", "false")
}
```

## Gradle Plugin Configuration

Complete plugin configuration reference:

```kotlin
junitAirgap {
    // Enable/disable plugin (default: true)
    enabled = true

    // Block all tests by default (default: false)
    applyToAllTests = false

    // Library version (default: matches plugin version)
    libraryVersion = "0.1.0-SNAPSHOT"

    // Global allowed hosts (default: empty)
    allowedHosts = listOf(
        "localhost",
        "127.0.0.1",
        "*.test.local",
        "*.staging.mycompany.com"
    )

    // Global blocked hosts (default: empty)
    blockedHosts = listOf(
        "evil.com",
        "*.tracking.com"
    )

    // Debug logging (default: false)
    debug = false

    // JUnit 4 automatic @Rule injection (default: false, experimental)
    injectJUnit4Rule = false
}
```

### Conditional Configuration

```kotlin
junitAirgap {
    enabled = !project.hasProperty("skipNoNetwork")
    debug = project.hasProperty("debugNoNetwork")

    allowedHosts = when {
        project.hasProperty("ci") -> listOf("localhost") // CI: strict
        else -> listOf("localhost", "*.staging.mycompany.com") // Local: relaxed
    }
}
```

### Per-Module Configuration

```kotlin
// Root build.gradle.kts
allprojects {
    pluginManager.withPlugin("io.github.garryjeromson.junit-airgap") {
        configure<JunitAirgapExtension> {
            enabled = true
            allowedHosts = listOf("localhost")
        }
    }
}

// Specific module build.gradle.kts
junitAirgap {
    enabled = false // Disable for this module
}
```

## Configuration Patterns

### Pattern 1: Strict Unit Tests

Block everything except localhost:

```kotlin
junitAirgap {
    applyToAllTests = true
    allowedHosts = listOf("localhost", "127.0.0.1")
}
```

### Pattern 2: Integration Tests

Allow specific staging environments:

```kotlin
junitAirgap {
    applyToAllTests = false
    allowedHosts = listOf(
        "localhost",
        "*.staging.mycompany.com",
        "*.test.mycompany.com"
    )
}
```

### Pattern 3: Development vs CI

```kotlin
junitAirgap {
    val isCi = System.getenv("CI") == "true"

    applyToAllTests = isCi // Strict on CI, relaxed locally
    debug = !isCi // Debug locally, quiet on CI

    allowedHosts = if (isCi) {
        listOf("localhost") // CI: strict
    } else {
        listOf("localhost", "*.staging.mycompany.com") // Local: relaxed
    }
}
```

### Pattern 4: Feature Flags

```kotlin
junitAirgap {
    enabled = project.findProperty("enableNoNetwork") == "true"
}
```

Run with: `./gradlew test -PenableNoNetwork=true`

## Troubleshooting

### Configuration Not Applied

**Checklist**:
1. Is plugin applied? Check `plugins { }`
2. Is configuration block present? Check `junitAirgap { }`
3. Is annotation present? Check `@BlockNetworkRequests`
4. Check priority order (maybe another config is overriding)
5. Enable debug mode: `debug = true`

### Unexpected Blocking Behavior

**Solution**: Check priority order. Higher priority configs override lower ones.

```kotlin
// Example: @AllowNetworkRequests always wins
@Test
@BlockNetworkRequests // Lower priority
@AllowNetworkRequests // Higher priority - WINS
fun test() {
    // Network is ALLOWED
}
```

### Host Filtering Not Working

**Checklist**:
1. Check wildcard syntax: `*.example.com` (not `*.example.*`)
2. Check blocked hosts (they take precedence)
3. Enable debug to see which hosts are checked
4. Remember: `*.example.com` does NOT match `example.com`

## Performance & Overhead

The JVMTI agent provides comprehensive network blocking with minimal performance impact.

### Quick Summary

- **Agent loading**: ONE TIME at JVM startup (~5-10ms)
- **Per-test overhead**: ~100-500 nanoseconds (ThreadLocal configuration)
- **Real-world impact**: <10% for tests doing meaningful work

### Performance Measurements

From benchmark suite (100 iterations, Java 21, macOS ARM64):

| Test Type | Overhead | Notes |
|-----------|----------|-------|
| Empty Test | +458 ns (+183%) | High % but negligible absolute time |
| Simple Assertion | -80 ns (-3.6%) | Measurement noise |
| Array Sorting (4.2ms) | +270 μs (+6.4%) | Realistic test - low overhead |
| String Operations | -56 μs (-6.6%) | Negative = measurement variance |

### Key Insights

1. **Small constant overhead**: ~250-500ns for ThreadLocal operations per test
2. **High % for tiny tests**: 183% overhead on 250ns operation = only 458ns absolute
3. **Low % for real tests**: 6.4% overhead on 4.2ms operation = 270μs absolute
4. **Negligible for I/O tests**: Any test doing I/O will have <1% overhead

### Understanding the Numbers

**Why high percentages for small tests?**

The overhead is a small constant (~500ns) that becomes a high percentage of very fast operations:
- Empty test: 250ns → 708ns = +183% (but only 458ns absolute)
- Real test: 4.2ms → 4.5ms = +6.4% (270μs absolute, negligible)

**When does overhead matter?**

Overhead is negligible if your test:
- ✅ Makes any I/O operations (file, network, database)
- ✅ Performs computation (>1ms)
- ✅ Allocates objects or uses reflection

Overhead might be noticeable if your test:
- ⚠️ Is a microbenchmark measuring nanoseconds
- ⚠️ Runs thousands of times in tight loop

### Three-Stage Loading Model

The "loading" process has three distinct stages:

1. **Agent Loading** (JVM Startup)
   - **When**: JVM process starts
   - **Frequency**: ONE TIME per JVM
   - **Duration**: ~5-10ms
   - **What**: Gradle plugin adds `-agentpath`, JVM loads native agent

2. **Native Method Replacement** (First Call)
   - **When**: First time socket method is called
   - **Frequency**: ONCE per native method
   - **Duration**: ~microseconds
   - **What**: Agent replaces function pointers with wrappers

3. **Configuration Setting** (Per-Test)
   - **When**: Before/after each test
   - **Frequency**: EVERY TEST
   - **Duration**: ~100-500ns
   - **What**: Set/clear ThreadLocal configuration

**Common misconception**: "The agent loads/unloads every test"
**Reality**: Only step 3 happens per-test. The agent loads once at startup.

### Detailed Documentation

For a comprehensive explanation of the loading model, performance characteristics, and benchmark methodology, see:

**[JVMTI Agent Loading & Performance](architecture/jvmti-loading.md)**

## See Also

- [Compatibility Matrix](compatibility-matrix.md)
- [Setup Guides](setup-guides/)
- [Gradle Plugin Guide](setup-guides/gradle-plugin.md)
- [Migration Guide: Java 24+](migration-java24.md)
- [JVMTI Agent Loading & Performance](architecture/jvmti-loading.md)
