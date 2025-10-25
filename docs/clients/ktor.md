# Ktor Client Guide

Ktor is a modern, asynchronous HTTP client for Kotlin Multiplatform. This guide shows how to test Ktor clients with the JUnit No-Network Extension.

## Compatibility

| Engine | Platform | Status | Exception Behavior |
|--------|----------|--------|-------------------|
| CIO | JVM | ✅ Fully Supported | Throws `NetworkRequestAttemptedException` directly |
| OkHttp | JVM, Android | ✅ Fully Supported | Wraps in `IOException` (check message) |
| Java | JVM | ✅ Fully Supported | Throws `NetworkRequestAttemptedException` directly |
| Darwin | iOS | ⚠️ Not Blocked | API structure only |

**Tested Version**: Ktor 2.3.7

## Engine-Specific Behavior

### CIO Engine (JVM)

**Best for**: JVM-only projects, testing

**Exception handling**: Throws `NetworkRequestAttemptedException` directly

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

@Test
@BlockNetworkRequests
fun testKtorCioBlocked() = runTest {
    val client = HttpClient(CIO)

    // CIO throws NetworkRequestAttemptedException directly
    assertFailsWith<NetworkRequestAttemptedException> {
        client.get("https://api.example.com/users")
    }

    client.close()
}
```

### OkHttp Engine (JVM, Android)

**Best for**: Android, KMP projects targeting Android

**Exception handling**: Wraps in `IOException` (check message)

```kotlin
import io.ktor.client.engine.okhttp.*

@Test
@BlockNetworkRequests
fun testKtorOkHttpBlocked() = runTest {
    val client = HttpClient(OkHttp)

    try {
        client.get("https://api.example.com/users")
        throw AssertionError("Should have thrown exception")
    } catch (e: Exception) {
        // OkHttp engine wraps NetworkRequestAttemptedException
        assertTrue(
            e.message?.contains("Network request blocked") == true ||
            e.cause is NetworkRequestAttemptedException
        )
    } finally {
        client.close()
    }
}
```

### Java Engine (JVM)

**Best for**: JVM projects, standard HttpClient

**Exception handling**: Throws `NetworkRequestAttemptedException` directly

```kotlin
import io.ktor.client.engine.java.*

@Test
@BlockNetworkRequests
fun testKtorJavaBlocked() = runTest {
    val client = HttpClient(Java)

    assertFailsWith<NetworkRequestAttemptedException> {
        client.get("https://api.example.com/users")
    }

    client.close()
}
```

### Darwin Engine (iOS)

**Status**: Not blocked (iOS limitation)

```kotlin
import io.ktor.client.engine.darwin.*

@Test
fun testKtorDarwinNotBlocked() = runTest {
    val client = HttpClient(Darwin)

    // iOS: Network requests are NOT blocked
    // Use mocking or skip network blocking assertions
    // ...

    client.close()
}
```

## JUnit 5 Examples

### GET Request with CIO

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertFailsWith

@ExtendWith(NoNetworkExtension::class)
class KtorCioGetTest {
    @Test
    @BlockNetworkRequests
    fun testGetBlocked() = runTest {
        val client = HttpClient(CIO)

        assertFailsWith<NetworkRequestAttemptedException> {
            client.get("https://api.example.com/users")
        }

        client.close()
    }
}
```

### POST Request with JSON

```kotlin
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

@Test
@BlockNetworkRequests
fun testPostBlocked() = runTest {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    assertFailsWith<NetworkRequestAttemptedException> {
        client.post("https://api.example.com/users") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("name" to "John", "email" to "john@example.com"))
        }
    }

    client.close()
}
```

### Custom Configuration

```kotlin
import io.ktor.client.plugins.*

@Test
@BlockNetworkRequests
fun testCustomClientBlocked() = runTest {
    val client = HttpClient(CIO) {
        engine {
            requestTimeout = 30_000
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }

    assertFailsWith<NetworkRequestAttemptedException> {
        client.get("https://api.example.com/users")
    }

    client.close()
}
```

## Kotlin Multiplatform Usage

### Expect/Actual Pattern

Create platform-specific HTTP clients:

#### commonMain/HttpClientFactory.kt

```kotlin
expect object HttpClientFactory {
    fun create(): HttpClient
}
```

#### jvmMain/HttpClientFactory.kt

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*

actual object HttpClientFactory {
    actual fun create(): HttpClient = HttpClient(CIO)
}
```

#### androidMain/HttpClientFactory.kt

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

actual object HttpClientFactory {
    actual fun create(): HttpClient = HttpClient(OkHttp)
}
```

