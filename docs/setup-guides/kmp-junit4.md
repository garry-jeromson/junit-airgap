# Setup Guide: Kotlin Multiplatform + JUnit 4

This guide shows how to set up the JUnit No-Network Extension for a Kotlin Multiplatform (KMP) project using JUnit 4.

## Requirements

- Java 21+ (uses JVMTI agent for network blocking)
- Gradle 7.x or later (tested with 8.11.1)
- Kotlin 1.9+ (tested with 2.1.0)
- JUnit 4.12+ (tested with 4.13.2)

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
    id("io.github.garryjeromson.junit-no-network") version "0.1.0-SNAPSHOT"
}

// Configure the plugin
junitNoNetwork {
    enabled = true
    applyToAllTests = false
    injectJUnit4Rule = true // Enable automatic @Rule injection
}

kotlin {
    jvmToolchain(21)

    // Configure targets
    jvm()
    androidTarget() // If targeting Android
    iosSimulatorArm64() // If targeting iOS

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.github.garryjeromson:junit-no-network:0.1.0-SNAPSHOT")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.robolectric:robolectric:4.14")
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

// Configure test tasks
tasks.withType<Test> {
}
```

## Basic Usage

### With Plugin Auto-Injection

With `injectJUnit4Rule = true`, you don't need manual `@Rule` declarations:

```kotlin
// commonTest/ApiClientTest.kt
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.Test
import java.net.Socket
import kotlin.test.assertFailsWith

class ApiClientTest {
    // No @Rule needed - plugin injects it automatically!

    @Test
    @BlockNetworkRequests
    fun testNetworkBlocked() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80).use { }
        }
    }
}
```

### With Manual @Rule

If auto-injection doesn't work or you prefer explicit configuration:

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import org.junit.Rule
import org.junit.Test
import java.net.Socket
import kotlin.test.assertFailsWith

class ApiClientTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun testNetworkBlocked() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80).use { }
        }
    }
}
```

## Testing Shared Ktor Client

### Common Test (Works on JVM and Android)

```kotlin
// commonTest/KtorClientTest.kt
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class KtorClientTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun testSharedClientBlocked() = runBlocking {
        val client = HttpClientFactory.create()

        try {
            // Behavior depends on platform engine
            client.get("https://example.com")
        } catch (e: Exception) {
            // Expected - network is blocked
            assertTrue(
                e is NetworkRequestAttemptedException ||
                e.message?.contains("Network request blocked") == true
            )
        } finally {
            client.close()
        }
    }
}
```

### Android-Specific Test with Robolectric

```kotlin
// androidUnitTest/RobolectricKtorTest.kt
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class RobolectricKtorTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun testKtorWithAndroidContext() = runBlocking {
        // Can use Android framework
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)

        // But network is blocked
        val client = HttpClient(OkHttp)
        try {
            client.get("https://example.com")
        } catch (e: Exception) {
            // OkHttp wraps the exception
            assertTrue(e.message?.contains("Network request blocked") == true)
        } finally {
            client.close()
        }
    }
}
```

## Platform-Specific Behavior

### Exception Handling

| Platform | Engine | Exception Behavior |
|----------|--------|-------------------|
| JVM | CIO | Throws `NetworkRequestAttemptedException` directly |
| Android | OkHttp | Wraps in `IOException` (check message) |
| iOS | Darwin | Not blocked (API only) |

**Handle both cases**:

```kotlin
try {
    client.get("https://example.com")
} catch (e: NetworkRequestAttemptedException) {
    // JVM with CIO engine
} catch (e: Exception) {
    // Android with OkHttp - check message
    assertTrue(e.message?.contains("Network request blocked") == true)
}
```

## Running Tests

```bash
# Run all tests (all platforms)
./gradlew allTests

# JVM tests only
./gradlew jvmTest

# Android tests only
./gradlew testDebugUnitTest
```

## Source Set Structure

```
src/
├── commonMain/kotlin/          # Shared production code
│   └── HttpClientFactory.kt (expect)
├── commonTest/kotlin/          # Shared test code (JUnit 4)
│   └── ApiClientTest.kt
├── jvmMain/kotlin/             # JVM-specific production code
│   └── HttpClientFactory.kt (actual - CIO)
├── jvmTest/kotlin/             # JVM-specific tests (JUnit 4)
│   └── JvmHttpClientTest.kt
├── androidMain/kotlin/         # Android-specific production code
│   └── HttpClientFactory.kt (actual - OkHttp)
└── androidUnitTest/kotlin/     # Android-specific tests (JUnit 4 + Robolectric)
    └── RobolectricKtorTest.kt
```

## Troubleshooting

### Issue: Auto-injection not working

**Solution**:
1. Ensure `injectJUnit4Rule = true` in plugin configuration
2. Try manual `@Rule` configuration as fallback
3. Enable debug: `debug = true` in plugin configuration

### Issue: Android tests not using Robolectric

**Solution**: Add `@RunWith(RobolectricTestRunner::class)`:

```kotlin
@RunWith(RobolectricTestRunner::class)
class MyAndroidTest {
    // ...
}
```

### Issue: Different exceptions on different platforms

**Expected**: OkHttp (Android) wraps exceptions differently than CIO (JVM).

**Solution**: Handle both cases or use platform-specific tests.

## Complete Example Projects

- `plugin-integration-tests/kmp-junit4` - KMP with JUnit 4
- `plugin-integration-tests/kmp-kotlintest` - KMP with kotlin.test + JUnit 4 runtime

## Migrating to JUnit 5

See [KMP + JUnit 5 Setup Guide](kmp-junit5.md) for migrating to JUnit 5.

**Benefits of JUnit 5**:
- Better parameterized tests
- Nested test classes
- Better IDE support
- Modern testing features

**Considerations**:
- Android JUnit 5 requires additional configuration
- JUnit 4 works well for most Android projects

## See Also

- [Compatibility Matrix](../compatibility-matrix.md) - Full compatibility information
- [JVM + JUnit 4 Setup Guide](jvm-junit4.md) - JVM-specific details
- [Android + JUnit 4 Setup Guide](android-junit4.md) - Android-specific details
- [KMP + JUnit 5 Setup Guide](kmp-junit5.md) - JUnit 5 alternative
- [Ktor Client Guide](../clients/ktor.md) - Ktor-specific documentation
