# Setup Guide: Android + JUnit 4 + Robolectric

This guide shows how to set up the JUnit Airgap Extension for an Android project using JUnit 4 with Robolectric.

## Requirements

- Java 21+ (uses JVMTI agent for network blocking)
- Android Gradle Plugin 7.x or later (tested with 8.7.3)
- Android SDK: Min API 26 (Android 8.0), Compile SDK 34+
- JUnit 4.12+ (tested with 4.13.2)
- Robolectric 4.x (tested with 4.14)
- Kotlin 1.9+ (tested with 2.1.0)

## Why Robolectric?

Robolectric allows Android unit tests to run on the JVM without requiring an Android device or emulator. This makes tests:
- **Fast**: Run in seconds, not minutes
- **Reliable**: No device/emulator flakiness
- **CI-friendly**: No emulator setup required
- **Debuggable**: Standard JVM debugging works

The JUnit Airgap Extension works with Robolectric tests to block network requests while still providing Android framework APIs.

## Installation

### Using the Gradle Plugin (Recommended)

Add the plugin to your Android module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.library") // or com.android.application
    kotlin("android")
    id("io.github.garryjeromson.junit-airgap") version "0.1.0-SNAPSHOT"
}

// Configure the plugin
junitAirgap {
    enabled = true
    applyToAllTests = false // Use @BlockNetworkRequests explicitly
    injectJUnit4Rule = true // Enable automatic @Rule injection
}

android {
    namespace = "com.example.myapp"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    // Production dependencies
    implementation("androidx.core:core-ktx:1.12.0")

    // Test dependencies
    testImplementation("io.github.garryjeromson:junit-airgap:0.1.0-SNAPSHOT")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14")
    testImplementation("androidx.test:core:1.5.0")
}
```

### Manual Configuration (Without Plugin)

```kotlin
dependencies {
    testImplementation("io.github.garryjeromson:junit-airgap:0.1.0-SNAPSHOT")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14")
    testImplementation("androidx.test:core:1.5.0")
}
```

Then manually add `@Rule` to each test class (see Basic Usage section).

## Basic Usage

### Simple Robolectric Test with Network Blocking

```kotlin
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.airgap.AirgapRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.Socket
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class MyAndroidTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

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

### Using Android APIs with Network Blocking

```kotlin
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.AirgapRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun testSharedPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)

        // Write data
        prefs.edit().putString("key", "value").apply()

        // Read data back
        val value = prefs.getString("key", null)
        assertEquals("value", value)

        // Network is still blocked
        // Socket("example.com", 80) // Would throw NetworkRequestAttemptedException
    }
}
```

## Testing Android HTTP Clients

### OkHttp (Recommended for Android)

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.AirgapRule
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class OkHttpAndroidTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun testOkHttpBlocked() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.example.com/data")
            .build()

        try {
            client.newCall(request).execute()
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            // OkHttp wraps NetworkRequestAttemptedException in IOException
            assertTrue(e.message?.contains("Network request blocked") == true)
        }
    }
}
```

### Retrofit with OkHttp

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.AirgapRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import kotlin.test.assertTrue

interface ApiService {
    @GET("users")
    fun getUsers(): retrofit2.Call<String>
}

@RunWith(RobolectricTestRunner::class)
class RetrofitAndroidTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun testRetrofitBlocked() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        try {
            service.getUsers().execute()
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            // Retrofit wraps the exception
            assertTrue(e.message?.contains("Network request blocked") == true)
        }
    }
}
```

### Ktor Client with OkHttp Engine

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.AirgapRule
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class KtorOkHttpAndroidTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun testKtorOkHttpBlocked() = runBlocking {
        val client = HttpClient(OkHttp)

        try {
            client.get("https://api.example.com")
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            // OkHttp engine wraps NetworkRequestAttemptedException
            assertTrue(
                e.message?.contains("Network request blocked") == true ||
                e.cause?.javaClass?.simpleName == "NetworkRequestAttemptedException"
            )
        } finally {
            client.close()
        }
    }
}
```

## Advanced Configuration

### Block All Tests by Default

```kotlin
@get:Rule
val noNetworkRule = AirgapRule(applyToAllTests = true)
```

Then opt-out specific tests:

```kotlin
@Test
@AllowNetworkRequests
fun testCanMakeNetworkRequests() {
    // Network allowed for this test
}
```

### Testing with Local Server (Localhost)

```kotlin
import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.AirgapRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.Socket