#### iosMain/HttpClientFactory.kt

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual object HttpClientFactory {
    actual fun create(): HttpClient = HttpClient(Darwin)
}
```

### Shared Test (commonTest)

```kotlin
// commonTest/ApiClientTest.kt
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.ktor.client.request.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ApiClientTest {
    @Test
    @BlockNetworkRequests
    fun testSharedClientBlocked() = runTest {
        val client = HttpClientFactory.create()

        try {
            client.get("https://api.example.com/users")
        } catch (e: Exception) {
            // Handle both CIO (JVM) and OkHttp (Android) exception types
            when {
                e is NetworkRequestAttemptedException -> {
                    // CIO/Java engine (JVM)
                }
                e.message?.contains("Network request blocked") == true -> {
                    // OkHttp engine (Android)
                }
                else -> throw e
            }
        } finally {
            client.close()
        }
    }
}
```

## Android with Robolectric

```kotlin
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class KtorAndroidTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun testKtorWithAndroidContext() = runTest {
        // Can use Android framework
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)

        // But network is blocked
        val client = HttpClient(OkHttp)
        try {
            client.get("https://api.example.com/users")
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Network request blocked") == true)
        } finally {
            client.close()
        }
    }
}
```

## Advanced Configuration

### Allow Localhost for Testing

```kotlin
import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts

@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
fun testCanConnectToLocalhost() = runTest {
    val client = HttpClient(CIO)

    try {
        client.get("http://localhost:8080/api/users")
    } catch (e: NetworkRequestAttemptedException) {
        throw AssertionError("Localhost should be allowed", e)
    } catch (e: Exception) {
        // Connection refused is OK (no server running)
    } finally {
        client.close()
    }
}
```

### Allow Specific API Endpoints

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*.staging.mycompany.com", "*.test.mycompany.com"])
fun testStagingApisAllowed() = runTest {
    val client = HttpClient(CIO)

    // ✅ https://api.staging.mycompany.com - allowed
    // ✅ https://auth.staging.mycompany.com - allowed
    // ❌ https://api.production.com - blocked

    client.close()
}
```

## Testing Production API Clients

### Production Code

```kotlin
class UserApiClient(
    private val baseUrl: String = "https://api.example.com",
    private val client: HttpClient = HttpClientFactory.create()
) {
    suspend fun getUser(id: Int): User {
        return client.get("$baseUrl/users/$id")
    }

    suspend fun createUser(user: User): User {
        return client.post("$baseUrl/users") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
    }

    fun close() {
        client.close()
    }
}
```

### Test with Network Blocking

```kotlin
@Test
@BlockNetworkRequests
fun testApiClientBlocked() = runTest {
    val apiClient = UserApiClient()

    try {
        assertFailsWith<NetworkRequestAttemptedException> {
            apiClient.getUser(1)
        }
    } finally {
        apiClient.close()
    }
}
```

### Test with Dependency Injection

```kotlin
@Test
fun testWithMockedClient() = runTest {
    val mockClient = mock<HttpClient>()
    val apiClient = UserApiClient(client = mockClient)

    // Test with mocked client - no real network calls
    whenever(mockClient.get<User>(any())).thenReturn(User(id = 1, name = "John"))

    val user = apiClient.getUser(1)
    assertEquals(1, user.id)

    apiClient.close()
}
```

## Integration with MockWebServer

```kotlin
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse

@Test
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
fun testWithMockWebServer() = runTest {
    val server = MockWebServer()
    server.enqueue(MockResponse().setBody("""{"id":1,"name":"John"}"""))
    server.start()

    val client = HttpClient(OkHttp) // Use OkHttp engine for MockWebServer compatibility
    val response = client.get<String>(server.url("/users").toString())

    assertTrue(response.contains("John"))

    client.close()
    server.shutdown()
}
```

## Common Issues

### Issue: Different exceptions on different platforms

**Expected**: CIO throws `NetworkRequestAttemptedException`, OkHttp wraps it.

**Solution**: Handle both cases:

```kotlin
try {
    client.get("https://example.com")
} catch (e: NetworkRequestAttemptedException) {
    // CIO/Java engines
} catch (e: Exception) {
    // OkHttp engine - check message
    assertTrue(e.message?.contains("Network request blocked") == true)
}
```

### Issue: iOS tests failing

**Expected**: Darwin engine (iOS) doesn't block network requests.

**Solution**: Skip network blocking assertions for iOS or use conditional compilation:

```kotlin
@Test
fun testNetworkBlocking() = runTest {
    if (Platform.isIOS) {
        // Skip or use different assertion
        return@runTest
    }

    // Network blocking test for JVM/Android
    assertFailsWith<NetworkRequestAttemptedException> {
        client.get("https://example.com")
    }
}
```

### Issue: Coroutines not waiting for exception

**Solution**: Use `runTest` from `kotlinx-coroutines-test`:

```kotlin
import kotlinx.coroutines.test.runTest

@Test
@BlockNetworkRequests
fun test() = runTest { // Use runTest, not runBlocking
    // ...
}
```

## Engine Selection Guide

| Use Case | Recommended Engine | Reason |
|----------|-------------------|---------|
| JVM-only project | CIO | Lightweight, direct exceptions |
| Android project | OkHttp | Better Android integration |
| KMP (JVM + Android) | CIO (JVM), OkHttp (Android) | Platform-specific engines |
| Standard HTTP features | Java | Uses java.net.http.HttpClient |
| iOS | Darwin | Required for iOS (not blocked) |

## See Also

- [OkHttp Client Guide](okhttp.md) - OkHttp engine uses this under the hood
- [KMP Setup Guide](../setup-guides/kmp-junit5.md) - Multiplatform configuration
- [JVM Setup Guide](../setup-guides/jvm-junit5.md) - JVM-specific details
- [Android Setup Guide](../setup-guides/android-junit4.md) - Android-specific details
