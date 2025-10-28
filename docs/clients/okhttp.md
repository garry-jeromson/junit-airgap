# OkHttp Client Guide

OkHttp is one of the most popular HTTP clients for JVM and Android. This guide shows how to test OkHttp with the JUnit Airgap Extension.

## Compatibility

| Platform | Status | Exception Behavior |
|----------|--------|-------------------|
| JVM | ✅ Fully Supported | Wraps `NetworkRequestAttemptedException` in `IOException` |
| Android | ✅ Fully Supported | Wraps `NetworkRequestAttemptedException` in `IOException` |

**Tested Version**: OkHttp 4.12.0

## Exception Handling

**Important**: OkHttp wraps `NetworkRequestAttemptedException` in `IOException`. You need to check the exception message or cause.

### Recommended Pattern

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
        throw AssertionError("Expected network to be blocked")
    } catch (e: Exception) {
        // OkHttp wraps NetworkRequestAttemptedException in IOException
        assertTrue(
            e.message?.contains("Network request blocked") == true,
            "Expected network blocking but got: ${e.message}"
        )
    }
}
```

### Alternative: Check Cause

```kotlin
try {
    client.newCall(request).execute()
} catch (e: IOException) {
    // Check if the cause is NetworkRequestAttemptedException
    assertTrue(
        e.cause is NetworkRequestAttemptedException ||
        e.message?.contains("Network request blocked") == true
    )
}
```

## JUnit 5 Examples

### Basic GET Request

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.AirgapExtension
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertTrue

@ExtendWith(AirgapExtension::class)
class OkHttpGetTest {
    @Test
    @BlockNetworkRequests
    fun testGetBlocked() {
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

### POST Request

```kotlin
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Test
@BlockNetworkRequests
fun testPostBlocked() {
    val client = OkHttpClient()

    val json = """{"name":"John","email":"john@example.com"}"""
    val body = json.toRequestBody("application/json".toMediaType())

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

### Custom OkHttpClient Configuration

```kotlin
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Test
@BlockNetworkRequests
fun testCustomClientBlocked() {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url("https://api.example.com/users")
        .build()

    try {
        client.newCall(request).execute()
    } catch (e: Exception) {
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

## JUnit 4 Examples

### Basic Usage with Robolectric (Android)

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

## Advanced Configuration

### Allow Localhost for Testing

```kotlin
import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests

@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
fun testCanConnectToLocalhost() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:8080/api/users")
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

### Allow Specific API Endpoints

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*.staging.mycompany.com", "*.test.mycompany.com"])
fun testStagingApisAllowed() {
    // ✅ api.staging.mycompany.com - allowed
    // ✅ auth.staging.mycompany.com - allowed
    // ❌ api.production.com - blocked
}
```

## Integration with Interceptors

OkHttp interceptors still work with network blocking:

```kotlin
import okhttp3.Interceptor
import okhttp3.Response

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        println("Request: ${request.url}")
        return chain.proceed(request) // Will throw NetworkRequestAttemptedException
    }
}

@Test
@BlockNetworkRequests
fun testInterceptorWithBlocking() {
    val client = OkHttpClient.Builder()
        .addInterceptor(LoggingInterceptor())
        .build()

    val request = Request.Builder()
        .url("https://api.example.com/users")
        .build()

    try {
        client.newCall(request).execute()
    } catch (e: Exception) {
        // Interceptor runs, but network call is blocked
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

## Mocking Strategies

When you want to test OkHttp without making real network calls, combine with mocking:

### MockWebServer

```kotlin
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse

@Test
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
fun testWithMockWebServer() {
    val server = MockWebServer()
    server.enqueue(MockResponse().setBody("""{"name":"John"}"""))
    server.start()

    val client = OkHttpClient()
    val request = Request.Builder()
        .url(server.url("/users"))
        .build()

    val response = client.newCall(request).execute()
    assertEquals(200, response.code)

    server.shutdown()
}
```

### Retrofit + OkHttp

See [Retrofit Client Guide](retrofit.md) for detailed Retrofit examples.

## Testing Real API Clients

### Production Code

```kotlin
class UserApiClient(private val baseUrl: String = "https://api.example.com") {
    private val client = OkHttpClient()

    fun getUser(id: Int): String {
        val request = Request.Builder()
            .url("$baseUrl/users/$id")
            .build()

        return client.newCall(request).execute().use { response ->
            response.body?.string() ?: throw IOException("Empty response")
        }
    }
}
```

### Test with Network Blocking

```kotlin
@Test
@BlockNetworkRequests
fun testApiClientBlocked() {
    val apiClient = UserApiClient()

    try {
        apiClient.getUser(1)
        throw AssertionError("Should have blocked network request")
    } catch (e: Exception) {
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

### Test with Dependency Injection

```kotlin
class UserApiClient(
    private val client: OkHttpClient, // Inject client for testing
    private val baseUrl: String = "https://api.example.com"
) {
    fun getUser(id: Int): String {
        val request = Request.Builder()
            .url("$baseUrl/users/$id")
            .build()

        return client.newCall(request).execute().use { response ->
            response.body?.string() ?: throw IOException("Empty response")
        }
    }
}

@Test
fun testWithMockedClient() {
    val mockClient = mock<OkHttpClient>()
    val apiClient = UserApiClient(mockClient, "https://api.example.com")

    // Test with mocked client - no real network calls
    whenever(mockClient.newCall(any())).thenReturn(mockCall)
    // ...
}
```

## Common Issues

### Issue: Exception message doesn't contain "Network request blocked"

**Possible causes**:
1. Network blocking not active (missing `@BlockNetworkRequests`)
2. OkHttp wrapping the exception differently
3. Interceptor modifying the exception

**Solution**: Check the exception cause:

```kotlin
catch (e: Exception) {
    val rootCause = generateSequence(e as Throwable) { it.cause }.last()
    assertTrue(rootCause is NetworkRequestAttemptedException)
}
```

### Issue: Localhost connections blocked unexpectedly

**Solution**: Explicitly allow localhost:

```kotlin
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
```

For Android emulator, also allow `10.0.2.2`.

## See Also

- [Retrofit Client Guide](retrofit.md) - Retrofit uses OkHttp under the hood
- [Ktor Client Guide](ktor.md) - Alternative HTTP client
- [Android Setup Guide](../setup-guides/android-junit4.md)
- [JVM Setup Guide](../setup-guides/jvm-junit5.md)
