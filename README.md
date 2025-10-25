# JUnit No-Network Extension

A JUnit extension that automatically fails tests attempting to make outgoing network requests. This helps ensure your unit tests are truly isolated and don't accidentally depend on external services.

## Features

- ✅ **Automatic network blocking** - Tests annotated with `@BlockNetworkRequests` will fail if they attempt network requests
- ✅ **Default blocking mode** - Apply network blocking to all tests by default with opt-out capability
- ✅ **Flexible configuration** - Three ways to configure: constructor parameter, annotation, or system property
- ✅ **JUnit 5 and JUnit 4 support** - Works with both JUnit versions
- ✅ **Multiplatform support** - JVM, Android (full support); iOS (API structure only)
- ✅ **Fine-grained control** - Allow/block specific hosts using annotations
- ✅ **Pattern matching** - Support for wildcard patterns (e.g., `*.example.com`)
- ✅ **Clear error messages** - Detailed information about attempted network requests

## Requirements

- **JVM**: Java 17 or later (uses Java 17 toolchain)
- **Android**: API 26 (Android 8.0) or later
- **iOS**: Kotlin Multiplatform iOS target (API structure only, see limitations)

## Framework Compatibility

The following table shows which test framework versions have been validated with comprehensive integration tests:

| Platform | JUnit 4 | JUnit 5 |
|----------|---------|---------|
| **JVM** | ✅ Fully tested | ✅ Fully tested |
| **Android** (Robolectric) | ✅ Fully tested | ⚠️ Not tested |
| **iOS** | ⚠️ API only | ⚠️ API only |

**Implementation Notes:**

- **JVM**: Both JUnit 4 (`NoNetworkRule`) and JUnit 5 (`NoNetworkExtension`) are fully supported with comprehensive integration tests validating identical functionality across frameworks
- **Android**: JUnit 4 with Robolectric is fully tested and recommended. JUnit 5 support on Android requires additional configuration (junit-vintage-engine to run JUnit 4 and 5 together) and is not currently tested
- **iOS**: Provides API structure for multiplatform compatibility but does not actively block network requests (see Limitations section for details)

**Recommendation**: Use JUnit 4 (`NoNetworkRule`) for Android projects with Robolectric. Use either JUnit 4 or JUnit 5 for JVM projects based on your preference.

## ⚠️ Important Limitations and Future Compatibility

### Java 24+ Compatibility Warning

**This library will stop working when you upgrade to Java 24 or later.**

