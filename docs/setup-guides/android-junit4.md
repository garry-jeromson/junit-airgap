# Setup Guide: Android + JUnit 4 + Robolectric

Block network requests in Android unit tests using Robolectric.

## Requirements

- Java 21+ (uses JVMTI agent)
- Android Gradle Plugin 7.x+ (tested with 8.7.3)
- Android SDK: Min API 26, Compile SDK 34+
- JUnit 4.12+ (tested with 4.13.2)
- Robolectric 4.x (tested with 4.14)
- Kotlin 1.9+ (tested with 2.1.0)

## Installation

### Gradle Plugin (Recommended)

```kotlin
plugins {
    id("com.android.library") // or com.android.application
    kotlin("android")
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-beta.1"
}

android {
    namespace = "com.example.myapp"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true // Required for Robolectric
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14")
    testImplementation("androidx.test:core:1.5.0")
}
```

### Manual Configuration

```kotlin
dependencies {
    testImplementation("io.github.garryjeromson:junit-airgap:0.1.0-beta.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14")
}
```

Then manually add `@Rule` to each test class.

## Basic Usage

### Simple Test with Network Blocking

```kotlin
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.Socket
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class MyAndroidTest {
    @Test
    @BlockNetworkRequests
    fun testNetworkBlockedWithAndroidContext() {
        // Can use Android framework APIs
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)

        // But network is blocked
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80).use { }
        }
    }
}
```

### Using Android APIs

```kotlin
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesTest {
    @Test
    @BlockNetworkRequests
    fun testSharedPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)

        prefs.edit().putString("key", "value").apply()

        val value = prefs.getString("key", null)
        assertEquals("value", value)

        // Network is blocked: Socket("example.com", 80) would throw
    }
}
```

## HTTP Client Testing

For testing with OkHttp, Retrofit, or Ktor, see the **[HTTP Client Guides](../clients/)**:
- **[OkHttp](../clients/okhttp.md)** - Android's most popular HTTP client
- **[Retrofit](../clients/retrofit.md)** - Type-safe API client
- **[Ktor](../clients/ktor.md)** - Multiplatform HTTP client

## Advanced Configuration

### Block All Tests by Default

```kotlin
junitAirgap {
    applyToAllTests = true
}
```

Then opt-out specific tests:

```kotlin
@Test
@AllowNetworkRequests
fun testCanMakeNetworkRequests() {
    // Network allowed for this test
}
```

### Allow Localhost

```kotlin
import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.Socket

@RunWith(RobolectricTestRunner::class)
class LocalServerTest {
    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(["localhost", "127.0.0.1", "10.0.2.2"]) // 10.0.2.2 = Android emulator host
    fun testCanConnectToLocalhost() {
        try {
            Socket("localhost", 8080).close()
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Localhost should be allowed", e)
        } catch (e: Exception) {
            // Connection refused is OK (no server running)
        }
    }
}
```

### Wildcard Patterns

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*.mycompany.com", "localhost"])
fun testInternalApisAllowed() {
    // ✅ api.mycompany.com, staging.mycompany.com - allowed
    // ❌ external-api.com - blocked
}
```

## Running Tests

```bash
# Run all unit tests
./gradlew test

# Run specific variant
./gradlew testDebugUnitTest

# Run specific test class
./gradlew test --tests MyAndroidTest

# Run with debug output
./gradlew test --info -Djunit.airgap.debug=true
```

### Android Studio

1. Right-click on test class → "Run 'MyAndroidTest'"
2. For debug output, add VM options in Run Configuration:
   - Run → Edit Configurations → Add VM options: `-Djunit.airgap.debug=true`

## Android-Specific Notes

### Android Emulator Localhost

The Android emulator uses `10.0.2.2` to access the host machine's localhost:

```kotlin
@AllowRequestsToHosts(["localhost", "127.0.0.1", "10.0.2.2"])
```

### HttpURLConnection (Android Framework)

```kotlin
@Test
@BlockNetworkRequests
fun testHttpURLConnectionBlocked() {
    val url = URL("https://example.com")
    assertFailsWith<NetworkRequestAttemptedException> {
        url.openConnection().connect()
    }
}
```

## Troubleshooting

### Resources$NotFoundException

**Cause**: Android resources not loaded by Robolectric.

**Solution**: Enable Android resources:

```kotlin
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
```

### NoClassDefFoundError: android/...

**Cause**: Missing Robolectric runner annotation.

**Solution**: Add `@RunWith(RobolectricTestRunner::class)` to test class.

### Auto-injection Not Working

1. Is JUnit 4 auto-detected? Check build output for "Auto-detected JUnit 4" message
2. Is `junit:junit` dependency present?
3. Try manual override: `injectJUnit4Rule = true` in plugin config
4. Enable debug: `junitAirgap { debug = true }`

### HTTP Client Exceptions

HTTP clients wrap `NetworkRequestAttemptedException` in their own exception types. Check the exception message or cause:

```kotlin
catch (e: Exception) {
    assertTrue(
        e.message?.contains("Network request blocked") == true ||
        e.cause is NetworkRequestAttemptedException,
        "Expected network blocking but got: $e"
    )
}
```

## Limitations

### JUnit 5 on Android

Not officially supported. Requires `junit-vintage-engine` and additional configuration. **Recommendation**: Stick with JUnit 4 for Android + Robolectric.

### Instrumentation Tests

This library only works with Robolectric unit tests, not Android instrumentation tests (device/emulator tests). JVMTI agents cannot run on Android devices.

For instrumentation tests, consider:
- WireMock for API mocking
- OkHttp Interceptors
- MockWebServer

## Example Project

See `plugin-integration-tests/android-robolectric` for a complete working example with:
- Gradle plugin configuration
- Robolectric setup
- Network blocking tests with Android APIs
- HTTP client integration tests

## See Also

- [Compatibility Matrix](../compatibility-matrix.md)
- [HTTP Client Guides](../clients/)
- [KMP + JUnit 4 Setup](kmp-junit4.md)
- [Advanced Configuration](../advanced-configuration.md)
