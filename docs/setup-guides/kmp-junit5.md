# Setup Guide: Kotlin Multiplatform + JUnit 5

Block network requests in Kotlin Multiplatform unit tests using JUnit 5.

## Requirements

- Java 21+ (uses JVMTI agent)
- Gradle 7.x+ (tested with 8.11.1)
- Kotlin 1.9+ (tested with 2.1.0)
- JUnit 5.8+ (tested with 5.11.3)

## Supported Platforms

| Platform | Network Blocking | Notes |
|----------|-----------------|-------|
| JVM | ✅ Fully Supported | JVMTI agent-based blocking |
| Android | ✅ Fully Supported | Requires Robolectric for unit tests |

**Note**: iOS is not supported. For iOS projects, use dependency injection and mocking instead.

## Installation

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library") // If targeting Android
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-beta.1"
}

kotlin {
    jvmToolchain(21)

    // Configure targets
    jvm()
    androidTarget() // If targeting Android

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.github.garryjeromson:junit-airgap:0.1.0-beta.1")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
            }
        }

        // For Android: requires Robolectric
        val androidUnitTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
                implementation("org.robolectric:robolectric:4.14")
            }
        }
    }
}

// Android configuration (if using Android target)
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true // Required for Robolectric
        }
    }
}
```

## Basic Usage

### Common Test (commonTest)

Write tests in `commonTest` that work across all platforms:

```kotlin
// commonTest/ApiClientTest.kt
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ApiClientTest {
    @Test
    @BlockNetworkRequests
    fun testNetworkBlocked() = runTest {
        val client = createPlatformHttpClient()

        try {
            // This test runs on JVM and Android
            assertFailsWith<NetworkRequestAttemptedException> {
                client.get("https://example.com")
            }
        } finally {
            client.close()
        }
    }
}
```

### Platform-Specific HTTP Clients

Use expect/actual pattern for platform-specific clients:

**commonMain:**
```kotlin
expect object HttpClientFactory {
    fun create(): HttpClient
}
```

**jvmMain:**
```kotlin
actual object HttpClientFactory {
    actual fun create() = HttpClient(CIO) // CIO engine for JVM
}
```

**androidMain:**
```kotlin
actual object HttpClientFactory {
    actual fun create() = HttpClient(OkHttp) // OkHttp engine for Android
}
```

## Exception Handling by Platform

Different platforms/engines throw different exception types:

| Platform | Engine | Exception Behavior |
|----------|--------|-------------------|
| JVM | CIO | Throws `NetworkRequestAttemptedException` directly |
| JVM | Java | Throws `NetworkRequestAttemptedException` directly |
| Android | OkHttp | Wraps in `IOException` (check message) |

**Cross-platform exception handling:**

```kotlin
try {
    client.get("https://example.com")
} catch (e: NetworkRequestAttemptedException) {
    // CIO/Java engines (JVM)
} catch (e: Exception) {
    // OkHttp engine (Android) - check message
    assertTrue(e.message?.contains("Network request blocked") == true)
}
```

## Advanced Configuration

### Block All Tests by Default

```kotlin
junitAirgap {
    applyToAllTests = true
}
```

### Allow Specific Hosts

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
fun testCanConnectToLocalhost() = runTest {
    // Localhost connections allowed
}
```

### Platform-Specific Configuration

```kotlin
junitAirgap {
    // JVM: strict blocking
    // Android: allow emulator localhost (10.0.2.2)
    allowedHosts = if (isAndroid) {
        listOf("localhost", "127.0.0.1", "10.0.2.2")
    } else {
        listOf("localhost", "127.0.0.1")
    }
}
```

## Running Tests

```bash
# Run all platform tests
./gradlew allTests

# Run JVM tests only
./gradlew jvmTest

# Run Android tests only
./gradlew testDebugUnitTest

# Run specific test class
./gradlew jvmTest --tests ApiClientTest

# Run with debug output
./gradlew allTests -Djunit.airgap.debug=true
```

## Troubleshooting

### Android Tests Not Using Robolectric

**Cause**: Missing `@RunWith(RobolectricTestRunner::class)` annotation.

**Solution**: Add Robolectric runner to Android test classes:

```kotlin
@RunWith(RobolectricTestRunner::class)
class MyAndroidTest {
    // ...
}
```

### Different Exception Types on Different Platforms

**Expected behavior**: CIO/Java throw directly, OkHttp wraps.

**Solution**: Handle both cases (see Exception Handling section above).

## Source Set Structure

```
src/
├── commonMain/kotlin/
│   └── HttpClientFactory.kt (expect declaration)
├── commonTest/kotlin/
│   └── ApiClientTest.kt (@Test with @BlockNetworkRequests)
├── jvmMain/kotlin/
│   └── HttpClientFactory.kt (actual - CIO engine)
├── jvmTest/kotlin/
│   └── JvmSpecificTest.kt
├── androidMain/kotlin/
│   └── HttpClientFactory.kt (actual - OkHttp engine)
└── androidUnitTest/kotlin/
    └── AndroidSpecificTest.kt (@RunWith(RobolectricTestRunner))
```

## Example Projects

See `plugin-integration-tests/` for complete working examples:
- **kmp-junit5** - Full KMP setup with JUnit 5
- **kmp-kotlintest-junit5** - Using kotlin.test with JUnit 5 runtime

## See Also

- [JVM + JUnit 5 Setup](jvm-junit5.md) - JVM-specific details
- [Android + JUnit 4 Setup](android-junit4.md) - Android/Robolectric details
- [Ktor Client Guide](../clients/ktor.md) - Platform-specific HTTP client details
- [Advanced Configuration](../advanced-configuration.md)
