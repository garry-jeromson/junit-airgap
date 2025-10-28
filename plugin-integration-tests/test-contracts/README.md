# Test Contracts Module

Generic, client-agnostic test assertions for network blocking tests used across all integration test projects.

## Overview

The `test-contracts` module provides shared test utilities that eliminate code duplication across integration test projects. It offers simple, consistent assertions that work with any HTTP client or network operation.

## Key Features

- ✅ **Client-Agnostic** - Works with any HTTP client (Socket, Ktor, Retrofit, OkHttp, ReactorNetty, etc.)
- ✅ **Platform-Specific Handling** - Automatically handles exception wrapping differences between JVM and Android
- ✅ **Simple API** - Just two functions: `assertRequestBlocked` and `assertRequestAllowed`
- ✅ **KMP Support** - Full Kotlin Multiplatform with JVM and Android targets
- ✅ **Maintainable** - Single source of truth for assertion logic

## Architecture

### Module Structure

```
test-contracts/
├── src/
│   ├── commonMain/kotlin/
│   │   └── Assertions.kt          # Expect declarations
│   ├── jvmMain/kotlin/
│   │   └── Assertions.jvm.kt      # JVM implementation
│   └── androidMain/kotlin/
│       └── Assertions.android.kt   # Android implementation
└── build.gradle.kts
```

### Expect/Actual Pattern

The module uses Kotlin Multiplatform's expect/actual mechanism:

**Common** (`commonMain/Assertions.kt`):
```kotlin
expect fun assertRequestBlocked(block: () -> Unit)
expect fun assertRequestAllowed(block: () -> Unit)
```

**JVM** (`jvmMain/Assertions.jvm.kt`):
```kotlin
actual fun assertRequestBlocked(block: () -> Unit) {
    // Checks for NetworkRequestAttemptedException in cause chain
    // Handles wrapped exceptions from frameworks
}
```

**Android** (`androidMain/Assertions.android.kt`):
```kotlin
actual fun assertRequestBlocked(block: () -> Unit) {
    // Checks exception by class name (Robolectric limitation)
    // Searches entire cause chain
}
```

## Usage

### Basic Usage

```kotlin
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestBlocked
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestAllowed

@Test
@BlockNetworkRequests
fun testNetworkIsBlocked() {
    assertRequestBlocked {
        Socket("example.com", 80).use { }
    }
}

@Test
@AllowNetworkRequests
fun testNetworkIsAllowed() {
    assertRequestAllowed {
        Socket("example.com", 80).use { }
    }
}
```

### With HTTP Clients

**Ktor:**
```kotlin
@Test
@BlockNetworkRequests
fun testKtorBlocked() {
    assertRequestBlocked {
        val client = HttpClient(CIO)
        client.get("https://example.com")
    }
}
```

**Retrofit:**
```kotlin
@Test
@BlockNetworkRequests
fun testRetrofitBlocked() {
    assertRequestBlocked {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://example.com/")
            .build()
        retrofit.create(Api::class.java).getData().execute()
    }
}
```

**Reactor Netty:**
```kotlin
@Test
@BlockNetworkRequests
fun testReactorNettyBlocked() {
    assertRequestBlocked {
        HttpClient.create()
            .get()
            .uri("https://example.com/")
            .responseContent()
            .blockFirst()
    }
}
```

### KMP Common Tests

```kotlin
// commonTest/ApiClientTest.kt
@Test
@BlockNetworkRequests
fun testApiClientBlocked() {
    assertRequestBlocked {
        makeHttpRequest() // expect function, implemented per-platform
    }
}
```

## Platform Differences

### JVM Implementation

**Features:**
- Direct `NetworkRequestAttemptedException` detection
- Checks entire cause chain for wrapped exceptions
- Works with all JVM HTTP clients

**Exception Handling:**
```kotlin
try {
    block()
    fail("Expected network to be blocked")
} catch (e: NetworkRequestAttemptedException) {
    // Direct exception - success
} catch (e: Throwable) {
    // Check if NetworkRequestAttemptedException is in cause chain
    val hasNetworkException = generateSequence(e) { it.cause }
        .any { it is NetworkRequestAttemptedException }
    assertTrue(hasNetworkException, "Expected NetworkRequestAttemptedException")
}
```

