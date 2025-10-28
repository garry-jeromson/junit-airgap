# Setup Guide: JVM + JUnit 5

Block network requests in JVM unit tests using JUnit 5.

## Requirements

- Java 21+ (uses JVMTI agent)
- Gradle 7.x+ (tested with 8.11.1)
- JUnit 5.8+ (tested with 5.11.3)
- Kotlin 1.9+ (tested with 2.1.0)

## Installation

### Gradle Plugin (Recommended)

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-beta.1"
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### Manual Configuration

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("io.github.garryjeromson:junit-airgap:0.1.0-beta.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

Then create `src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.extensions.autodetection.enabled=true
```

## Basic Usage

### Simple Test

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import org.junit.jupiter.api.Test
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
        }
    }
}
```

### Class-Level Annotation

Apply to all tests in a class:

```kotlin
import io.github.garryjeromson.junit.airgap.AirgapExtension
import io.github.garryjeromson.junit.airgap.AllowNetworkRequests
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(AirgapExtension::class)
@BlockNetworkRequests
class MyTest {
    @Test
    fun test1() {
        // Network blocked (class-level annotation)
    }

    @Test
    @AllowNetworkRequests
    fun test2() {
        // Network allowed (opt-out)
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

Or via Gradle plugin:

```kotlin
junitAirgap {
    applyToAllTests = true
}
```

### Allow Specific Hosts

```kotlin
import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts

@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
fun testCanConnectToLocalhost() {
    try {
        Socket("localhost", 8080).close()
    } catch (e: NetworkRequestAttemptedException) {
        throw AssertionError("Localhost should be allowed", e)
    } catch (e: Exception) {
        // Connection refused is OK
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

### Block Specific Hosts Only

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

### Tests Pass When They Should Fail

1. Is `@BlockNetworkRequests` annotation present?
2. Is `@ExtendWith(AirgapExtension::class)` on class?
3. Is auto-detection enabled in `junit-platform.properties`?
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

See `plugin-integration-tests/jvm-junit5` for a complete working example with:
- Gradle plugin configuration
- Network blocking tests
- HTTP client integration tests
- Advanced configuration examples

## See Also

- [Compatibility Matrix](../compatibility-matrix.md)
- [HTTP Client Guides](../clients/)
- [Advanced Configuration](../advanced-configuration.md)
- [JVM + JUnit 4 Setup](jvm-junit4.md) - Using JUnit 4
