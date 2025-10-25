# Setup Guide: JVM + JUnit 5

This guide shows how to set up the JUnit No-Network Extension for a pure JVM project using JUnit 5.

## Requirements

- Java 21+ (uses JVMTI agent for network blocking)
- Gradle 7.x or later (tested with 8.11.1)
- JUnit 5.8+ (tested with 5.11.3)
- Kotlin 1.9+ (tested with 2.1.0)

## Installation

### Option 1: Using the Gradle Plugin (Recommended)

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(21)
}

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Use @BlockNetworkRequests explicitly
}

// Configure test tasks
tasks.withType<Test> {
    useJUnitPlatform()
}
```

### Option 2: Manual Dependency Configuration

If you prefer not to use the plugin:

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

Create `src/test/resources/junit-platform.properties` for auto-discovery:

```properties
junit.jupiter.extensions.autodetection.enabled=true
```

## Basic Usage

### Simple Network Blocking Test

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.junit.jupiter.api.Test
import java.net.Socket
import kotlin.test.assertFailsWith

class MyTest {
    @Test
    @BlockNetworkRequests
    fun `should block network requests`() {
        // This will throw NetworkRequestAttemptedException
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80).use { }
        }
    }

    @Test
    fun `network allowed without annotation`() {
        // Network requests work normally without @BlockNetworkRequests
        try {
            Socket("example.com", 80).close()
        } catch (e: Exception) {
            // Connection might fail for other reasons (no internet, etc.)
            // but it won't throw NetworkRequestAttemptedException
        }
    }
}
```

### Using @AllowNetworkRequests

```kotlin
import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.jupiter.api.Test
import java.net.Socket

class MyTest {
    @Test
    @BlockNetworkRequests
    fun `blocked by default`() {
        // Network is blocked
    }

    @Test
    @AllowNetworkRequests
    fun `explicitly allowed`() {
        // Network is allowed even if applyToAllTests=true
        Socket("example.com", 80).close()
    }
}
```

## Testing HTTP Clients

### OkHttp Client

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class OkHttpTest {
    @Test
    @BlockNetworkRequests
    fun `OkHttp is blocked`() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://example.com")
            .build()

        try {
            client.newCall(request).execute()
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            // OkHttp wraps NetworkRequestAttemptedException in IOException
            assertTrue(e.message?.contains("Network request blocked") == true)
        }
    }
}
```

### Ktor CIO Client

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class KtorCioTest {
    @Test
    @BlockNetworkRequests
    fun `Ktor CIO is blocked`() = runTest {
        val client = HttpClient(CIO)

        // CIO engine throws NetworkRequestAttemptedException directly
        assertFailsWith<NetworkRequestAttemptedException> {
            client.get("https://example.com")
        }

        client.close()
    }
}
```

### Apache HttpClient 5

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ApacheHttpClientTest {
    @Test
    @BlockNetworkRequests
    fun `Apache HttpClient is blocked`() {
        val client = HttpClients.createDefault()
        val request = HttpGet("https://example.com")

        // Throws NetworkRequestAttemptedException directly
        assertFailsWith<NetworkRequestAttemptedException> {
            client.execute(request)
        }
    }
}
```

## Advanced Configuration

### Block All Tests by Default

Use `applyToAllTests = true` to block network for all tests:

```kotlin
junitNoNetwork {
    enabled = true
    applyToAllTests = true // Block by default
}
```

Then opt-out specific tests:

```kotlin
@Test
@AllowNetworkRequests
fun `this test can make network requests`() {
    // Network allowed
}
```

### Allow Specific Hosts

```kotlin
import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.jupiter.api.Test
import java.net.Socket

class LocalhostTest {
    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(["localhost", "127.0.0.1"])
    fun `can connect to localhost`() {
        // Localhost connections are allowed
        try {
            Socket("localhost", 8080).close()
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Localhost should be allowed", e)
        } catch (e: Exception) {
            // Other exceptions (connection refused, etc.) are OK
        }
    }
}
```

### Wildcard Patterns

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*.example.com", "*.test.local"])
fun `allows subdomains`() {
    // ✅ api.example.com - allowed
    // ✅ www.example.com - allowed
    // ❌ example.com - blocked (doesn't match *.example.com)
    // ❌ other.com - blocked
}
```

### Block Specific Hosts Only

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*"]) // Allow all
@BlockRequestsToHosts(["evil.com", "tracking.example.com"]) // Except these
fun `blocks specific hosts`() {
    // ✅ Most hosts are allowed
    // ❌ evil.com - blocked
    // ❌ tracking.example.com - blocked
}
```

## Running Tests

### Gradle Command Line

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests NetworkBlockingTest

# Run with debug output
./gradlew test --info -Djunit.nonetwork.debug=true
```

### IntelliJ IDEA

1. Add JVM args to your test configuration:
   - Run → Edit Configurations
   - Add VM options: `-Djava.security.manager=allow`

2. Or configure globally in `.idea/workspace.xml`:
```xml
<component name="RunManager">
  <configuration default="true" type="JUnit">
    <option name="VM_PARAMETERS" value="-Djava.security.manager=allow" />
  </configuration>
</component>
```

## Troubleshooting

### Issue: "UnsupportedOperationException: The Security Manager is deprecated and will be removed in a future release"

**Solution**: Add JVM arg `-Djava.security.manager=allow` for Java 21+:

```kotlin
tasks.withType<Test> {
}
```

### Issue: Tests pass when they should fail

**Checklist**:
1. Is `@BlockNetworkRequests` annotation present?
2. Is the Gradle plugin applied correctly?
3. Is `junit-platform.properties` configured (if not using plugin)?
4. Check with debug mode: `-Djunit.nonetwork.debug=true`

### Issue: OkHttp exception message is unclear

OkHttp wraps `NetworkRequestAttemptedException` in `IOException`. Check the message:

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

## Complete Example Project

See the `plugin-integration-tests/jvm-junit5` module for a complete working example with:
- Gradle plugin configuration
- Network blocking tests
- HTTP client tests (OkHttp, Ktor CIO, Retrofit, Apache HttpClient5, Reactor Netty, AsyncHttpClient)
- Advanced configuration examples

## See Also

- [Compatibility Matrix](../compatibility-matrix.md) - Full compatibility information
- [HTTP Client Guides](../clients/) - Detailed guides for each HTTP client
- [Advanced Configuration](../advanced-configuration.md) - All configuration options
