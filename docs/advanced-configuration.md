# Advanced Configuration

Complete reference for configuring the JUnit Airgap Extension.

## Table of Contents

- [Configuration Methods](#configuration-methods)
- [Priority Order](#priority-order)
- [Host Filtering](#host-filtering)
- [Default Blocking Mode](#default-blocking-mode)
- [Debug Mode](#debug-mode)
- [Gradle Plugin](#gradle-plugin)
- [Common Patterns](#common-patterns)

## Configuration Methods

Four ways to configure network blocking (can be combined):

1. **Annotations** - `@BlockNetworkRequests`, `@AllowNetworkRequests`, `@AllowRequestsToHosts`, `@BlockRequestsToHosts`
2. **Constructor parameters** - `AirgapExtension(applyToAllTests = true)` or `AirgapRule(applyToAllTests = true)`
3. **System properties** - `-Djunit.airgap.applyToAllTests=true`
4. **Gradle plugin** - `junitAirgap { applyToAllTests = true }`

## Priority Order

When multiple configurations are present (highest to lowest priority):

1. **@AllowNetworkRequests** - Always allows network (opt-out)
2. **Constructor parameter** - `applyToAllTests = true/false`
3. **JUnit Platform config** - `junit-platform.properties`
4. **System property** - `-Djunit.airgap.applyToAllTests=true`
5. **@BlockNetworkRequests** - Explicit blocking
6. **Default** - No blocking

**Example:**
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
        // Network ALLOWED (@AllowNetworkRequests overrides)
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
    // ✅ localhost, 127.0.0.1, api.test.local - allowed
    // ❌ example.com - blocked
}
```

### Wildcard Patterns

Supports subdomain matching:

```kotlin
@AllowRequestsToHosts(["*.example.com", "*.staging.mycompany.com"])
```

**Matches:**
- ✅ `api.example.com`, `www.example.com`, `auth.staging.mycompany.com`

**Does NOT match:**
- ❌ `example.com` (root domain doesn't match `*.example.com`)
- ❌ `api.production.mycompany.com`

### Block Specific Hosts

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*"]) // Allow all
@BlockRequestsToHosts(["evil.com", "*.tracking.com"]) // Except these
fun testBlockList() {
    // ✅ Most hosts - allowed
    // ❌ evil.com, analytics.tracking.com - blocked
}
```

**Important:** Blocked hosts ALWAYS take precedence over allowed hosts.

### Platform-Specific Hosts

**IPv6:**
```kotlin
@AllowRequestsToHosts(["::1", "[0:0:0:0:0:0:0:1]"])
```

**Android Emulator:**
```kotlin
@AllowRequestsToHosts(["localhost", "127.0.0.1", "10.0.2.2"]) // 10.0.2.2 = host machine
```

## Default Blocking Mode

Block all tests by default, opt-out as needed.

### Via Constructor

**JUnit 5:**
```kotlin
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
class MyTest {
    @get:Rule
    val noNetworkRule = AirgapRule(applyToAllTests = true)

    // Same behavior as JUnit 5
}
```

### Via System Property

```bash
./gradlew test -Djunit.airgap.applyToAllTests=true
```

**Gradle configuration:**
```kotlin
tasks.test {
    systemProperty("junit.airgap.applyToAllTests", "true")
}
```

### Via Gradle Plugin

```kotlin
junitAirgap {
    applyToAllTests = true
}
```

## Debug Mode

Enable detailed logging for troubleshooting.

### System Property

```bash
./gradlew test -Djunit.airgap.debug=true
```

### Gradle Plugin

```kotlin
junitAirgap {
    debug = true
}
```

### Sample Output

```
NetworkBlocker: Installing network blocker
NetworkBlocker: Configuration: allowedHosts=[localhost], blockedHosts=[]
NetworkBlocker: Checking connection to example.com:443
NetworkBlocker: Blocked connection to example.com:443
```

## Gradle Plugin

Complete plugin configuration:

```kotlin
junitAirgap {
    // Enable/disable plugin (default: true)
    enabled = true

    // Block all tests by default (default: false)
    applyToAllTests = false

    // Global allowed hosts (default: empty)
    allowedHosts = listOf(
        "localhost",
        "127.0.0.1",
        "*.test.local"
    )

    // Global blocked hosts (default: empty)
    blockedHosts = listOf(
        "evil.com",
        "*.tracking.com"
    )

    // Debug logging (default: false)
    debug = false
}
```

### Per-Test Override

```kotlin
// Global: block all, allow localhost
junitAirgap {
    applyToAllTests = true
    allowedHosts = listOf("localhost")
}
```

```kotlin
@Test
@AllowNetworkRequests // Override: allow all for this test
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

### Class-Level Configuration

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
    @AllowNetworkRequests // Override class-level
    fun test2() {
        // Network fully allowed
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

// Specific module
junitAirgap {
    enabled = false // Disable for this module
}
```

## Common Patterns

### Strict Unit Tests

Block everything except localhost:

```kotlin
junitAirgap {
    applyToAllTests = true
    allowedHosts = listOf("localhost", "127.0.0.1")
}
```

### Development vs CI

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

### Feature Flags

```kotlin
junitAirgap {
    enabled = project.findProperty("enableNoNetwork") == "true"
}
```

Run with: `./gradlew test -PenableNoNetwork=true`

## Troubleshooting

### Configuration Not Applied

1. Is plugin applied? Check `plugins { }`
2. Is annotation present? Check `@BlockNetworkRequests`
3. Check priority order (higher priority configs override)
4. Enable debug mode: `debug = true`

### Unexpected Blocking Behavior

Check priority order - higher priority configs always win:

```kotlin
@Test
@BlockNetworkRequests // Lower priority
@AllowNetworkRequests // Higher priority - WINS
fun test() {
    // Network is ALLOWED
}
```

### Host Filtering Not Working

1. Check wildcard syntax: `*.example.com` (not `*.example.*`)
2. Blocked hosts take precedence over allowed hosts
3. Enable debug to see host checks
4. `*.example.com` does NOT match `example.com` (root domain)

## Performance

The JVMTI agent loads once at JVM startup with minimal overhead:

- **Agent loading**: ONE TIME at startup (~5-10ms)
- **Per-test overhead**: ~100-500 nanoseconds
- **Real-world impact**: <10% for tests doing meaningful work

For detailed performance analysis and benchmark results, see **[JVMTI Agent Loading & Performance](architecture/jvmti-loading.md)**.

## See Also

- [Compatibility Matrix](compatibility-matrix.md)
- [Setup Guides](setup-guides/)
- [Gradle Plugin Reference](setup-guides/gradle-plugin.md)
- [JVMTI Performance Deep Dive](architecture/jvmti-loading.md)