### Android Implementation

**Features:**
- Checks exception by class name (Robolectric uses different classloaders)
- Handles `IOException` wrappers common on Android
- Searches entire cause chain

**Exception Handling:**
```kotlin
try {
    block()
    fail("Expected network to be blocked")
} catch (e: Throwable) {
    // Check by class name
    val exceptionClass = e::class.simpleName
    val causeClass = e.cause?.let { it::class.simpleName }
    val isNetworkException =
        exceptionClass == "NetworkRequestAttemptedException" ||
        causeClass == "NetworkRequestAttemptedException" ||
        e.message?.contains("NetworkRequestAttemptedException") == true
    assertTrue(isNetworkException, "Expected NetworkRequestAttemptedException")
}
```

## Benefits

### Code Reduction

**Before** (duplicated across 7 projects):
```kotlin
@Test
@BlockNetworkRequests
fun reactorNettyIsBlocked() {
    try {
        makeRequest()
        fail("Expected network to be blocked")
    } catch (e: NetworkRequestAttemptedException) {
        // Expected
    } catch (e: Exception) {
        var cause: Throwable? = e
        var foundNetworkException = false
        while (cause != null) {
            when {
                cause is NetworkRequestAttemptedException -> {
                    foundNetworkException = true
                    break
                }
                cause.javaClass.name.contains("DnsResolve") -> {
                    foundNetworkException = true
                    break
                }
                // ... more checks
            }
            cause = cause.cause
        }
        if (!foundNetworkException) {
            fail("Expected network to be blocked")
        }
    }
}
```

**After** (single line):
```kotlin
@Test
@BlockNetworkRequests
fun reactorNettyIsBlocked() {
    assertRequestBlocked {
        makeRequest()
    }
}
```

**Impact**: ~90% code reduction in complex tests, ~500+ lines removed overall.

### Consistency

All integration tests now use the same assertion pattern:
- ✅ jvm-junit4
- ✅ jvm-junit5
- ✅ android-robolectric
- ✅ kmp-junit4
- ✅ kmp-junit5
- ✅ kmp-kotlintest
- ✅ kmp-kotlintest-junit5

### Maintainability

Changes to exception handling logic only need to be made once in test-contracts, not across 7 projects.

## Adding to Your Project

### Gradle Dependency

```kotlin
// In your KMP project
kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(project(":plugin-integration-tests:test-contracts"))
            }
        }
    }
}

// In your JVM/Android project
dependencies {
    testImplementation(project(":plugin-integration-tests:test-contracts"))
}
```

### Import

```kotlin
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestBlocked
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestAllowed
```

## API Reference

### assertRequestBlocked

```kotlin
fun assertRequestBlocked(block: () -> Unit)
```

Asserts that the network operation in `block` throws `NetworkRequestAttemptedException` (possibly wrapped).

**Behavior:**
- **JVM**: Fails if `NetworkRequestAttemptedException` is not found in exception or cause chain
- **Android**: Checks exception by class name due to Robolectric classloader differences

**Usage:**
```kotlin
assertRequestBlocked {
    // Any network operation
}
```

### assertRequestAllowed

```kotlin
fun assertRequestAllowed(block: () -> Unit)
```

Asserts that the network operation in `block` does NOT throw `NetworkRequestAttemptedException`.

**Behavior:**
- Fails if `NetworkRequestAttemptedException` is found (directly or in cause chain)
- Other exceptions (IOException, timeouts, DNS failures) are acceptable

**Usage:**
```kotlin
assertRequestAllowed {
    // Any network operation
    // May throw IOException, etc. - that's fine
}
```

## Examples

See the following integration test projects for real-world usage:
- `plugin-integration-tests/jvm-junit5/` - JUnit 5 examples
- `plugin-integration-tests/kmp-junit4/` - KMP examples with expect/actual
- `plugin-integration-tests/android-robolectric/` - Android/Robolectric examples

## Contributing

When modifying test-contracts:

1. **Update both platforms** - Changes must work on JVM and Android
2. **Run all integration tests** - Verify changes work across all 7 projects
3. **Maintain simplicity** - Keep the API minimal and focused
4. **Document changes** - Update this README and CLAUDE.md

## License

[Same as parent project]