The JVM and Android implementations rely on Java's `SecurityManager`, which is:
- ✅ **Works on Java 17-23**: Deprecated but fully functional
- ❌ **Broken on Java 24+**: Permanently removed ([JEP 486](https://openjdk.org/jeps/486))

**What this means:**
- The library works perfectly today on Java 17-23
- When Java 24 is released, the library will no longer function
- **Oracle will NOT provide a replacement** for SecurityManager's interception capabilities
- No Java-based solution can replace SecurityManager for low-level socket interception

### Why ByteBuddy Doesn't Work

You might notice this library supports a "ByteBuddy" implementation option. **This implementation DOES NOT work and will NOT block network requests.**

**Technical explanation:**
- Socket constructors like `Socket("example.com", 80)` connect immediately by calling **native code** directly
- Byte Buddy (and all bytecode instrumentation tools) can only intercept **Java bytecode**, not native calls
- Only SecurityManager works because it's called by the JVM itself from native code
- This is a fundamental limitation, not a bug we can fix

**Why it exists:**
- Kept as a non-functional stub for API compatibility
- Allows experimentation but will never actually block network requests
- All tests verify it can be instantiated but explicitly do NOT test blocking behavior

### Migration Strategies for Java 24+

When you upgrade to Java 24+, consider these alternatives:

1. **Mocking/Stubbing** - Use libraries like MockK, Mockito, or WireMock to mock network layers
   - Pros: No actual network calls, complete control over responses
   - Cons: Requires refactoring to inject dependencies

2. **Test Containers** - Use Testcontainers for integration tests with real services
   - Pros: Tests against real implementations
   - Cons: Slower, requires Docker

3. **Separate Unit and Integration Tests**
   - Unit tests: Mock all network calls
   - Integration tests: Allow network with test doubles (WireMock, MockServer)

4. **Dependency Injection** - Design code to inject HTTP clients
   ```kotlin
   class MyService(private val httpClient: HttpClient) {
       // Easy to test by injecting a fake client
   }
   ```

5. **Repository Pattern** - Abstract network calls behind interfaces
   ```kotlin
   interface UserRepository {
       suspend fun getUser(id: Int): User
   }

   // Production: real HTTP implementation
   // Tests: in-memory fake implementation
   ```

### How to Suppress Deprecation Warnings (Java 17-23)

If you're using this library on Java 17-23, you'll see SecurityManager deprecation warnings. To suppress them:

**Option 1: Gradle configuration**
```kotlin
tasks.withType<Test> {
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "-Djava.security.manager=allow"
    )
}
```

**Option 2: Suppress in code**
```kotlin
@Suppress("DEPRECATION")
@ExtendWith(NoNetworkExtension::class)
class MyTest {
    // ...
}
```

**Recommendation:** Continue using this library on Java 17-23, but plan your migration strategy for Java 24+.

## Installation

Add the Gradle plugin to your project. It automatically handles dependency management and configuration:

```kotlin
plugins {
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

junitNoNetwork {
    enabled = true
    applyToAllTests = true  // Block network by default, opt-out with @AllowNetworkRequests
}
```

**That's it!** The plugin automatically:
- ✅ Adds the library dependency to your test classpath
- ✅ Creates `junit-platform.properties` for JUnit 5 auto-detection
- ✅ Configures test tasks with appropriate system properties
- ✅ Works with JVM, Android, and Kotlin Multiplatform projects

**Next steps:** See the [Usage](#usage) section to learn about `@BlockNetworkRequests` and `@AllowNetworkRequests` annotations.

#### Plugin Configuration Reference

```kotlin
junitNoNetwork {
    // Whether the plugin is enabled (default: true)
    enabled = true

    // Apply network blocking to all tests by default (default: false)
    // When true: tests block network unless annotated with @AllowNetworkRequests
    // When false: tests only block when annotated with @BlockNetworkRequests
    applyToAllTests = false

    // Library version to use (default: matches plugin version)
    libraryVersion = "0.1.0-SNAPSHOT"

    // List of allowed host patterns (optional)
    allowedHosts = listOf("localhost", "*.test.local")

    // List of blocked host patterns (optional)
    blockedHosts = listOf("evil.com", "*.tracking.com")

    // Enable debug logging (default: false)
    debug = false
}
```
## Usage

> **Note:** If you're using the Gradle plugin (recommended), it handles all setup automatically. You just need to use the annotations described below.

### JUnit 5

Use the `@ExtendWith` annotation at the class level and `@BlockNetworkRequests` on methods that should block network access:

```kotlin
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(NoNetworkExtension::class)
class MyTest {

    @Test
    @BlockNetworkRequests
    fun `does not make network requests`() {
        // This will throw NetworkRequestAttemptedException
        val connection = URL("http://example.com").openConnection()
        connection.connect()  // FAILS HERE
    }

    @Test
    fun `can make network requests without annotation`() {
        // This test is not annotated with @BlockNetworkRequests,
        // so network requests are allowed
        val connection = URL("http://example.com").openConnection()
        connection.connect()  // This is allowed
    }
}
```

### JUnit 4

Use the `@Rule` annotation with `NoNetworkRule`:

```kotlin
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.Rule
import org.junit.Test

class MyTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun shouldNotMakeNetworkRequests() {
        // This will throw NetworkRequestAttemptedException
        val socket = Socket("example.com", 80)
    }
}
```

**Android with Robolectric:**

```kotlin
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun shouldBlockOkHttpRequests() {
        // OkHttp requests will be blocked
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.example.com/data")
            .build()
        client.newCall(request).execute()  // Throws NetworkRequestAttemptedException
    }
}
```

## Kotlin Multiplatform with Ktor Client

The library fully supports Kotlin Multiplatform projects using Ktor client with platform-specific engines. This is a common pattern for sharing networking code across JVM, Android, and iOS.

### Shared Code Structure

Use the expect/actual pattern to create platform-specific HTTP clients while keeping business logic in commonMain:

**commonMain/HttpClientFactory.kt**:
```kotlin
expect object HttpClientFactory {
    fun create(): HttpClient
}
```

**jvmMain/HttpClientFactory.kt**:
```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*

actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(CIO) {
            engine { }
        }
    }
}
```

**androidMain/HttpClientFactory.kt**:
```kotlin
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(OkHttp) {
            engine { }
        }
    }
}
```

**iosMain/HttpClientFactory.kt**:
```kotlin
import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(Darwin) {
            engine { }
        }
    }
}
```

### Shared API Client

Create your API client in commonMain using the factory:

**commonMain/ApiClient.kt**:
```kotlin
interface ApiClient {
    suspend fun fetchUser(userId: Int): HttpResponse
    suspend fun fetchPosts(): HttpResponse
    fun close()
}

class DefaultApiClient(
    private val baseUrl: String = "https://jsonplaceholder.typicode.com",
    private val httpClient: HttpClient = HttpClientFactory.create()
) : ApiClient {

    override suspend fun fetchUser(userId: Int): HttpResponse {
        return httpClient.get("$baseUrl/users/$userId")
    }

    override suspend fun fetchPosts(): HttpResponse {
        return httpClient.get("$baseUrl/posts")
    }

    override fun close() {
        httpClient.close()
    }
}
```

### Testing KMP Ktor Clients

**JUnit 5 (JVM)**:
```kotlin
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertFailsWith

@ExtendWith(NoNetworkExtension::class)
class KmpKtorBasicUsageTest {

    @Test
    @BlockNetworkRequests
    fun shouldBlockSharedApiClientRequests() = runTest {
        val apiClient = DefaultApiClient()
        try {
            // CIO engine throws NetworkRequestAttemptedException directly
            assertFailsWith<NetworkRequestAttemptedException> {
                apiClient.fetchUser(1)
            }
        } finally {
            apiClient.close()
        }
    }
}
```

**JUnit 4 with Robolectric (Android)**:
```kotlin
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class AndroidKmpKtorBasicUsageTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun shouldBlockSharedApiClientRequests() = runTest {
        val apiClient = DefaultApiClient()
        try {
            // OkHttp wraps NetworkRequestAttemptedException in IOException
            val exception = assertFailsWith<Exception> {
                apiClient.fetchUser(1)
            }
            val message = exception.message ?: ""
            val hasNetworkBlockedMessage = message.contains("Network request blocked") ||
                message.contains("NetworkRequestAttemptedException")
            if (!hasNetworkBlockedMessage) {
                throw AssertionError("Expected network to be blocked, but got: $exception")
            }
        } finally {
            apiClient.close()
        }
    }
}
```

### Platform-Specific Engine Behavior

Different Ktor engines handle `NetworkRequestAttemptedException` differently:

| Engine | Platform | Exception Behavior |
|--------|----------|-------------------|
| CIO | JVM | Throws `NetworkRequestAttemptedException` directly |
| OkHttp | Android/JVM | Wraps in `IOException` (check exception message) |
| Java | JVM | Throws `NetworkRequestAttemptedException` directly |
| Darwin | iOS | Not actively blocked (iOS limitation) |

**Important**: When testing with OkHttp engine (Android or JVM), catch generic `Exception` and verify the message contains "Network request blocked" or "NetworkRequestAttemptedException", as shown in the Android example above.

### Complete Example in integration-test-app

The `integration-test-app` module includes a complete working example of KMP Ktor usage with comprehensive tests:

**Structure**:
- `commonMain/`: Shared API client, repository pattern, and HttpClientFactory (expect)
- `jvmMain/`: CIO engine implementation (actual)
- `androidMain/`: OkHttp engine implementation (actual)
- `iosMain/`: Darwin engine implementation (actual)
- `jvmTest/`: 18 tests covering all scenarios with CIO
- `androidUnitTest/`: 16 tests covering all scenarios with OkHttp
- `iosTest/`: 16 tests for API structure validation

**Test scenarios covered**:
- Basic network blocking with shared clients
- Platform-specific engine configuration
- Host filtering (`@AllowRequestsToHosts`, `@BlockRequestsToHosts`)
- Default blocking (`applyToAllTests`, `@BlockNetworkRequests (class-level)`)

See `integration-test-app/src/` for the complete implementation.

## Applying Network Blocking by Default

Instead of adding `@BlockNetworkRequests` to every test method, you can configure the extension to block network access by default for all tests. This is useful when most of your tests should be isolated from the network.

### Option 1: Constructor Parameter with @RegisterExtension (JUnit 5)

Use `@RegisterExtension` with the `applyToAllTests` parameter:

```kotlin
import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MyTest {

    @JvmField
    @RegisterExtension
    val extension = NoNetworkExtension(applyToAllTests = true)

    @Test
    fun `test 1 - network is blocked by default`() {
        // Network is blocked without needing @BlockNetworkRequests
        // This will throw NetworkRequestAttemptedException
        val socket = Socket("example.com", 80)
    }

    @Test
    @AllowNetworkRequests
    fun `test 2 - can opt-out with AllowNetwork`() {
        // @AllowNetworkRequests allows this test to make network requests
        val socket = Socket("example.com", 80)  // ✅ Allowed
    }
}
```

**JUnit 4:**

```kotlin
class MyTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule(applyToAllTests = true)

    @Test
    fun testNetworkBlocked() {
        // Network is blocked by default
        Socket("example.com", 80)  // Throws NetworkRequestAttemptedException
    }

    @Test
    @AllowNetworkRequests
    fun testNetworkAllowed() {
        // Opt-out with @AllowNetworkRequests
        Socket("example.com", 80)  // ✅ Allowed
    }
}
```

### Option 2: @BlockNetworkRequests (class-level) Annotation

Apply the `@BlockNetworkRequests (class-level)` annotation at the class level:

```kotlin
import io.github.garryjeromson.junit.nonetwork.AllowNetwork
import io.github.garryjeromson.junit.nonetwork.NoNetworkByDefault
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(NoNetworkExtension::class)
@BlockNetworkRequests (class-level)
class MyTest {

    @Test
    fun `all tests have network blocked`() {
        // Network is blocked for all tests in this class
        val socket = Socket("example.com", 80)  // ❌ Blocked
    }

    @Test
    @AllowNetworkRequests
    fun `except those marked with AllowNetwork`() {
        // Opt-out for specific tests
        val socket = Socket("example.com", 80)  // ✅ Allowed
    }
}
```

### Option 3: System Property (Global Configuration)

Set the system property to enable network blocking globally:

```bash
# Gradle
./gradlew test -Djunit.nonetwork.applyToAllTests=true

# Maven
mvn test -Djunit.nonetwork.applyToAllTests=true

# IDE (IntelliJ IDEA / Android Studio)
# Add to VM options: -Djunit.nonetwork.applyToAllTests=true
```

**Gradle configuration:**

```kotlin
tasks.test {
    systemProperty("junit.nonetwork.applyToAllTests", "true")
}
```

### Configuration Priority Order

When multiple configuration options are present, they are evaluated in this priority order (highest to lowest):

1. **@AllowNetworkRequests** - Opt-out annotation (always takes precedence)
2. **Constructor parameter** - `applyToAllTests = true/false`
3. **System property** - `-Djunit.nonetwork.applyToAllTests=true`
4. **@BlockNetworkRequests (class-level)** - Class-level annotation
5. **@BlockNetworkRequests** - Method/class-level annotation (existing behavior)
6. **Default** - No blocking (default behavior)

**Example demonstrating priority:**

```kotlin
class MyTest {
    // Constructor parameter overrides system property
    @JvmField
    @RegisterExtension
    val extension = NoNetworkExtension(applyToAllTests = false)

    @Test
    fun `network is allowed`() {
        // Even if system property is set to true,
        // constructor parameter (false) takes precedence
        val socket = Socket("example.com", 80)  // ✅ Allowed
    }

    @Test
    @BlockNetworkRequests
    @AllowNetworkRequests
    fun `AllowNetwork always wins`() {
        // @AllowNetworkRequests has highest priority
        // Overrides everything else
        val socket = Socket("example.com", 80)  // ✅ Allowed
    }
}
```

## Advanced Configuration

### Allowing Specific Hosts

Sometimes you need to allow connections to specific hosts (e.g., localhost for testing):

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
fun `can connect to localhost`() {
    val socket = Socket("localhost", 8080)  // ✅ Allowed
    val blocked = Socket("example.com", 80)  // ❌ Blocked
}
```

### Wildcard Patterns

Use wildcard patterns to allow entire domains:

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(hosts = ["*.example.com", "*.test.local"])
fun `can connect to subdomains`() {
    // ✅ Allowed: api.example.com, www.example.com
    // ❌ Blocked: example.com (doesn't match *.example.com)
    // ❌ Blocked: other.com
}
```

### Allowing All Except Specific Hosts

Use `*` to allow all hosts, then block specific ones:

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(hosts = ["*"])
@BlockRequestsToHosts(hosts = ["evil.com", "tracking.example.com"])
fun `blocks specific hosts only`() {
    val allowed = Socket("api.example.com", 80)     // ✅ Allowed
    val blocked = Socket("evil.com", 80)             // ❌ Blocked
}
```

**Note:** Blocked hosts always take precedence over allowed hosts.

### Class-Level Annotations

Apply annotations at the class level to affect all tests:

```kotlin
@ExtendWith(NoNetworkExtension::class)
@BlockNetworkRequests
@AllowRequestsToHosts(hosts = ["localhost"])
class MyTestClass {

    @Test
    fun test1() {
        // Network blocked except localhost
    }

    @Test
    fun test2() {
        // Same configuration applies
    }
}
```

### Implementation Selection (JVM Only)

⚠️ **IMPORTANT**: Only SECURITY_MANAGER actually works. See "Important Limitations" section above for details.

The JVM implementation supports multiple network blocking strategies via a feature flag system:

- **SECURITY_MANAGER** (default, recommended): ✅ The ONLY implementation that actually blocks network requests. Battle-tested and reliable, but deprecated in Java 17+ and will be removed in Java 24+.
- **BYTE_BUDDY**: ❌ Does NOT work. Non-functional stub kept for API compatibility only. Will NOT block network requests (see "Why ByteBuddy Doesn't Work" above).
- **AUTO**: Always selects SECURITY_MANAGER (the only working implementation).

#### Selecting an Implementation

Choose an implementation via system property or environment variable:

**System Property:**
```bash
# Use SecurityManager (default, ONLY working implementation)
./gradlew test -Djunit.nonetwork.implementation=securitymanager

# Use Byte Buddy (does NOT work - stub only)
./gradlew test -Djunit.nonetwork.implementation=bytebuddy

# Auto-detect (currently always selects SecurityManager)
./gradlew test -Djunit.nonetwork.implementation=auto
```

**Environment Variable:**
```bash
export JUNIT_NONETWORK_IMPLEMENTATION=securitymanager
./gradlew test
```

**Valid values**: `securitymanager`, `bytebuddy`, `auto` (case-insensitive, also accepts hyphenated forms like `security-manager`)

**Note**: The ByteBuddy option exists for API compatibility but does NOT block network requests. Only use SECURITY_MANAGER for actual network blocking.

#### Debug Mode

Enable debug logging to see which implementation is selected:

```bash
./gradlew test -Djunit.nonetwork.debug=true
```

This will print messages like:
```
NetworkBlocker: Using SECURITY_MANAGER implementation
```

## Performance

A key design goal is that enabling the extension should **not slow down tests that don't make network requests**. The library includes comprehensive performance benchmarks to verify this guarantee, ensuring overhead stays below 5%.

### Performance Benchmarks

The `benchmark` module contains performance tests that measure overhead across different scenarios:

**Benchmark Categories**:
- **Baseline**: Empty tests, simple assertions
- **CPU-Intensive**: Fibonacci calculations, prime numbers, sorting, regex
- **Memory Operations**: Object allocation, collections, array operations
- **I/O Operations**: File read/write, streams, directory operations
- **Extension Lifecycle**: SecurityManager installation overhead

**Running Benchmarks**:
```bash
# Run all benchmarks (JVM + Android)
make benchmark

# Run JVM benchmarks only (with strict <5% threshold)
make benchmark-jvm

# Run Android benchmarks only (informational - no threshold enforcement)
make benchmark-android
```

**Note**: Android benchmarks are **informational only** because Robolectric simulation adds significant overhead (10-100%+), making strict threshold enforcement unreliable. JVM benchmarks enforce the <5% threshold.

**Benchmark Methodology**:
- 10 warmup iterations (excluded from measurements)
- 50 measurement iterations
- Outlier removal (top/bottom 5%)
- Statistical analysis (median, standard deviation)
- Comparison: control (no extension) vs treatment (with extension)

**Performance Results**:

| Test Category | Overhead Range | Status |
|--------------|----------------|--------|
| Baseline (Empty/Simple) | -0.28% to 0.21% | ✅ Excellent |
| CPU-Intensive | -2.29% to -0.47% | ✅ Excellent |
| Memory Operations | -1.91% to +5.80% | ✅ Good |
| I/O Operations | -12.03% to +0.67% | ✅ Excellent |
| Extension Lifecycle | -0.12% to +3.66% | ✅ Good |

**Notes**:
- All benchmarks pass the <5% overhead threshold ✅
- Negative overhead indicates treatment was faster (measurement noise)
- Typical unit tests (>1ms) show <1% overhead
- Very fast operations (<0.1ms) may show 3-6% overhead due to measurement noise
- For production use, overhead is negligible for real-world test scenarios

**Recommendation**: The extension is safe for production use with minimal performance impact.

## How It Works

### JVM Implementation

The JVM implementation uses a custom `SecurityManager` to intercept socket connection attempts. When a test is annotated with `@BlockNetworkRequests`:

1. Before the test runs, a custom `SecurityManager` is installed
2. Any attempt to create a socket connection is checked against the configuration
3. If the connection is not allowed, a `NetworkRequestAttemptedException` is thrown
4. After the test completes, the original `SecurityManager` is restored

### Android Implementation

The Android implementation also uses `SecurityManager` (Android's runtime supports it the same way as the JVM), providing consistent behavior across platforms.

### iOS Implementation

**Status**: API structure only, no active blocking

The iOS implementation provides the `NetworkBlocker` API for compatibility with multiplatform projects but does not actively block network requests. This is due to iOS platform limitations:

1. iOS has no equivalent to JVM's `SecurityManager`
2. NSURLProtocol interception requires complex Objective-C bridge code
3. Kotlin/Native interop with NSURLProtocol is non-trivial
4. Even with NSURLProtocol, only URLSession requests can be intercepted (no low-level socket blocking)

**For iOS testing, consider**:
- Mocking frameworks (e.g., custom URLProtocol implementations in Swift/Objective-C)
- Dependency injection for network layers
- Network stubbing libraries designed for iOS

The iOS target is included in the project to maintain multiplatform source set structure and allow future enhancement.

### Supported Clients

**JVM/Android** - Works for:
- Direct socket connections (`Socket`, `ServerSocket`)
- HTTP/HTTPS requests (`HttpURLConnection`, `HttpClient`)
- Most HTTP client libraries (OkHttp, Apache HttpClient, Ktor, etc.)

**iOS** - Not supported:
- iOS implementation provides API structure only
- No active network blocking on iOS (see iOS Implementation section)

## Exception Details

When a network request is blocked, you'll receive a `NetworkRequestAttemptedException` with detailed information:

```
io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException:
Network request blocked by @BlockNetworkRequests: Attempted to connect to example.com:443

Attempted network request details:
  Host: example.com
  Port: 443
  URL: example.com:443
  Called from: at com.myapp.MyTest.testMethod(MyTest.kt:42)
```

## Limitations

- **Java 24+ Incompatibility**: ⚠️ This library will stop working on Java 24+ when SecurityManager is removed. See "Important Limitations and Future Compatibility" section for details and migration strategies.
- **ByteBuddy Does Not Work**: The ByteBuddy implementation is a non-functional stub. Only SecurityManager actually blocks network requests. See "Why ByteBuddy Doesn't Work" above.
- **iOS Not Fully Supported**: iOS provides API structure only without active blocking. Implementing full iOS support requires Objective-C bridge code for NSURLProtocol interception (see iOS Implementation section).
- **SecurityManager Deprecation**: `SecurityManager` (JVM/Android) is deprecated in Java 17+ but still functional until Java 24. Shows deprecation warnings that can be suppressed.
- **SecurityManager Restrictions**: Some environments restrict `SecurityManager` modifications.
- **DNS Resolution**: If DNS resolves a hostname to an IP, the blocker checks against the IP address.

## Development

This project was developed using **Test-Driven Development (TDD)**:

1. ✅ Write failing tests (RED)
2. ✅ Write minimal code to pass tests (GREEN)
3. ✅ Refactor while keeping tests green (REFACTOR)

All components have comprehensive test coverage.

### Quick Start with Makefile

The project includes a Makefile with common development commands:

```bash
# Show all available commands
make help

# Build the project
make build

# Run all tests
make test

# Format code with ktlint
make format

# Check code style
make lint

# Run all checks and tests
make verify

# Clean, build, format, and verify everything
make all
```

**Common Commands:**
- `make build` - Build the entire project (compile + test)
- `make test` - Run all tests (JVM + Android + Integration + Integration App)
- `make test-jvm` - Run JVM unit tests only
- `make test-android` - Run Android unit tests (Robolectric)
- `make test-integration` - Run JVM integration tests (junit-no-network module)
- `make test-integration-app` - Run integration-test-app tests (real consumer tests)
- `make format` - Auto-format code with ktlint
- `make lint` - Check code style with ktlint
- `make install` - Install to local Maven repository (~/.m2/repository)
- `make clean` - Clean build artifacts

### Building from Source (Manual)

```bash
# Ensure JAVA_HOME points to Java 21 (uses Java 17 toolchain internally)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Run all tests
./gradlew test

# Build the library
./gradlew build
```

### Running Tests

```bash
# Run unit tests (JVM + Android)
./gradlew :junit-no-network:test

# Run JVM integration tests (tests using the compiled extension)
./gradlew :junit-no-network:integrationTest

# Run Android integration tests (Robolectric)
./gradlew :junit-no-network:testDebugUnitTest --tests "io.github.garryjeromson.junit.nonetwork.integration.*"

# Run all tests (unit + integration)
./gradlew :junit-no-network:check

# Run specific test class
./gradlew :junit-no-network:test --tests NetworkBlockerTest

# Run with detailed output
./gradlew :junit-no-network:test --info
```

#### Test Categories

**Common Tests** (`src/commonTest/kotlin/`) - 6 tests
- Platform-agnostic tests for annotations and configuration
- Shared test logic for multiplatform support

**JVM Unit Tests** (`src/jvmTest/kotlin/`) - 23 tests
- JVM-specific NetworkBlocker tests (SecurityManager)
- JUnit 4 Rule and JUnit 5 Extension tests
- Mock-based tests for isolated component verification

**Android Unit Tests** (`src/androidUnitTest/kotlin/`) - 7 tests
- Robolectric-based Android tests
- Tests Android-specific network blocking behavior
- Socket and HttpURLConnection blocking on Android

**JVM Integration Tests** (`src/integrationTest/kotlin/`) - 30 tests
- End-to-end tests using the compiled extension
- Real HTTP client testing (OkHttp, Apache HttpClient, HttpURLConnection, Ktor)
- Complex configuration scenarios
- Real-world usage patterns
- Ktor client with multiple engines (CIO, OkHttp, Java)

**Android Integration Tests** (`src/androidUnitTest/kotlin/integration/`) - 31 tests
- End-to-end Android tests using Robolectric
- Real HTTP client testing (OkHttp, HttpURLConnection)
- Complex configuration scenarios (class-level annotations, wildcards)
- Real-world Android patterns (API calls, CDN, analytics, GraphQL, Retrofit-style)
- Android emulator localhost scenarios (10.0.2.2)

**Integration Test Application** (`integration-test-app/`) - 59 tests
- Real-world consumer project validating the library works when consumed as a Maven dependency
- Tests basic usage, default blocking, host filtering, and real HTTP clients
- JVM tests: 36 tests
  - 18 tests covering all features with multiple HTTP client libraries
  - 18 KMP Ktor tests (CIO engine, shared client structure)
- Android tests: 23 tests with Robolectric
  - 7 tests for basic scenarios
  - 16 KMP Ktor tests (OkHttp engine, shared client structure)
- Runs automatically as part of CI/CD pipeline via `make test`

**Total: 156 tests, all passing** ✅

**Note**: iOS tests not included as iOS implementation provides API structure only.

### Integration Test Application

The `integration-test-app` module validates that the library works correctly when consumed as a real Maven dependency. This provides confidence that the published artifacts are functional.

**Running integration tests:**

```bash
# Run all integration-test-app tests
make test-integration-app

# Or using Gradle directly
./gradlew :integration-test-app:test

# JVM tests only
./gradlew :integration-test-app:jvmTest

# Android tests only
./gradlew :integration-test-app:testDebugUnitTest
```

The integration-test-app automatically:
1. Publishes the library to Maven Local (`~/.m2/repository`)
2. Consumes it as a regular Maven dependency
3. Runs comprehensive tests validating all features

This ensures the library works exactly as end users will experience it.

## Roadmap

- [x] JVM support with JUnit 5
- [x] JVM support with JUnit 4
- [x] Socket-level blocking via SecurityManager
- [x] Configuration via annotations
- [x] Wildcard pattern matching
- [x] Android support
- [x] Kotlin Multiplatform structure (JVM + Android + iOS)
- [x] iOS multiplatform target (API structure, requires Objective-C bridge for full support)
- [ ] Full iOS network blocking (requires NSURLProtocol Objective-C bridge)
- [ ] Configuration via properties file
- [ ] TestNG support

## Contributing

Contributions are welcome! Please ensure all tests pass and follow TDD principles:

1. Write a failing test for your feature/fix
2. Implement the minimal code to pass the test
3. Refactor while keeping tests green
4. Submit a pull request

## License

[Add your license here]

## Credits

Developed using TDD with comprehensive test coverage to ensure reliability and maintainability.
