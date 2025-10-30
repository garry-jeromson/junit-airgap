<div align="center">
  <img src="docs/img/logo.png" alt="JUnit Airgap Logo" width="200"/>
  <h1>JUnit Airgap Extension</h1>
  <p><strong>Stop your unit tests from accidentally hitting real APIs.</strong></p>
</div>

[![Java 21+](https://img.shields.io/badge/Java-21+-blue.svg)](docs/compatibility-matrix.md)
[![JUnit 4 & 5](https://img.shields.io/badge/JUnit-4%20%26%205-green.svg)](docs/compatibility-matrix.md)
[![KMP Support](https://img.shields.io/badge/KMP-JVM%20%7C%20Android-orange.svg)](docs/setup-guides/kmp-junit5.md)

---

## Why Block Network Requests in Unit Tests?

Unit tests that make real network requests are:

- **Slow** - Network I/O is orders of magnitude slower than in-memory operations
- **Flaky** - Tests fail randomly due to network issues, timeouts, or service downtime
- **Dangerous** - Tests can accidentally modify production data or trigger side effects
- **Hidden** - Hard to spot network dependencies in code review
- **Environment-dependent** - Pass locally but fail in CI (or vice versa)

**The solution**: Automatically fail any test that attempts a network request. Force yourself to use mocks, fakes, or test doubles instead.

### The Problem

```kotlin
@Test
fun `calculates user stats`() {
    val stats = userService.calculateStats(userId = 123)

    assertEquals(42, stats.totalPurchases)
}
```

**What's wrong?** This test looks innocent, but if `userService` internally makes an HTTP request to fetch user data, you've got a hidden network dependency. The test will:
- Take 500ms+ instead of <1ms
- Fail when the API is down
- Potentially hit production servers

### The Fix

```kotlin
@Test
@BlockNetworkRequests  // ← Fails immediately if network is accessed
fun `calculates user stats`() {
    val fakeService = FakeUserService(
        users = listOf(User(id = 123, purchases = 42))
    )

    val stats = fakeService.calculateStats(userId = 123)

    assertEquals(42, stats.totalPurchases)  // Fast, reliable, isolated ✅
}
```

---

## Quick Start

### 1. Add the Gradle Plugin

```kotlin
plugins {
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-beta.1"
}
```

That's it for setup! The plugin automatically configures everything.

### 2. Annotate Your Tests

**JUnit 5:**
```kotlin
@Test
@BlockNetworkRequests
fun `test with no network access`() {
    // This will throw NetworkRequestAttemptedException
    Socket("example.com", 80)
}
```

**JUnit 4:**
```kotlin
@Test
@BlockNetworkRequests
fun testWithNoNetworkAccess() {
    // This will throw NetworkRequestAttemptedException
    Socket("example.com", 80)
}
```

### 3. Run Your Tests

```bash
./gradlew test
```

Any test annotated with `@BlockNetworkRequests` will now fail fast if it attempts network I/O:

```
io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException:
Network request blocked: example.com:80
    at MyTest.testWithNoNetworkAccess(MyTest.kt:15)
```

**That's it!** 🎉

---

## How It Works

### JVM & Android (Full Support)

Uses a **JVMTI agent** (JVM Tool Interface) to intercept network calls at the native level:

1. **C++ agent intercepts sockets and DNS** - Catches all network operations before they reach the network stack
2. **Auto-loaded at JVM startup** - Plugin automatically extracts and loads the native agent
3. **Checks against configuration** - Evaluates allowed/blocked hosts with wildcard support
4. **Fails fast** - Throws `NetworkRequestAttemptedException` immediately
5. **Works everywhere** - Same implementation for JVM and Android (via Robolectric)

**Key benefits:**
- ✅ Works on any Java version (JVMTI is not version-dependent)
- ✅ Requires Java 21+ for build (Kotlin Gradle Plugin requirement)
- ✅ Intercepts ALL HTTP clients (works at socket level)
- ✅ Catches both hostname and IP address connections
- ✅ Includes DNS interception for complete coverage
- ✅ Zero-configuration with Gradle plugin

---

## Compatibility Overview

### ✅ What's Supported

| Category | Status | Details |
|----------|--------|---------|
| **Java** | Java 21+ | Single JVMTI agent works across all 21+ versions |
| **JUnit** | 4.13.2 & 5.11.3 | Both frameworks fully supported |
| **Platform** | JVM (macOS ARM64) | Native agent currently for macOS ARM64 |
| **Platform** | Android (Robolectric) | Full support via Robolectric unit tests |
| **HTTP Clients** | All major clients | OkHttp, Retrofit, Ktor, Apache, Spring, etc. |

### 🚧 Coming Soon

- Linux (x86_64, ARM64)
- Windows (x86_64)
- macOS Intel (x86_64)

### ❌ Not Supported

- **iOS/Kotlin Native**: Platform limitations prevent comprehensive network interception. See `IOS_SUPPORT_INVESTIGATION.md` for technical details.

### 📖 Detailed Compatibility

For complete compatibility information including:
- Specific HTTP client versions tested
- Platform architecture details
- Exception handling by client
- Known limitations

See the **[Compatibility Matrix →](docs/compatibility-matrix.md)**

---

## Documentation

### 📚 Setup Guides

Step-by-step instructions for your project type:

- **[JVM + JUnit 5](docs/setup-guides/jvm-junit5.md)** - Pure JVM projects with JUnit 5
- **[JVM + JUnit 4](docs/setup-guides/jvm-junit4.md)** - Pure JVM projects with JUnit 4
- **[Android + Robolectric](docs/setup-guides/android-junit4.md)** - Android unit tests
- **[Kotlin Multiplatform + JUnit 5](docs/setup-guides/kmp-junit5.md)** - KMP with JUnit 5
- **[Kotlin Multiplatform + JUnit 4](docs/setup-guides/kmp-junit4.md)** - KMP with JUnit 4
- **[Gradle Plugin Reference](docs/setup-guides/gradle-plugin.md)** - Complete plugin configuration

### 🌐 HTTP Client Guides

Client-specific examples and exception handling:

- **[OkHttp](docs/clients/okhttp.md)** - Most popular Android/JVM HTTP client
- **[Retrofit](docs/clients/retrofit.md)** - Type-safe HTTP client
- **[Ktor](docs/clients/ktor.md)** - Kotlin Multiplatform HTTP client

### ⚙️ Configuration

- **[Advanced Configuration](docs/advanced-configuration.md)** - All configuration options
- **[Compatibility Matrix](docs/compatibility-matrix.md)** - Complete compatibility info

---

## Configuration Examples

### Block All Tests by Default

```kotlin
junitAirgap {
    applyToAllTests = true  // Block by default
}
```

```kotlin
@Test
fun test1() {
    // Network blocked automatically
}

@Test
@AllowNetworkRequests  // Opt-out when needed
fun test2() {
    // Network allowed
}
```

### Allow Specific Hosts

Perfect for testing with local servers or staging environments:

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "127.0.0.1", "*.staging.mycompany.com"])
fun testWithStagingAPI() {
    // ✅ localhost - allowed
    // ✅ api.staging.mycompany.com - allowed
    // ❌ api.production.mycompany.com - blocked
    // ❌ external-api.com - blocked
}
```

### Global Configuration

```kotlin
junitAirgap {
    enabled = true
    applyToAllTests = false
    allowedHosts = listOf("localhost", "*.test.local")
    blockedHosts = listOf("*.tracking.com")
    debug = false
}
```

**More examples**: [Advanced Configuration Guide →](docs/advanced-configuration.md)

---

## Supported HTTP Clients

All tested with comprehensive integration tests:

**Core:**
- ✅ Raw sockets (`Socket`, `ServerSocket`)
- ✅ Java HTTP (`HttpURLConnection`, `HttpClient`)

**Popular Libraries:**
- ✅ **OkHttp** 4.12.0
- ✅ **Retrofit** 2.11.0
- ✅ **Ktor** 2.3.7 (CIO, OkHttp, Java engines)
- ✅ **Apache HttpClient5** 5.3.1
- ✅ **Reactor Netty HTTP** 1.1.22
- ✅ **AsyncHttpClient** 3.0.0
- ✅ **Spring WebClient** 6.2.0
- ✅ **OpenFeign** 13.5
- ✅ **Fuel** 2.3.1
- ✅ **Android Volley** 1.2.1

**Exception handling varies by client** - some throw `NetworkRequestAttemptedException` directly, others wrap it in `IOException`. See [HTTP Client Guides](docs/clients/) for details.

---

## Installation Options

### Option 1: Gradle Plugin (Recommended)

Zero configuration - plugin handles everything automatically:

```kotlin
plugins {
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-beta.1"
}
```

### Option 2: Manual Dependency

Requires manual configuration (see [Setup Guides](docs/setup-guides/) for details):

```kotlin
dependencies {
    testImplementation("io.github.garryjeromson:junit-airgap:0.1.0-beta.1")
}
```

---

## Performance

The JVMTI agent loads **once** at JVM startup and has minimal overhead:

- **Agent loading**: ONE TIME at startup (~5-10ms)
- **Per-test overhead**: ~100-500 nanoseconds
- **Real-world impact**: <10% for tests doing meaningful work

### Benchmark Results

From benchmark suite (100 iterations, Java 21):

| Test Type | Overhead | Notes |
|-----------|----------|-------|
| Empty Test | +458 ns (+183%) | High % but negligible absolute time |
| Array Sorting (4.2ms) | +270 μs (+6.4%) | Realistic test - low overhead |

**Key insight**: Small constant overhead appears as high percentage for nanosecond operations, but is negligible for real tests.

Run benchmarks: `make benchmark`

**[Learn more about JVMTI performance →](docs/architecture/jvmti-loading.md)**

---

## Testing

```bash
# Run all tests
make test

# Run specific test suites
make test-jvm                     # JVM only
make test-android                 # Android only
make test-integration             # Integration tests
make test-plugin-integration      # Plugin integration tests
```

---

## Troubleshooting

### Tests Pass But Network Requests Go Through

**Checklist:**
1. ✅ Is `@BlockNetworkRequests` annotation present?
2. ✅ For JUnit 5: Is `@ExtendWith(AirgapExtension::class)` on class?
3. ✅ Is JVMTI agent loaded? (check with `-Djunit.airgap.debug=true`)

### JVMTI Agent Not Found

```
WARNING: JVMTI agent not found at: /path/to/agent.dylib
```

**Solution:**
```bash
./gradlew clean build  # Rebuild to extract agent
```

Verify plugin is enabled:
```kotlin
junitAirgap {
    enabled = true
}
```

### Platform Not Supported

```
java.lang.UnsatisfiedLinkError: no junit-airgap-agent in java.library.path
```

**Current support**: macOS ARM64 only

**Coming soon**: Linux, Windows, macOS Intel

See [Platform Compatibility](docs/compatibility-matrix.md) for details.

### Enable Debug Logging

See detailed information about what's being blocked:

```bash
./gradlew test -Djunit.airgap.debug=true
```

Or in `build.gradle.kts`:
```kotlin
tasks.test {
    systemProperty("junit.airgap.debug", "true")
}
```

### Getting Help

- 📖 [Setup Guides](docs/setup-guides/) - Step-by-step instructions
- 📊 [Compatibility Matrix](docs/compatibility-matrix.md) - Verify your setup
- 🐛 [GitHub Issues](https://github.com/garryjeromson/junit-airgap/issues) - Report bugs or ask questions

---

## Example Projects

Complete working examples in `plugin-integration-tests/`:

- **test-contracts** - Shared test assertions (used by all projects)
- **jvm-junit4** - JVM with JUnit 4
- **jvm-junit5** - JVM with JUnit 5
- **android-robolectric** - Android with Robolectric
- **kmp-junit4** - Kotlin Multiplatform with JUnit 4
- **kmp-junit5** - Kotlin Multiplatform with JUnit 5
- **kmp-kotlintest** - KMP with kotlin.test + JUnit 4 runtime
- **kmp-kotlintest-junit5** - KMP with kotlin.test + JUnit 5 runtime

---

## Contributing

Contributions welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## License

MIT License - See [LICENSE](LICENSE) for details

---

## Credits

Built with comprehensive test coverage across all platforms, frameworks, and HTTP clients.

---

**Made with ❤️ for better unit tests**
