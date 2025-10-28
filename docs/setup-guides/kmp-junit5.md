# Setup Guide: Kotlin Multiplatform + JUnit 5

This guide shows how to set up the JUnit Airgap Extension for a Kotlin Multiplatform (KMP) project using JUnit 5.

## Requirements

- Java 21+ (uses JVMTI agent for network blocking)
- Gradle 7.x or later (tested with 8.11.1)
- Kotlin 1.9+ (tested with 2.1.0)
- JUnit 5.8+ (tested with 5.11.3)

## Supported Platforms

| Platform | Network Blocking | Notes |
|----------|-----------------|-------|
| JVM | ✅ Fully Supported | JVMTI agent-based blocking |
| Android | ✅ Fully Supported | Requires Robolectric for unit tests |
| iOS | ⚠️ API Only | Provides API structure but doesn't block |

## Installation

Add the plugin to your KMP module's `build.gradle.kts`:

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library") // If targeting Android
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-SNAPSHOT"
}

// Configure the plugin
junitAirgap {
    enabled = true
    applyToAllTests = false
}

kotlin {
    jvmToolchain(21)

    // Configure targets
    jvm()
    androidTarget() // If targeting Android
    iosSimulatorArm64() // If targeting iOS

    sourceSets {
        // Common source set (shared code)
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:2.3.7")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.github.garryjeromson:junit-airgap:0.1.0-SNAPSHOT")
            }
        }

        // JVM source set
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:2.3.7") // CIO engine for JVM
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
            }
        }

        // Android source set (if using Android target)
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.7") // OkHttp engine for Android
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
                implementation("org.robolectric:robolectric:4.14")
            }
        }

        // iOS source set (if using iOS target)
        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.7") // Darwin engine for iOS
            }
        }
    }
}

// Android configuration (if using Android target)
android {
    namespace = "com.example.myapp"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Configure JUnit Platform for JVM tests
tasks.withType<Test> {
    useJUnitPlatform()
}
```

## Shared HTTP Client Pattern (Expect/Actual)

Create platform-specific HTTP clients while keeping business logic in `commonMain`:

### commonMain/HttpClientFactory.kt

```kotlin
expect object HttpClientFactory {
    fun create(): HttpClient
}
```

### jvmMain/HttpClientFactory.kt

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

### androidMain/HttpClientFactory.kt

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

### iosMain/HttpClientFactory.kt

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

## Testing Shared Code

### Common Test (commonTest)

Write tests in `commonTest` that work across all platforms:

```kotlin
// commonTest/ApiClientTest.kt
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import io.ktor.client.request.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ApiClientTest {
    @Test
    @BlockNetworkRequests
    fun testNetworkBlocked() = runTest {
        val client = HttpClientFactory.create()

        try {
            // This test runs on JVM and Android (not iOS - API only)
            assertFailsWith<NetworkRequestAttemptedException> {
                client.get("https://example.com")
            }
        } finally {
            client.close()
        }
    }
}
```

### JVM-Specific Test (jvmTest)

```kotlin
// jvmTest/JvmHttpClientTest.kt
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class JvmHttpClientTest {
    @Test
    @BlockNetworkRequests
    fun testKtorCioBlocked() = runTest {
        val client = HttpClient(CIO)

        // CIO engine throws NetworkRequestAttemptedException directly
        assertFailsWith<NetworkRequestAttemptedException> {
            client.get("https://example.com")
        }

        client.close()
    }
}
```

### Android-Specific Test (androidUnitTest)

```kotlin
// androidUnitTest/AndroidHttpClientTest.kt
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.AirgapRule
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AndroidHttpClientTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun testKtorOkHttpBlocked() = runTest {
        val client = HttpClient(OkHttp)

        try {
            client.get("https://example.com")
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            // OkHttp engine wraps NetworkRequestAttemptedException in IOException
            assertTrue(e.message?.contains("Network request blocked") == true)
        } finally {
            client.close()
        }
    }
}
```

## Platform-Specific Behavior

### Exception Handling by Platform/Engine

| Platform | Engine | Exception Behavior |
|----------|--------|-------------------|
| JVM | CIO | Throws `NetworkRequestAttemptedException` directly |
| JVM | Java | Throws `NetworkRequestAttemptedException` directly |
| Android | OkHttp | Wraps in `IOException` (check message) |
| iOS | Darwin | Not blocked (API only) |

**Important**: When testing OkHttp-based clients (Android), catch generic `Exception` and verify the message:

```kotlin
catch (e: Exception) {
    assertTrue(
        e.message?.contains("Network request blocked") == true ||
        e.cause is NetworkRequestAttemptedException
    )
}
```

## Advanced Configuration

### Block All Tests by Default

```kotlin
junitAirgap {
    enabled = true
    applyToAllTests = true // Block by default
}
```

### Allow Specific Hosts

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
fun testCanConnectToLocalhost() {
    // Localhost allowed
}
```

