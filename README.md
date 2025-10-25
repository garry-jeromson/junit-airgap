# JUnit No-Network Extension

A JUnit extension that automatically fails tests attempting to make outgoing network requests. This helps ensure your unit tests are truly isolated and don't accidentally depend on external services.

[![Java 17-23](https://img.shields.io/badge/Java-17--23-blue.svg)](docs/compatibility-matrix.md)
[![JUnit 4 & 5](https://img.shields.io/badge/JUnit-4%20%26%205-green.svg)](docs/compatibility-matrix.md)
[![KMP Support](https://img.shields.io/badge/KMP-JVM%20%7C%20Android-orange.svg)](docs/setup-guides/kmp-junit5.md)

---

## 📋 Quick Links

- **[Compatibility Matrix](docs/compatibility-matrix.md)** - Java versions, JUnit versions, platforms, and HTTP clients
- **[Setup Guides](docs/setup-guides/)** - Step-by-step guides for JVM, Android, and KMP
- **[HTTP Client Guides](docs/clients/)** - OkHttp, Retrofit, Ktor, and more
- **[Advanced Configuration](docs/advanced-configuration.md)** - All configuration options
- **[Migration Guide: Java 24+](docs/migration-java24.md)** - Prepare for SecurityManager removal

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
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
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
| 17-23 | ✅ Supported | Fully functional |
| 24+ | ❌ Not Supported | SecurityManager removed ([JEP 486](https://openjdk.org/jeps/486)) |

**⚠️ Important**: See the [Migration Guide](docs/migration-java24.md) to prepare for Java 24+.

### JUnit Versions

| Framework | Status | Guide |
|-----------|--------|-------|
| JUnit 4.13.2 | ✅ Supported | [JVM](docs/setup-guides/jvm-junit4.md) / [Android](docs/setup-guides/android-junit4.md) / [KMP](docs/setup-guides/kmp-junit4.md) |
| JUnit 5.11.3 | ✅ Supported | [JVM](docs/setup-guides/jvm-junit5.md) / [KMP](docs/setup-guides/kmp-junit5.md) |

### Platforms

| Platform | Status | Guide |
|----------|--------|-------|
| JVM | ✅ Full Support | [JUnit 5](docs/setup-guides/jvm-junit5.md) / [JUnit 4](docs/setup-guides/jvm-junit4.md) |
| Android (Robolectric) | ✅ Full Support | [Setup Guide](docs/setup-guides/android-junit4.md) |
| KMP (JVM + Android) | ✅ Full Support | [JUnit 5](docs/setup-guides/kmp-junit5.md) / [JUnit 4](docs/setup-guides/kmp-junit4.md) |
| iOS | ⚠️ API Only | No active blocking |

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
- **[Migration Guide: Java 24+](docs/migration-java24.md)** - Prepare for SecurityManager removal

---

## ⚠️ Java 24+ Warning

**This library will stop working on Java 24+** when SecurityManager is permanently removed ([JEP 486](https://openjdk.org/jeps/486)).

### Timeline

- **Java 17-23**: ✅ Fully supported
- **Java 24+**: ❌ Will not work (SecurityManager removed)

### What to Do

**Start planning now**, even if you're on Java 17-21:

1. **Read the [Migration Guide](docs/migration-java24.md)**
2. **Choose a strategy**: Mocking, Dependency Injection, Repository Pattern, Testcontainers
3. **Refactor gradually**: Start with new code, migrate existing code over time

**Migration strategies**:
- **Mocking** - Use MockK, Mockito, or WireMock
- **Dependency Injection** - Inject HTTP clients for easy testing
- **Repository Pattern** - Abstract network calls behind interfaces
- **Testcontainers** - Integration tests with real services

See the [Migration Guide](docs/migration-java24.md) for complete details and examples.

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

### JVM Implementation

Uses Java's `SecurityManager` to intercept socket connections:
1. Before test runs, installs custom `SecurityManager`
2. Socket connections checked against configuration
3. Unauthorized connections throw `NetworkRequestAttemptedException`
4. After test, original `SecurityManager` restored

### Android Implementation

Same as JVM - uses `SecurityManager` with Robolectric.

### iOS Implementation

**Status**: API structure only, no active blocking. iOS has no SecurityManager equivalent.

**For iOS testing**: Use mocking frameworks or URLProtocol interception.

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

**Exception handling varies by client**:
- **Direct exceptions**: Ktor CIO, Apache HttpClient5, Reactor Netty, AsyncHttpClient
- **Wrapped in IOException**: OkHttp, Retrofit, Ktor OkHttp engine

See [HTTP Client Guides](docs/clients/) for details.

---

## 📦 Installation

### Gradle Plugin (Recommended)

```kotlin
plugins {
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

junitNoNetwork {
    enabled = true
}
```

### Manual Dependency

```kotlin
dependencies {
    testImplementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
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
    libraryVersion = "0.1.0-SNAPSHOT" // Library version
    allowedHosts = listOf("localhost", "*.test.local") // Allowed hosts
    blockedHosts = listOf("evil.com") // Blocked hosts
    debug = false // Debug logging
    injectJUnit4Rule = false // Auto-inject @Rule for JUnit 4 (experimental)
}
```

### Java 21+ Requirement

Java 21+ requires explicit SecurityManager permission:

```kotlin
tasks.withType<Test> {
    jvmArgs("-Djava.security.manager=allow")
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

**Design goal**: Zero overhead for tests that don't make network requests.

**Benchmark results**:
- Baseline tests: **< 1% overhead**
- CPU-intensive: **< 3% overhead**
- Memory operations: **< 6% overhead**
- I/O operations: **< 1% overhead**

All benchmarks pass **< 5% threshold** ✅

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

[Add your license here]

---

## 🙏 Credits

Developed using TDD with comprehensive test coverage to ensure reliability and maintainability.

**Test coverage**: 156 tests covering all platforms, frameworks, and HTTP clients.

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
- [Migration Guide](docs/migration-java24.md) - Prepare for Java 24+

### Example Projects

Complete working examples in `plugin-integration-tests/`:
- `jvm-junit4` - JVM with JUnit 4
- `jvm-junit5` - JVM with JUnit 5
- `android-robolectric` - Android with Robolectric
- `kmp-junit4` - KMP with JUnit 4
- `kmp-junit5` - KMP with JUnit 5
- `kmp-kotlintest` - KMP with kotlin.test

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