@RunWith(RobolectricTestRunner::class)
class LocalServerTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

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

### Wildcard Patterns for API Endpoints

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*.mycompany.com", "localhost"])
fun testInternalApisAllowed() {
    // ✅ api.mycompany.com - allowed
    // ✅ staging.mycompany.com - allowed
    // ❌ external-api.com - blocked
}
```

## Running Tests

### Gradle Command Line

```bash
# Run all unit tests (including Robolectric tests)
./gradlew test

# Run tests for specific variant
./gradlew testDebugUnitTest

# Run specific test class
./gradlew test --tests MyAndroidTest

# Run with debug output
./gradlew test --info -Djunit.airgap.debug=true
```

### Android Studio

1. Right-click on test class → "Run 'MyAndroidTest'"
2. Add JVM args if needed:
   - Run → Edit Configurations
   - Add VM options: `-Djava.security.manager=allow` (Java 21+)

## Android-Specific Considerations

### Android Emulator Localhost (10.0.2.2)

The Android emulator uses `10.0.2.2` to access the host machine's localhost. Allow it explicitly:

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

### Testing Repository Pattern

```kotlin
// Production code
class UserRepository(private val apiService: ApiService) {
    fun getUsers(): List<User> {
        return apiService.getUsers().execute().body() ?: emptyList()
    }
}

// Test
@RunWith(RobolectricTestRunner::class)
class UserRepositoryTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun testRepositoryDoesNotMakeNetworkCall() {
        // This test will fail if repository tries real network call
        // Force using mocks or fakes instead
        val mockService = mock<ApiService>()
        val repository = UserRepository(mockService)

        // Test with mock - no real network calls
        whenever(mockService.getUsers()).thenReturn(mockResponse)
        val users = repository.getUsers()
        // assertions...
    }
}
```

## Troubleshooting

### Issue: "UnsupportedOperationException: The Security Manager is deprecated"

**Solution**: For Java 21+, configure test tasks:

```kotlin
tasks.withType<Test> {
}
```

### Issue: "java.lang.NoClassDefFoundError: android/..."

**Cause**: Missing Robolectric runner annotation.

**Solution**: Add `@RunWith(RobolectricTestRunner::class)`:

```kotlin
@RunWith(RobolectricTestRunner::class)
class MyTest {
    // ...
}
```

### Issue: "Resources$NotFoundException"

**Cause**: Android resources not loaded by Robolectric.

**Solution**: Enable Android resources in `build.gradle.kts`:

```kotlin
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
```

### Issue: Auto-injection not working

**Checklist**:
1. Is `injectJUnit4Rule = true` in plugin configuration?
2. Are test classes compiled before injection?
3. Try manual `@Rule` configuration as fallback
4. Check with debug mode: `debug = true`

### Issue: OkHttp exception message unclear

OkHttp wraps `NetworkRequestAttemptedException`. Check the message or cause:

```kotlin
catch (e: Exception) {
    assertTrue(
        e.message?.contains("Network request blocked") == true ||
        e.cause is NetworkRequestAttemptedException,
        "Expected network blocking but got: $e"
    )
}
```

## JUnit 5 on Android

**Status**: Not officially supported, requires additional configuration.

JUnit 5 on Android requires:
- `junit-vintage-engine` to run both JUnit 4 and JUnit 5
- Additional Gradle configuration
- Limited IDE support

**Recommendation**: Stick with JUnit 4 for Android projects with Robolectric.

## Instrumentation Tests

**Not Supported**: This library only works with Robolectric unit tests, not Android instrumentation tests.

**Why?** Instrumentation tests run on a real device/emulator where:
- JVMTI agent cannot be loaded on Android devices
- Different security restrictions apply
- Network mocking requires different approaches

**For instrumentation tests**, consider:
- WireMock for API mocking
- OkHttp Interceptors for request/response mocking
- MockWebServer for fake server

## Complete Example Project

See the `plugin-integration-tests/android-robolectric` module for a complete working example with:
- Gradle plugin configuration
- Robolectric setup
- Network blocking tests with Android APIs
- HTTP client tests (OkHttp, Retrofit, Ktor)
- Advanced configuration examples

## See Also

- [Compatibility Matrix](../compatibility-matrix.md) - Full compatibility information
- [HTTP Client Guides](../clients/) - Detailed guides for each HTTP client
- [KMP + JUnit 4 Setup Guide](kmp-junit4.md) - Multiplatform projects
- [Advanced Configuration](../advanced-configuration.md) - All configuration options