### Platform-Specific Configuration

Use `expect/actual` for platform-specific test configurations:

```kotlin
// commonTest
expect fun getPlatformAllowedHosts(): Array<String>

// jvmTest
actual fun getPlatformAllowedHosts(): Array<String> = arrayOf("localhost")

// androidUnitTest
actual fun getPlatformAllowedHosts(): Array<String> = arrayOf("localhost", "10.0.2.2")
```

## Running Tests

### Run All Platform Tests

```bash
# Run all tests (all platforms)
./gradlew allTests

# JVM tests only
./gradlew jvmTest

# Android tests only
./gradlew testDebugUnitTest

# Specific platform
./gradlew :mymodule:jvmTest
```

### Run Specific Test Class

```bash
# JVM
./gradlew jvmTest --tests ApiClientTest

# Android
./gradlew testDebugUnitTest --tests AndroidHttpClientTest
```

## Troubleshooting

### Issue: Tests fail on iOS

**Expected behavior**: iOS implementation is API-only and doesn't block network requests. Skip network blocking assertions for iOS or use conditional compilation:

```kotlin
@Test
fun testNetworkBlocking() {
    if (Platform.isIOS) {
        // Skip or use different assertion for iOS
        return
    }

    // Network blocking test for JVM/Android
    assertFailsWith<NetworkRequestAttemptedException> {
        // ...
    }
}
```

### Issue: "UnsupportedOperationException: The Security Manager is deprecated"

**Solution**: Add JVM arg for Java 21+ in test tasks:

```kotlin
tasks.withType<Test> {
}
```

### Issue: Android tests not using Robolectric

**Solution**: Add `@RunWith(RobolectricTestRunner::class)` to Android test classes:

```kotlin
@RunWith(RobolectricTestRunner::class)
class MyAndroidTest {
    // ...
}
```

### Issue: Different exception types on different platforms

**Solution**: Handle platform-specific exception wrapping:

```kotlin
try {
    client.get("https://example.com")
} catch (e: NetworkRequestAttemptedException) {
    // JVM with CIO/Java engines
} catch (e: Exception) {
    // Android with OkHttp engine - check message
    assertTrue(e.message?.contains("Network request blocked") == true)
}
```

## Source Set Structure

```
src/
├── commonMain/kotlin/          # Shared production code
│   ├── ApiClient.kt
│   └── HttpClientFactory.kt (expect)
├── commonTest/kotlin/          # Shared test code
│   └── ApiClientTest.kt
├── jvmMain/kotlin/             # JVM-specific production code
│   └── HttpClientFactory.kt (actual - CIO)
├── jvmTest/kotlin/             # JVM-specific tests (JUnit 5)
│   └── JvmHttpClientTest.kt
├── androidMain/kotlin/         # Android-specific production code
│   └── HttpClientFactory.kt (actual - OkHttp)
├── androidUnitTest/kotlin/     # Android-specific tests (JUnit 5 + Robolectric)
│   └── AndroidHttpClientTest.kt
├── iosMain/kotlin/             # iOS-specific production code
│   └── HttpClientFactory.kt (actual - Darwin)
└── iosTest/kotlin/             # iOS-specific tests (API structure only)
    └── IosHttpClientTest.kt
```

## Complete Example Projects

See the integration test projects for complete working examples:

- `plugin-integration-tests/kmp-junit5` - KMP with JUnit 5
- `plugin-integration-tests/kmp-junit4` - KMP with JUnit 4
- `plugin-integration-tests/kmp-kotlintest` - KMP with kotlin.test
- `plugin-integration-tests/kmp-kotlintest-junit5` - KMP with kotlin.test + JUnit 5 runtime

## See Also

- [Compatibility Matrix](../compatibility-matrix.md) - Full compatibility information
- [JVM + JUnit 5 Setup Guide](jvm-junit5.md) - JVM-specific details
- [Android + JUnit 4 Setup Guide](android-junit4.md) - Android-specific details
- [KMP + JUnit 4 Setup Guide](kmp-junit4.md) - JUnit 4 alternative
- [Ktor Client Guide](../clients/ktor.md) - Ktor-specific documentation
