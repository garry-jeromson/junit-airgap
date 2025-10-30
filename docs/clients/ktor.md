# Ktor Client Guide

Test Ktor HTTP clients with automatic network blocking.

## Compatibility

| Engine | Platform | Status | Exception Behavior |
|--------|----------|--------|-------------------|
| CIO | JVM | ✅ Fully Supported | Throws `NetworkRequestAttemptedException` directly |
| OkHttp | JVM, Android | ✅ Fully Supported | Wraps in `IOException` (check message) |
| Java | JVM | ✅ Fully Supported | Throws `NetworkRequestAttemptedException` directly |

**Tested Version**: Ktor 2.3.7

## Basic Usage

### CIO Engine (JVM)

Best for JVM-only projects. Throws exceptions directly:

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
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

    assertFailsWith<NetworkRequestAttemptedException> {
        client.get("https://api.example.com/users")
    }

    client.close()
}
```

### OkHttp Engine (Android)

Best for Android and KMP projects. Wraps exceptions:

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
        // OkHttp wraps NetworkRequestAttemptedException
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

Uses standard `java.net.http.HttpClient`. Throws exceptions directly:

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

## POST Requests with JSON

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

## Kotlin Multiplatform

For KMP projects, use platform-specific engines:

**commonTest:**
```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.ktor.client.request.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ApiClientTest {
    @Test
    @BlockNetworkRequests
    fun testSharedClientBlocked() = runTest {
        // Platform-specific client injected via expect/actual
        val client = HttpClientFactory.create()

        try {
            client.get("https://api.example.com/users")
        } catch (e: Exception) {
            // Handle both CIO (JVM) and OkHttp (Android) exceptions
            when {
                e is NetworkRequestAttemptedException -> { /* CIO/Java */ }
                e.message?.contains("Network request blocked") == true -> { /* OkHttp */ }
                else -> throw e
            }
        } finally {
            client.close()
        }
    }
}
```

**jvmMain:**
```kotlin
expect object HttpClientFactory {
    fun create(): HttpClient
}

// jvmMain
actual object HttpClientFactory {
    actual fun create() = HttpClient(CIO)
}

// androidMain
actual object HttpClientFactory {
    actual fun create() = HttpClient(OkHttp)
}
```

## Advanced Configuration

### Allow Localhost

```kotlin
import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts

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

    // ✅ api.staging.mycompany.com - allowed
    // ✅ auth.staging.mycompany.com - allowed
    // ❌ api.production.com - blocked

    client.close()
}
```

## Testing Production Code

### API Client Example

```kotlin
class UserApiClient(
    private val baseUrl: String = "https://api.example.com",
    private val client: HttpClient = HttpClient(CIO)
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

    fun close() = client.close()
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

### Test with Mocked Client

```kotlin
@Test
fun testWithMockedClient() = runTest {
    val mockClient = mock<HttpClient>()
    val apiClient = UserApiClient(client = mockClient)

    // No real network calls
    whenever(mockClient.get<User>(any())).thenReturn(User(id = 1, name = "John"))

    val user = apiClient.getUser(1)
    assertEquals(1, user.id)

    apiClient.close()
}
```

## Troubleshooting

### Different Exceptions on Different Platforms

**Expected behavior:** CIO/Java throw `NetworkRequestAttemptedException`, OkHttp wraps it.

**Solution:** Handle both cases:

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

### Coroutines Not Waiting for Exception

**Solution:** Use `runTest` from `kotlinx-coroutines-test`:

```kotlin
import kotlinx.coroutines.test.runTest

@Test
@BlockNetworkRequests
fun test() = runTest { // Use runTest, not runBlocking
    client.get("https://example.com")
}
```

## Engine Selection Guide

| Use Case | Recommended Engine | Reason |
|----------|-------------------|---------|
| JVM-only project | CIO | Lightweight, direct exceptions |
| Android project | OkHttp | Better Android integration |
| KMP (JVM + Android) | CIO (JVM), OkHttp (Android) | Platform-specific engines |
| Standard HTTP features | Java | Uses java.net.http.HttpClient |

## See Also

- [OkHttp Client Guide](okhttp.md) - OkHttp engine details
- [KMP Setup Guide](../setup-guides/kmp-junit5.md)
- [JVM Setup Guide](../setup-guides/jvm-junit5.md)
- [Android Setup Guide](../setup-guides/android-junit4.md)
