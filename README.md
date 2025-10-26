# JUnit No-Network Extension

A JUnit extension that automatically fails tests attempting to make outgoing network requests. This helps ensure your unit tests are truly isolated and don't accidentally depend on external services.

[![Java 21+](https://img.shields.io/badge/Java-21+-blue.svg)](docs/compatibility-matrix.md)
[![JUnit 4 & 5](https://img.shields.io/badge/JUnit-4%20%26%205-green.svg)](docs/compatibility-matrix.md)
[![KMP Support](https://img.shields.io/badge/KMP-JVM%20%7C%20Android-orange.svg)](docs/setup-guides/kmp-junit5.md)

---

## 📋 Quick Links

- **[Compatibility Matrix](docs/compatibility-matrix.md)** - Java versions, JUnit versions, platforms, and HTTP clients
- **[Setup Guides](docs/setup-guides/)** - Step-by-step guides for JVM, Android, and KMP
- **[HTTP Client Guides](docs/clients/)** - OkHttp, Retrofit, Ktor, and more
- **[Advanced Configuration](docs/advanced-configuration.md)** - All configuration options

---

## ✨ Features

- ✅ **Automatic network blocking** - Fail tests that attempt network requests
- ✅ **JUnit 5 and JUnit 4 support** - Works with both frameworks
- ✅ **Multiplatform** - JVM, Android (full support); iOS (API structure only)
- ✅ **Zero configuration** - Gradle plugin handles everything
- ✅ **Fine-grained control** - Allow/block specific hosts with wildcards
- ✅ **Clear error messages** - Detailed information about attempted requests

## 🚀 Quick Start

### 1. Add the Gradle Plugin

```kotlin
plugins {
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-beta.1"
}

junitNoNetwork {
    enabled = true
}
```

### 2. Write Tests

**JUnit 5:**
```kotlin
@ExtendWith(NoNetworkExtension::class)
class MyTest {
    @Test
    @BlockNetworkRequests
    fun `should not make network requests`() {
        // This will throw NetworkRequestAttemptedException
        Socket("example.com", 80)
    }
}
```

**JUnit 4:**
```kotlin
class MyTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun shouldNotMakeNetworkRequests() {
        // This will throw NetworkRequestAttemptedException
        Socket("example.com", 80)
    }
}
```

That's it! 🎉

---

## 📊 Compatibility at a Glance

### Java Versions

| Version | Status | Notes |
|---------|--------|-------|
| 21+ | ✅ Supported | Single JVMTI agent binary works across all 21+ versions |
| 17-20 | ❌ Not Supported | Requires Java 21+ |

**Java version compatibility**: The JVMTI agent is compiled once per platform and works across all Java 21+ versions due to JVMTI's stable API and backward compatibility. You do NOT need different agent binaries for Java 21 vs 22 vs 25. [Learn more →](docs/architecture/java-version-compatibility.md)

### JUnit Versions

| Framework | Status | Guide |
|-----------|--------|-------|
| JUnit 4.13.2 | ✅ Supported | [JVM](docs/setup-guides/jvm-junit4.md) / [Android](docs/setup-guides/android-junit4.md) / [KMP](docs/setup-guides/kmp-junit4.md) |
| JUnit 5.11.3 | ✅ Supported | [JVM](docs/setup-guides/jvm-junit5.md) / [KMP](docs/setup-guides/kmp-junit5.md) |

### Platforms

| Platform | OS/Architecture | Status | Guide |
|----------|-----------------|--------|-------|
| JVM | macOS ARM64 | ✅ Supported | [JUnit 5](docs/setup-guides/jvm-junit5.md) / [JUnit 4](docs/setup-guides/jvm-junit4.md) |
| JVM | macOS Intel, Linux, Windows | 🚧 Planned | See [platform roadmap](docs/architecture/java-version-compatibility.md#platform-matrix) |
| Android (Robolectric) | All | ✅ Supported | [Setup Guide](docs/setup-guides/android-junit4.md) |
| KMP (JVM + Android) | macOS ARM64 + Android | ✅ Supported | [JUnit 5](docs/setup-guides/kmp-junit5.md) / [JUnit 4](docs/setup-guides/kmp-junit4.md) |
| iOS | - | ⚠️ API Only | No active blocking (Kotlin/Native limitation) |

**Note**: Native JVMTI agent currently built for macOS ARM64. Linux, Windows, and macOS Intel support coming soon.

### HTTP Clients

All tested and supported:

| Client | JVM | Android | Guide |
|--------|-----|---------|-------|
| OkHttp | ✅ | ✅ | [Guide](docs/clients/okhttp.md) |
| Retrofit | ✅ | ✅ | [Guide](docs/clients/retrofit.md) |
| Ktor (CIO) | ✅ | - | [Guide](docs/clients/ktor.md) |
| Ktor (OkHttp) | ✅ | ✅ | [Guide](docs/clients/ktor.md) |
| Ktor (Java) | ✅ | - | [Guide](docs/clients/ktor.md) |
| Apache HttpClient5 | ✅ | ✅ | - |
| Reactor Netty | ✅ | - | - |
| AsyncHttpClient | ✅ | - | - |
| Spring WebClient | ✅ | - | - |
| OpenFeign | ✅ | - | - |
| Fuel | ✅ | - | - |
| Volley | - | ✅ | - |

**Full compatibility details**: [Compatibility Matrix](docs/compatibility-matrix.md)

---

## 📖 Documentation

### Setup Guides

Choose your project type:

- **[JVM + JUnit 5](docs/setup-guides/jvm-junit5.md)** - Pure JVM projects with JUnit 5
- **[JVM + JUnit 4](docs/setup-guides/jvm-junit4.md)** - Pure JVM projects with JUnit 4
- **[Android + JUnit 4 + Robolectric](docs/setup-guides/android-junit4.md)** - Android projects
- **[KMP + JUnit 5](docs/setup-guides/kmp-junit5.md)** - Kotlin Multiplatform with JUnit 5
- **[KMP + JUnit 4](docs/setup-guides/kmp-junit4.md)** - Kotlin Multiplatform with JUnit 4
- **[Gradle Plugin](docs/setup-guides/gradle-plugin.md)** - Complete plugin reference

### HTTP Client Guides

- **[OkHttp](docs/clients/okhttp.md)** - Most popular Android/JVM HTTP client
- **[Retrofit](docs/clients/retrofit.md)** - Type-safe HTTP client for Android/JVM
- **[Ktor](docs/clients/ktor.md)** - Modern Kotlin Multiplatform HTTP client

### Configuration & Advanced Usage

- **[Compatibility Matrix](docs/compatibility-matrix.md)** - Complete compatibility information
- **[Advanced Configuration](docs/advanced-configuration.md)** - All configuration options

---

## 💡 Usage Examples

### Block Network by Default

```kotlin
junitNoNetwork {
    applyToAllTests = true // Block by default
}
```

```kotlin
@Test
fun test1() {
    // Network blocked automatically
}

@Test
@AllowNetworkRequests // Opt-out
fun test2() {
    // Network allowed
}
```

### Allow Specific Hosts

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "*.test.local"])
fun testWithLocalServer() {
    // ✅ localhost - allowed
    // ✅ api.test.local - allowed
    // ❌ example.com - blocked
}
```

### Wildcard Patterns

```kotlin
@AllowRequestsToHosts(["*.staging.mycompany.com"])
// ✅ api.staging.mycompany.com
// ✅ auth.staging.mycompany.com
// ❌ api.production.mycompany.com
```

### Kotlin Multiplatform + Ktor

```kotlin
// commonTest/ApiClientTest.kt
@Test
@BlockNetworkRequests
fun testSharedClient() = runTest {
    val client = HttpClientFactory.create() // CIO on JVM, OkHttp on Android

    assertFailsWith<NetworkRequestAttemptedException> {
        client.get("https://api.example.com/users")
    }
}
```

**More examples**: See the [Setup Guides](docs/setup-guides/) and [HTTP Client Guides](docs/clients/).

---

## 🛠️ How It Works

### JVM and Android Implementation

Uses JVMTI (JVM Tool Interface) agent for network blocking:
1. C++ JVMTI agent intercepts socket and DNS operations at the native level
2. Agent automatically packaged with library and extracted at runtime
3. Socket connections checked against configuration (allowed/blocked hosts)
4. Unauthorized connections throw `NetworkRequestAttemptedException`
5. Works seamlessly with all JVM-based platforms including Robolectric for Android

**Key Benefits**:
- Works on Java 21+ (no SecurityManager dependency)
- Intercepts network calls at the lowest possible level
- Supports both hostname and IP address blocking
- Includes DNS interception for reliable blocking
- Platform-agnostic: same implementation for JVM and Android

### iOS Implementation

**Status**: API structure only, no active blocking. iOS uses Kotlin/Native which doesn't support JVMTI.

**For iOS testing**: Use mocking frameworks or custom network interception.

---

## 🔍 Supported Clients

**All tested** with comprehensive integration tests:

- ✅ **Direct sockets**: `Socket`, `ServerSocket`
- ✅ **Standard HTTP**: `HttpURLConnection`, `HttpClient`
- ✅ **OkHttp**: 4.12.0 ([Guide](docs/clients/okhttp.md))
- ✅ **Retrofit**: 2.11.0 ([Guide](docs/clients/retrofit.md))
- ✅ **Ktor**: 2.3.7 (CIO, OkHttp, Java engines) ([Guide](docs/clients/ktor.md))
- ✅ **Apache HttpClient5**: 5.3.1
- ✅ **Reactor Netty HTTP**: 1.1.22
- ✅ **AsyncHttpClient**: 3.0.0
- ✅ **Spring WebClient**: 6.2.0
- ✅ **OpenFeign**: 13.5
- ✅ **Fuel**: 2.3.1
- ✅ **Android Volley**: 1.2.1

**Exception handling varies by client**:
- **Direct exceptions**: Ktor CIO, Apache HttpClient5, Reactor Netty, AsyncHttpClient
- **Wrapped in IOException**: OkHttp, Retrofit, Ktor OkHttp engine

See [HTTP Client Guides](docs/clients/) for details.

---

## 📦 Installation

### Gradle Plugin (Recommended)

```kotlin
plugins {
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-beta.1"
}

junitNoNetwork {
    enabled = true
}
```

### Manual Dependency

```kotlin
dependencies {
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-beta.1")
}
```

**Note**: Manual setup requires additional configuration. See [Setup Guides](docs/setup-guides/) for details.

---

## ⚙️ Configuration

### Gradle Plugin

```kotlin
junitNoNetwork {
    enabled = true // Enable plugin
    applyToAllTests = false // Block all tests by default
    libraryVersion = "0.1.0-beta.1" // Library version
    allowedHosts = listOf("localhost", "*.test.local") // Allowed hosts
    blockedHosts = listOf("evil.com") // Blocked hosts
    debug = false // Debug logging
    injectJUnit4Rule = false // Auto-inject @Rule for JUnit 4 (experimental)
}
```

**Complete configuration**: [Advanced Configuration Guide](docs/advanced-configuration.md)

---

## 🧪 Testing

### Test Statistics

**156 tests** across all platforms:
- **JVM unit tests**: 23 tests (SecurityManager, JUnit 4/5)
- **Android unit tests**: 7 tests (Robolectric)
- **JVM integration tests**: 30 tests (all HTTP clients)
- **Android integration tests**: 31 tests (OkHttp, HttpURLConnection)
- **Common tests**: 6 tests (platform-agnostic)
- **Integration test app**: 59 tests (real Maven dependency)

All tests passing ✅

### Running Tests

```bash
# All tests
make test

# JVM only
make test-jvm

# Android only
make test-android

# Integration tests
make test-integration

# Plugin integration tests
make test-plugin-integration
```

---

## ⚡ Performance

The JVMTI agent loads **once** at JVM startup and has minimal per-test overhead.

### Quick Summary

- **Agent loading**: ONE TIME at JVM startup (~5-10ms)
- **Per-test overhead**: ~100-500 nanoseconds (ThreadLocal configuration)
- **Real-world impact**: <10% for tests doing meaningful work

### Benchmark Results

From benchmark suite (100 iterations, Java 21):

| Test Type | Overhead | Notes |
|-----------|----------|-------|
| Empty Test | +458 ns (+183%) | High % but negligible absolute time |
| Array Sorting (4.2ms) | +270 μs (+6.4%) | Realistic test - low overhead |

**Key insight**: The small constant overhead (~500ns) appears as high percentage for nanosecond operations, but is negligible for real tests.

### Common Misconceptions

**❌ "The agent loads/unloads every test"**
**✅ Reality**: The JVMTI agent loads ONCE at JVM startup. Per-test operations only set ThreadLocal configuration (~500ns).

**❌ "400% overhead means tests run 4x slower"**
**✅ Reality**: High percentage only appears on nanosecond-scale operations. Real tests have <10% overhead.

**[Learn more about JVMTI loading behavior →](docs/architecture/jvmti-loading.md)**

Run benchmarks: `make benchmark`

---

## 🤝 Contributing

Contributions welcome! This project was developed using **Test-Driven Development (TDD)**:

1. Write failing tests (RED)
2. Write minimal code to pass (GREEN)
3. Refactor while keeping tests green (REFACTOR)

**Process**:
1. Fork the repository
2. Write failing tests for your feature/fix
3. Implement the minimal code
4. Ensure all tests pass
5. Submit a pull request

---

## 📝 License

MIT License - See [LICENSE](LICENSE) for details

---

## 🙏 Credits

Developed using TDD with comprehensive test coverage to ensure reliability and maintainability.

**Test coverage**: 156 tests covering all platforms, frameworks, and HTTP clients.

---

## 🔧 Troubleshooting

### Common Issues

**Tests Pass But Network Requests Go Through**

1. **Check JVMTI Agent is Loaded**
   - Enable debug logging: `-Djunit.nonetwork.debug=true`
   - Look for "JVMTI agent installed" messages
   - Agent must be loaded at JVM startup via `-agentpath` or auto-extraction

2. **Verify Annotations Are Applied**
   - Check that `@BlockNetworkRequests` is on your test method/class
   - For JUnit 5: Verify `@ExtendWith(NoNetworkExtension::class)` is present
   - For JUnit 4: Verify `@Rule NoNetworkRule` field exists

3. **Check Configuration**
   - Ensure `allowedHosts` doesn't accidentally permit the request
   - Review wildcard patterns (e.g., `*` allows everything)
   - Check `blockedHosts` isn't overriding your intent

**JVMTI Agent Not Found**

```
WARNING: JVMTI agent not found at: /path/to/agent.dylib
```

**Solution**: The Gradle plugin should automatically extract and load the agent. If you see this:
- Run `./gradlew clean build` to rebuild
- Check that `junitNoNetwork { enabled = true }` in build.gradle.kts
- For manual setup, see [Gradle Plugin Guide](docs/setup-guides/gradle-plugin.md)

**UnsatisfiedLinkError on Native Library**

```
java.lang.UnsatisfiedLinkError: no junit-no-network-agent in java.library.path
```

**Solution**: Agent file doesn't match your platform/architecture.
- Verify your OS and arch: macOS ARM64 currently supported
- Linux, Windows, and macOS Intel support coming soon
- Check [Platform Compatibility](docs/compatibility-matrix.md)

**Tests Hang or Timeout**

**Cause**: Some HTTP clients have timeouts that prevent quick failure.

**Solution**:
```kotlin
// Reduce timeout for faster test failure
val client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofMillis(100))
    .build()
```

**Netty DNS Resolver Warning**

```
SEVERE: Unable to load io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider
```

**Solution**: Add Netty's macOS DNS resolver dependency:
```kotlin
testImplementation("io.netty:netty-resolver-dns-native-macos:4.1.115.Final:osx-aarch_64")
```

### Enable Debug Logging

To see detailed information about network blocking:

```kotlin
// Gradle
tasks.test {
    systemProperty("junit.nonetwork.debug", "true")
}
```

Or run tests with:
```bash
./gradlew test -Djunit.nonetwork.debug=true
```

### Getting Help

Still stuck? Check:
- [Compatibility Matrix](docs/compatibility-matrix.md) - Verify your setup is supported
- [Setup Guides](docs/setup-guides/) - Follow step-by-step instructions
- [GitHub Issues](https://github.com/garry-jeromson/junit-request-blocker/issues) - Report bugs or ask questions

---

## 🆘 Support

- **Documentation**: [docs/](docs/)
- **Issues**: [GitHub Issues](https://github.com/garry-jeromson/junit-request-blocker/issues)
- **Compatibility**: [Compatibility Matrix](docs/compatibility-matrix.md)

---

## 📚 Additional Resources

### Quick Reference

- [Compatibility Matrix](docs/compatibility-matrix.md) - What works where
- [Setup Guides](docs/setup-guides/) - Step-by-step setup
- [HTTP Client Guides](docs/clients/) - Client-specific examples
- [Advanced Configuration](docs/advanced-configuration.md) - All options

### Example Projects

Complete working examples in `plugin-integration-tests/`:
- `test-contracts` - Shared test assertions module (used by all projects below)
- `jvm-junit4` - JVM with JUnit 4
- `jvm-junit5` - JVM with JUnit 5
- `android-robolectric` - Android with Robolectric
- `kmp-junit4` - KMP with JUnit 4
- `kmp-junit5` - KMP with JUnit 5
- `kmp-kotlintest` - KMP with kotlin.test + JUnit 4 runtime
- `kmp-kotlintest-junit5` - KMP with kotlin.test + JUnit 5 runtime

All projects use the `test-contracts` module for generic, client-agnostic test assertions.

### Common Patterns

**Unit test pattern**:
```kotlin
@Test
@BlockNetworkRequests
fun testLogic() {
    // Test business logic
    // Network automatically blocked
}
```

**Integration test pattern**:
```kotlin
@Test
@AllowRequestsToHosts(["localhost", "*.staging.mycompany.com"])
fun testIntegration() {
    // Test with staging APIs allowed
}
```

**Repository pattern**:
```kotlin
interface UserRepository {
    suspend fun getUser(id: Int): User
}

// Test with fake implementation
class FakeUserRepository : UserRepository {
    override suspend fun getUser(id: Int) = User(id, "Fake")
}
```

---

**Made with ❤️ using Test-Driven Development**
