# OkHttp Client Guide

Test OkHttp clients with automatic network blocking.

## Compatibility

- **OkHttp Version**: 4.12.0 (tested)
- **Platforms**: JVM, Android
- **Exception Behavior**: Wraps `NetworkRequestAttemptedException` in `IOException`

## Basic Usage

### JUnit 5

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Test
@BlockNetworkRequests
fun testOkHttpBlocked() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.example.com/users")
        .build()

    try {
        client.newCall(request).execute()
        throw AssertionError("Should have thrown exception")
    } catch (e: Exception) {
        // OkHttp wraps NetworkRequestAttemptedException in IOException
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

### JUnit 4

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import kotlin.test.assertTrue

class OkHttpTest {
    @Test
    @BlockNetworkRequests
    fun testOkHttpBlocked() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.example.com/users")
            .build()

        try {
            client.newCall(request).execute()
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Network request blocked") == true)
        }
    }
}
```

## Exception Handling

OkHttp wraps the `NetworkRequestAttemptedException` in `IOException`. Check the message:

```kotlin
try {
    client.newCall(request).execute()
} catch (e: Exception) {
    assertTrue(
        e.message?.contains("Network request blocked") == true,
        "Expected network blocking but got: $e"
    )
}
```

## Request Types

### GET Request

```kotlin
@Test
@BlockNetworkRequests
fun testGetBlocked() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.example.com/users")
        .get()
        .build()

    try {
        client.newCall(request).execute()
        throw AssertionError("Should have thrown exception")
    } catch (e: Exception) {
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

### POST Request

```kotlin
@Test
@BlockNetworkRequests
fun testPostBlocked() {
    val client = OkHttpClient()
    val json = MediaType.parse("application/json; charset=utf-8")
    val body = RequestBody.create(json, """{"name":"John"}""")

    val request = Request.Builder()
        .url("https://api.example.com/users")
        .post(body)
        .build()

    try {
        client.newCall(request).execute()
        throw AssertionError("Should have thrown exception")
    } catch (e: Exception) {
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

## Advanced Configuration

### Allow Localhost

```kotlin
import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts

@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
fun testCanConnectToLocalhost() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:8080/api")
        .build()

    try {
        client.newCall(request).execute()
    } catch (e: NetworkRequestAttemptedException) {
        throw AssertionError("Localhost should be allowed", e)
    } catch (e: Exception) {
        // Connection refused is OK (no server running)
    }
}
```

### Allow Specific Hosts

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*.staging.example.com"])
fun testStagingApiAllowed() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.staging.example.com/users")
        .build()

    // ✅ staging.example.com - allowed
    // ❌ api.production.example.com - blocked
}
```

## Testing Production Code

### API Client Example

```kotlin
class UserApiClient(private val baseUrl: String = "https://api.example.com") {
    private val client = OkHttpClient()

    fun getUser(id: Int): User {
        val request = Request.Builder()
            .url("$baseUrl/users/$id")
            .build()

        client.newCall(request).execute().use { response ->
            return parseUser(response.body!!.string())
        }
    }
}
```

### Test with Network Blocking

```kotlin
@Test
@BlockNetworkBlocked
fun testApiClientBlocked() {
    val apiClient = UserApiClient()

    try {
        apiClient.getUser(1)
        throw AssertionError("Should have thrown exception")
    } catch (e: Exception) {
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

### Test with Dependency Injection

```kotlin
@Test
fun testWithMockedClient() {
    val mockClient = mock<OkHttpClient>()
    val apiClient = UserApiClient(client = mockClient)

    // No real network calls
    whenever(mockClient.newCall(any()).execute()).thenReturn(mockResponse)

    val user = apiClient.getUser(1)
    assertEquals(1, user.id)
}
```

## Android with Robolectric

```kotlin
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class OkHttpAndroidTest {
    @Test
    @BlockNetworkRequests
    fun testOkHttpWithAndroidContext() {
        // Can use Android framework
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)

        // But network is blocked
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.example.com")
            .build()

        try {
            client.newCall(request).execute()
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Network request blocked") == true)
        }
    }
}
```

## See Also

- [Retrofit Client Guide](retrofit.md) - Retrofit uses OkHttp under the hood
- [Ktor Client Guide](ktor.md) - Ktor OkHttp engine
- [JVM + JUnit 5 Setup](../setup-guides/jvm-junit5.md)
- [Android Setup](../setup-guides/android-junit4.md)
