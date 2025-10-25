# Setup Guide: JVM + JUnit 4

This guide shows how to set up the JUnit No-Network Extension for a pure JVM project using JUnit 4.

## Requirements

- Java 17-23 (Java 24+ not supported due to SecurityManager removal)
- Gradle 7.x or later (tested with 8.11.1)
- JUnit 4.12+ (tested with 4.13.2)
- Kotlin 1.9+ (tested with 2.1.0)

## Installation

### Option 1: Using the Gradle Plugin with Auto-Injection (Recommended)

The plugin can automatically inject `NoNetworkRule` into your test classes using bytecode enhancement:

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(21) // Or 17-23
}

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false // Use @BlockNetworkRequests explicitly
    injectJUnit4Rule = true // Enable automatic @Rule injection (experimental)
}

// Configure test tasks
tasks.withType<Test> {
    // NOTE: Do NOT use useJUnitPlatform() for pure JUnit 4 projects

    // Required for Java 21+
    jvmArgs("-Djava.security.manager=allow")
}
```

With auto-injection enabled, you don't need to manually add `@Rule` fields - just use `@BlockNetworkRequests`:

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.Test
import java.net.Socket

class MyTest {
    // No @Rule field needed - plugin injects it automatically!

    @Test
    @BlockNetworkRequests
    fun testNetworkBlocked() {
        Socket("example.com", 80) // Will throw NetworkRequestAttemptedException
    }
}
```

### Option 2: Manual @Rule Configuration

If you prefer explicit configuration or auto-injection doesn't work for your setup:

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<Test> {
    jvmArgs("-Djava.security.manager=allow") // Required for Java 21+
}
```

Then manually add `@Rule` to each test class:

```kotlin
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.Rule
import org.junit.Test
import java.net.Socket

class MyTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun testNetworkBlocked() {
        Socket("example.com", 80) // Throws NetworkRequestAttemptedException
    }
}
```

## Basic Usage

### Simple Network Blocking Test

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import org.junit.Rule
import org.junit.Test
import java.net.Socket
import kotlin.test.assertFailsWith

class MyTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun testNetworkBlocked() {
        // This will throw NetworkRequestAttemptedException
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
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import org.junit.Rule
import org.junit.Test
import java.net.Socket

class MyTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule(applyToAllTests = true)

    @Test
    fun testBlockedByDefault() {
        // Network is blocked because applyToAllTests=true
    }

    @Test
    @AllowNetworkRequests
    fun testExplicitlyAllowed() {
        // Network is allowed even though applyToAllTests=true
        Socket("example.com", 80).close()
    }
}
```

## Testing HTTP Clients

### OkHttp Client

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class OkHttpTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun testOkHttpBlocked() {
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
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFailsWith

class KtorCioTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun testKtorCioBlocked() = runBlocking {
        val client = HttpClient(CIO)

        // CIO engine throws NetworkRequestAttemptedException directly
        assertFailsWith<NetworkRequestAttemptedException> {
            client.get("https://example.com")
        }

        client.close()
    }
}
```

### Retrofit

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import kotlin.test.assertTrue

interface ApiService {
    @GET("/")
    fun getData(): retrofit2.Call<String>
}

class RetrofitTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun testRetrofitBlocked() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://example.com")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        try {
            service.getData().execute()
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            // Retrofit wraps the exception
            assertTrue(e.message?.contains("Network request blocked") == true)
        }
    }
}
```

## Advanced Configuration

### Block All Tests by Default

```kotlin
@get:Rule
val noNetworkRule = NoNetworkRule(applyToAllTests = true)
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
import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import org.junit.Rule
import org.junit.Test
import java.net.Socket

class LocalhostTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

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
fun testAllowsSubdomains() {
    // ✅ api.example.com - allowed
    // ✅ www.example.com - allowed
    // ❌ example.com - blocked (doesn't match *.example.com)
    // ❌ other.com - blocked
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
    jvmArgs("-Djava.security.manager=allow")
}
```

### Issue: Auto-injection not working

**Checklist**:
1. Is `injectJUnit4Rule = true` in plugin configuration?
2. Are test classes compiled before injection task runs? (Should be automatic)
3. Try manual `@Rule` configuration as fallback
4. Check with debug mode: `debug = true` in plugin configuration

### Issue: Tests pass when they should fail

**Checklist**:
1. Is `@BlockNetworkRequests` annotation present?
2. Is `@Rule val noNetworkRule = NoNetworkRule()` declared?
3. Is the Gradle plugin applied correctly?
4. Check with debug mode: `-Djunit.nonetwork.debug=true`

### Issue: Network blocking not working on Java 24+

**This is expected**. Java 24+ permanently removes SecurityManager. See the [Migration Guide](../migration-java24.md) for alternatives.

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

## JUnit 4 vs JUnit 5

**Key Differences:**

| Feature | JUnit 4 | JUnit 5 |
|---------|---------|---------|
| Configuration | `@Rule NoNetworkRule()` | `@ExtendWith(NoNetworkExtension::class)` |
| Auto-discovery | No (needs manual `@Rule`) | Yes (via `junit-platform.properties`) |
| Plugin auto-injection | Yes (bytecode enhancement) | Yes (properties file) |
| Test runner | JUnit 4 | JUnit Platform |

**When to use JUnit 4:**
- Legacy projects already using JUnit 4
- Android projects with Robolectric (better compatibility)
- Projects that can't migrate to JUnit 5 yet

**When to migrate to JUnit 5:**
- New projects
- Modern testing features needed (parameterized tests, nested tests, etc.)
- Better IDE support

See [JVM + JUnit 5 Setup Guide](jvm-junit5.md) for JUnit 5 migration.

## Complete Example Project

See the `plugin-integration-tests/jvm-junit4` module for a complete working example with:
- Gradle plugin configuration with auto-injection
- Network blocking tests
- HTTP client tests (OkHttp, Ktor CIO, Retrofit, Apache HttpClient5, Reactor Netty, AsyncHttpClient)
- Advanced configuration examples

## See Also

- [Compatibility Matrix](../compatibility-matrix.md) - Full compatibility information
- [HTTP Client Guides](../clients/) - Detailed guides for each HTTP client
- [Migration Guide: Java 24+](../migration-java24.md) - Migrating away from SecurityManager
- [Advanced Configuration](../advanced-configuration.md) - All configuration options
- [JVM + JUnit 5 Setup Guide](jvm-junit5.md) - Migrating to JUnit 5
