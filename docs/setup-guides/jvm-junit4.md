# Setup Guide: JVM + JUnit 4

Block network requests in JVM unit tests using JUnit 4.

## Requirements

- Java 21+ for build (Kotlin Gradle Plugin requirement)
- JVMTI agent works on any Java version at runtime
- Gradle 8.0 or later (tested with 8.0, 8.11.1, 9.1)
- JUnit 4.12+ (tested with 4.13.2)
- Kotlin 1.9+ (tested with 2.1.0)

## Installation

### Gradle Plugin (Recommended)

With auto-injection, no manual `@Rule` fields needed:

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
    id("io.github.garry-jeromson.junit-airgap") version "0.1.0-beta.1"
}

kotlin {
    jvmToolchain(21)
}

// Note: Do NOT use useJUnitPlatform() for JUnit 4 projects
```

Usage with auto-injection:

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.Test
import java.net.Socket

class MyTest {
    // No @Rule field needed - plugin injects it automatically!

    @Test
    @BlockNetworkRequests
    fun testNetworkBlocked() {
        Socket("example.com", 80) // Throws NetworkRequestAttemptedException
    }
}
```

### Manual Configuration

If you prefer explicit configuration:

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("io.github.garryjeromson:junit-airgap:0.1.0-beta.1")
    testImplementation("junit:junit:4.13.2")
}
```

Then manually add `@Rule`:

```kotlin
import io.github.garryjeromson.junit.airgap.AirgapRule
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.Rule
import org.junit.Test
import java.net.Socket

class MyTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun testNetworkBlocked() {
        Socket("example.com", 80) // Throws NetworkRequestAttemptedException
    }
}
```

## Basic Usage

### Simple Test

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import org.junit.Test
import java.net.Socket
import kotlin.test.assertFailsWith

class MyTest {
    @Test
    @BlockNetworkRequests
    fun testNetworkBlocked() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    fun testNetworkAllowed() {
        // Network requests work normally without @BlockNetworkRequests
        try {
            Socket("example.com", 80).close()
        } catch (e: Exception) {
            // Connection might fail for other reasons
            // but it won't throw NetworkRequestAttemptedException
        }
    }
}
```

### Opt-Out with @AllowNetworkRequests

```kotlin
import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.AirgapRule
import org.junit.Rule
import org.junit.Test
import java.net.Socket

class MyTest {
    @get:Rule
    val noNetworkRule = AirgapRule(applyToAllTests = true)

    @Test
    fun testBlockedByDefault() {
        // Network is blocked because applyToAllTests=true
    }

    @Test
    @AllowNetworkRequests
    fun testExplicitlyAllowed() {
        // Network allowed even though applyToAllTests=true
        Socket("example.com", 80).close()
    }
}
```

## HTTP Client Testing

For testing with popular HTTP clients, see the **[HTTP Client Guides](../clients/)**:
- **[OkHttp](../clients/okhttp.md)** - Most popular JVM/Android HTTP client
- **[Retrofit](../clients/retrofit.md)** - Type-safe API client
- **[Ktor](../clients/ktor.md)** - Kotlin Multiplatform HTTP client

## Advanced Configuration

### Block All Tests by Default

```kotlin
@get:Rule
val noNetworkRule = AirgapRule(applyToAllTests = true)
```

Then opt-out specific tests:

```kotlin
@Test
@AllowNetworkRequests
fun testCanMakeNetworkRequests() {
    // Network allowed
}
```

### Allow Specific Hosts

```kotlin
import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.Test
import java.net.Socket

class LocalhostTest {
    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(["localhost", "127.0.0.1"])
    fun testCanConnectToLocalhost() {
        // Localhost connections are allowed
        try {
            Socket("localhost", 8080).close()
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Localhost should be allowed", e)
        } catch (e: Exception) {
            // Connection refused is OK
        }
    }
}
```

### Wildcard Patterns

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*.example.com", "*.test.local"])
fun testAllowsSubdomains() {
    // ✅ api.example.com, www.example.com - allowed
    // ❌ example.com - blocked (doesn't match *.example.com)
    // ❌ other.com - blocked
}
```

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests NetworkBlockingTest

# Run with debug output
./gradlew test --info -Djunit.airgap.debug=true
```

### IntelliJ IDEA

Add JVM args to test configuration:
- Run → Edit Configurations
- Add VM options: `-Djunit.airgap.debug=true`

Or configure globally in `.idea/workspace.xml`:
```xml
<component name="RunManager">
  <configuration default="true" type="JUnit">
    <option name="VM_PARAMETERS" value="-Djunit.airgap.debug=true" />
  </configuration>
</component>
```

## Troubleshooting

### Auto-injection Not Working

1. Is JUnit 4 auto-detected? Check build output for "Auto-detected JUnit 4" message
2. Is `junit:junit` dependency present in `testImplementation`?
3. Are you using `useJUnitPlatform()`? (This enables JUnit 5 mode - remove it)
4. Try manual override: `injectJUnit4Rule = true` in plugin config
5. Try manual `@Rule` configuration as fallback
6. Enable debug: `junitAirgap { debug = true }`

### Tests Pass When They Should Fail

1. Is `@BlockNetworkRequests` annotation present?
2. Is `@Rule val noNetworkRule = AirgapRule()` declared? (if not using plugin)
3. Is the Gradle plugin applied correctly?
4. Enable debug: `-Djunit.airgap.debug=true`

### HTTP Client Exceptions

HTTP clients wrap `NetworkRequestAttemptedException`. Check the exception message:

```kotlin
try {
    client.newCall(request).execute()
} catch (e: Exception) {
    assertTrue(
        e.message?.contains("Network request blocked") == true,
        "Expected network blocking but got: $e"
    )
}
```

## Example Project

See `plugin-integration-tests/jvm-junit4` for a complete working example with:
- Gradle plugin configuration with auto-injection
- Network blocking tests
- HTTP client integration tests
- Advanced configuration examples

## See Also

- [Compatibility Matrix](../compatibility-matrix.md)
- [HTTP Client Guides](../clients/)
- [Advanced Configuration](../advanced-configuration.md)
- [JVM + JUnit 5 Setup Guide](jvm-junit5.md) - Migrating to JUnit 5
