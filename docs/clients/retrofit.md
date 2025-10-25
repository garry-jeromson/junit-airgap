# Retrofit Client Guide

Retrofit is a type-safe HTTP client for Android and Java. It uses OkHttp under the hood, so network blocking behavior is identical to OkHttp.

## Compatibility

| Platform | Status | Exception Behavior |
|----------|--------|-------------------|
| JVM | ✅ Fully Supported | Wraps `NetworkRequestAttemptedException` in `IOException` |
| Android | ✅ Fully Supported | Wraps `NetworkRequestAttemptedException` in `IOException` |

**Tested Version**: Retrofit 2.11.0 (with OkHttp 4.12.0)

## Basic Usage

### Define API Interface

```kotlin
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("users/{id}")
    fun getUser(@Path("id") id: Int): Call<User>

    @GET("users")
    fun getUsers(): Call<List<User>>

    @POST("users")
    fun createUser(@Body user: User): Call<User>

    @PUT("users/{id}")
    fun updateUser(@Path("id") id: Int, @Body user: User): Call<User>

    @DELETE("users/{id}")
    fun deleteUser(@Path("id") id: Int): Call<Void>
}

data class User(val id: Int, val name: String, val email: String)
```

### Test with Network Blocking (JUnit 5)

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.assertTrue

@ExtendWith(NoNetworkExtension::class)
class RetrofitTest {
    @Test
    @BlockNetworkRequests
    fun testRetrofitBlocked() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        try {
            service.getUser(1).execute()
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            // Retrofit wraps NetworkRequestAttemptedException
            assertTrue(
                e.message?.contains("Network request blocked") == true,
                "Expected network blocking but got: ${e.message}"
            )
        }
    }
}
```

### Test with Network Blocking (JUnit 4 + Robolectric)

```kotlin
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RetrofitAndroidTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun testRetrofitBlocked() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        try {
            service.getUser(1).execute()
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Network request blocked") == true)
        }
    }
}
```

## Exception Handling

**Important**: Since Retrofit uses OkHttp, it wraps `NetworkRequestAttemptedException` in `IOException`.

### Recommended Pattern

```kotlin
try {
    service.getUser(1).execute()
} catch (e: Exception) {
    assertTrue(
        e.message?.contains("Network request blocked") == true,
        "Expected network blocking but got: ${e.message}"
    )
}
```

## Advanced Examples

### POST Request

```kotlin
@Test
@BlockNetworkRequests
fun testPostBlocked() {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(ApiService::class.java)
    val newUser = User(id = 0, name = "John", email = "john@example.com")

    try {
        service.createUser(newUser).execute()
        throw AssertionError("Should have thrown exception")
    } catch (e: Exception) {
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

### Custom OkHttpClient

```kotlin
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Test
@BlockNetworkRequests
fun testWithCustomClient() {
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(ApiService::class.java)

    try {
        service.getUser(1).execute()
    } catch (e: Exception) {
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

### Kotlin Coroutines (Suspend Functions)

```kotlin
interface ApiService {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): User

    @GET("users")
    suspend fun getUsers(): List<User>
}

@Test
@BlockNetworkRequests
fun testSuspendFunctionBlocked() = runTest {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(ApiService::class.java)

    try {
        service.getUser(1)
        throw AssertionError("Should have thrown exception")
    } catch (e: Exception) {
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

## Testing Production Code

### Production Repository

```kotlin
class UserRepository(
    private val apiService: ApiService
) {
    fun getUser(id: Int): User? {
        return try {
            apiService.getUser(id).execute().body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserSuspend(id: Int): User {
        return apiService.getUser(id)
    }
}
```

### Test with Network Blocking

```kotlin
@Test
@BlockNetworkRequests
fun testRepositoryBlocked() {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService = retrofit.create(ApiService::class.java)
    val repository = UserRepository(apiService)

    try {
        repository.getUser(1)
    } catch (e: Exception) {
        // Repository should handle exception gracefully
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

### Test with Mock

```kotlin
@Test
fun testWithMock() {
    val mockService = mock<ApiService>()
    val repository = UserRepository(mockService)

    val mockCall = mock<Call<User>>()
    val mockResponse = Response.success(User(1, "John", "john@example.com"))

    whenever(mockService.getUser(1)).thenReturn(mockCall)
    whenever(mockCall.execute()).thenReturn(mockResponse)

    val user = repository.getUser(1)
    assertEquals("John", user?.name)
}
```

## Advanced Configuration

### Allow Localhost

```kotlin
import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts

@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
fun testCanConnectToLocalhost() {
    val retrofit = Retrofit.Builder()
        .baseUrl("http://localhost:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(ApiService::class.java)

    try {
        service.getUser(1).execute()
    } catch (e: NetworkRequestAttemptedException) {
        throw AssertionError("Localhost should be allowed", e)
    } catch (e: Exception) {
        // Connection refused is OK (no server running)
    }
}
```

### Integration with MockWebServer

```kotlin
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse

@Test
@AllowRequestsToHosts(["localhost", "127.0.0.1"])
fun testWithMockWebServer() {
    val server = MockWebServer()
    server.enqueue(MockResponse().setBody("""{"id":1,"name":"John","email":"john@example.com"}"""))
    server.start()

    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(ApiService::class.java)
    val response = service.getUser(1).execute()

    assertEquals(200, response.code())
    assertEquals("John", response.body()?.name)

    server.shutdown()
}
```

## Common Issues

### Issue: Exception message unclear

**Cause**: Retrofit wraps the exception through multiple layers.

**Solution**: Check the message string or root cause:

```kotlin
catch (e: Exception) {
    val rootCause = generateSequence(e as Throwable) { it.cause }.last()
    assertTrue(
        e.message?.contains("Network request blocked") == true ||
        rootCause is NetworkRequestAttemptedException
    )
}
```

### Issue: Suspend functions behave differently

**Solution**: Use `runTest` from kotlinx-coroutines-test:

```kotlin
import kotlinx.coroutines.test.runTest

@Test
@BlockNetworkRequests
fun test() = runTest {
    // Test suspend functions here
}
```

## See Also

- [OkHttp Client Guide](okhttp.md) - Retrofit uses OkHttp
- [Android Setup Guide](../setup-guides/android-junit4.md)
- [JVM Setup Guide](../setup-guides/jvm-junit5.md)
- [Compatibility Matrix](../compatibility-matrix.md)
