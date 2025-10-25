# Compatibility Matrix

This document provides a comprehensive overview of which Java versions, JUnit versions, platforms, and HTTP clients are supported by the JUnit No-Network Extension.

## Java Version Compatibility

| Java Version | Status | Notes |
|--------------|--------|-------|
| 17 | ✅ Fully Supported | Uses Java 17 toolchain |
| 18 | ✅ Fully Supported | SecurityManager deprecated but functional |
| 19 | ✅ Fully Supported | SecurityManager deprecated but functional |
| 20 | ✅ Fully Supported | SecurityManager deprecated but functional |
| 21 | ✅ Fully Supported | SecurityManager deprecated but functional |
| 22 | ✅ Fully Supported | SecurityManager deprecated but functional |
| 23 | ✅ Fully Supported | SecurityManager deprecated but functional |
| 24+ | ❌ **Not Supported** | **SecurityManager permanently removed** ([JEP 486](https://openjdk.org/jeps/486)) |

### Java 21+ Configuration

When using Java 21 or later, you need to explicitly allow SecurityManager usage:

```kotlin
tasks.withType<Test> {
    jvmArgs("-Djava.security.manager=allow")
}
```

## JUnit Version Compatibility

| JUnit Version | Status | Notes |
|---------------|--------|-------|
| JUnit 4.13.2 | ✅ Fully Supported | Use `NoNetworkRule` |
| JUnit 5.11.3 | ✅ Fully Supported | Use `NoNetworkExtension` |
| JUnit 4 (older) | ⚠️ Should Work | Versions 4.12+ likely compatible but not tested |
| JUnit 5 (older) | ⚠️ Should Work | Versions 5.8+ likely compatible but not tested |

## Platform Compatibility

| Platform | Status | Implementation | Notes |
|----------|--------|----------------|-------|
| **JVM** | ✅ Fully Supported | SecurityManager / SecurityPolicy | All features work |
| **Android** (API 26+) | ✅ Fully Supported | SecurityManager | Requires Robolectric for unit tests |
| **iOS** | ⚠️ API Structure Only | No-op | Provides API for KMP but doesn't block requests |

### Platform Details

#### JVM
- **Minimum**: Java 17
- **Recommended**: Java 21 or 23 (latest LTS)
- **Test Framework**: JUnit 4 or JUnit 5
- **Network Blocking**: Fully functional via SecurityManager
- **All HTTP Clients**: Supported

#### Android
- **Minimum SDK**: API 26 (Android 8.0)
- **Compile SDK**: API 35
- **Test Framework**: JUnit 4 with Robolectric (recommended)
- **JUnit 5 Support**: Requires additional configuration (junit-vintage-engine)
- **Network Blocking**: Fully functional via SecurityManager
- **All HTTP Clients**: Supported

#### iOS
- **Target**: Kotlin/Native iOS
- **Status**: API structure only for multiplatform compatibility
- **Network Blocking**: Not implemented
- **Limitation**: iOS has no SecurityManager equivalent
- **Alternative**: Use mocking frameworks or custom URLProtocol

## Network Blocker Implementation Compatibility

| Implementation | Status | Notes |
|----------------|--------|-------|
| SECURITY_MANAGER | ✅ Fully Working | Default, recommended, battle-tested |
| SECURITY_POLICY | ✅ Fully Working | Alternative declarative approach |
| BYTE_BUDDY | ❌ Non-Functional | Stub only, cannot intercept native Socket calls |

**Important**: Only `SECURITY_MANAGER` and `SECURITY_POLICY` actually block network requests. `BYTE_BUDDY` is a non-functional stub kept for API compatibility.

## HTTP Client Compatibility

All HTTP clients listed below are validated with comprehensive integration tests:

### ✅ Fully Tested Clients

| Client Library | Version Tested | Platforms | Exception Behavior |
|----------------|----------------|-----------|-------------------|
| **OkHttp** | 4.12.0 | JVM, Android | Wraps `NetworkRequestAttemptedException` in `IOException` |
| **Retrofit** | 2.11.0 | JVM, Android | Wraps exception (check message) |
| **Ktor Client (CIO)** | 2.3.7 | JVM | Throws `NetworkRequestAttemptedException` directly |
| **Ktor Client (OkHttp)** | 2.3.7 | JVM, Android | Wraps in `IOException` |
| **Ktor Client (Java)** | 2.3.7 | JVM | Throws `NetworkRequestAttemptedException` directly |
| **Apache HttpClient5** | 5.3.1 | JVM, Android | Throws `NetworkRequestAttemptedException` directly |
| **Reactor Netty HTTP** | 1.1.22 | JVM | Throws `NetworkRequestAttemptedException` directly |
| **AsyncHttpClient** | 3.0.0 | JVM | Throws `NetworkRequestAttemptedException` directly |
| **HttpURLConnection** | Java stdlib | JVM, Android | Throws `NetworkRequestAttemptedException` directly |
| **java.net.Socket** | Java stdlib | JVM, Android | Throws `NetworkRequestAttemptedException` directly |

### Exception Handling by Client

**Throws NetworkRequestAttemptedException Directly:**
```kotlin
assertFailsWith<NetworkRequestAttemptedException> {
    // CIO, Java HttpClient, Apache HttpClient5, Reactor Netty, AsyncHttpClient
    client.get("https://example.com")
}
```

**Wraps in IOException (OkHttp-based):**
```kotlin
val exception = assertFailsWith<Exception> {
    // OkHttp, Retrofit, Ktor OkHttp engine
    client.get("https://example.com")
}
assertTrue(
    exception.message?.contains("Network request blocked") == true ||
    exception.cause is NetworkRequestAttemptedException
)
```

## Kotlin Version Compatibility

| Kotlin Version | Status | Notes |
|----------------|--------|-------|
| 2.1.0 | ✅ Tested | Current version |
| 2.0.x | ⚠️ Likely Compatible | Not explicitly tested |
| 1.9.x | ⚠️ Likely Compatible | Not explicitly tested |

## Build Tool Compatibility

| Build Tool | Status | Notes |
|------------|--------|-------|
| Gradle 8.x | ✅ Fully Supported | Tested with 8.11.1 |
| Gradle 7.x | ⚠️ Should Work | Not tested |
| Maven | ⚠️ Should Work | Not tested, manual dependency configuration required |

## IDE Compatibility

| IDE | Status | Notes |
|-----|--------|-------|
| IntelliJ IDEA | ✅ Works | May need JVM args: `-Djava.security.manager=allow` |
| Android Studio | ✅ Works | Use with Robolectric tests |
| VS Code | ⚠️ Should Work | Not tested |

## Integration Test Coverage

The library includes integration tests for the following project configurations:

| Project Type | JUnit 4 | JUnit 5 | HTTP Clients Tested |
|--------------|---------|---------|---------------------|
| **JVM** | ✅ | ✅ | OkHttp, Retrofit, Ktor (CIO/OkHttp/Java), Apache HttpClient5, Reactor Netty, AsyncHttpClient |
| **Android** (Robolectric) | ✅ | ❌ | OkHttp, Retrofit, Ktor (OkHttp), Reactor Netty |
| **KMP** (JVM + Android) | ✅ | ✅ | Ktor (CIO on JVM, OkHttp on Android) |
| **KMP** (kotlin.test) | ✅ (JUnit 4 runtime) | ✅ (JUnit 5 runtime) | Ktor (CIO on JVM, OkHttp on Android) |

**Legend:**
- ✅ = Comprehensive integration tests
- ❌ = Not tested (should work with additional configuration)

## Quick Reference: What Works?

### ✅ What Works
- Java 17-23 on JVM
- JUnit 4 and JUnit 5
- Android API 26+ with Robolectric
- All major HTTP clients (OkHttp, Retrofit, Ktor, Apache HttpClient, etc.)
- Kotlin Multiplatform projects (JVM + Android targets)
- SecurityManager and SecurityPolicy implementations

### ❌ What Doesn't Work
- Java 24+ (SecurityManager removed)
- iOS network blocking (API structure only)
- ByteBuddy implementation (non-functional stub)
- Android instrumentation tests (Robolectric unit tests only)

### ⚠️ What Needs Configuration
- Java 21+: Requires `-Djava.security.manager=allow`
- OkHttp-based clients: Exception wrapping (check message)
- Android JUnit 5: Requires junit-vintage-engine

## See Also

- [Setup Guide: JVM + JUnit 5](setup-guides/jvm-junit5.md)
- [Setup Guide: JVM + JUnit 4](setup-guides/jvm-junit4.md)
- [Setup Guide: Android + JUnit 4](setup-guides/android-junit4.md)
- [Setup Guide: KMP + JUnit 4](setup-guides/kmp-junit4.md)
- [Setup Guide: KMP + JUnit 5](setup-guides/kmp-junit5.md)
- [Migration Guide: Java 24+](migration-java24.md)
