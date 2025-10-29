# Retrofit Client Guide

Test Retrofit clients with automatic network blocking.

## Compatibility

- **Retrofit Version**: 2.11.0 (tested)
- **Platforms**: JVM, Android
- **Exception Behavior**: Wraps `NetworkRequestAttemptedException` in `IOException` (uses OkHttp)

## Basic Usage

### JUnit 5

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import kotlin.test.assertTrue

interface ApiService {
    @GET("users")
    fun getUsers(): retrofit2.Call<String>
}

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
        // Retrofit wraps NetworkRequestAttemptedException
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

### JUnit 4

```kotlin
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import kotlin.test.assertTrue

interface ApiService {
    @GET("users")
    fun getUsers(): retrofit2.Call<String>
}

class RetrofitTest {
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
            assertTrue(e.message?.contains("Network request blocked") == true)
        }
    }
}
```

## Exception Handling

Retrofit uses OkHttp under the hood, which wraps `NetworkRequestAttemptedException`:

```kotlin
try {
    service.getUsers().execute()
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
interface ApiService {
    @GET("users/{id}")
    fun getUser(@Path("id") id: Int): Call<User>
}

@Test
@BlockNetworkRequests
fun testGetBlocked() {
    val service = retrofit.create(ApiService::class.java)

    try {
        service.getUser(1).execute()
        throw AssertionError("Should have thrown exception")
    } catch (e: Exception) {
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

### POST Request

```kotlin
interface ApiService {
    @POST("users")
    fun createUser(@Body user: User): Call<User>
}

@Test
@BlockNetworkRequests
fun testPostBlocked() {
    val service = retrofit.create(ApiService::class.java)
    val newUser = User(name = "John", email = "john@example.com")

    try {
        service.createUser(newUser).execute()
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
    val retrofit = Retrofit.Builder()
        .baseUrl("http://localhost:8080/")
        .build()

    val service = retrofit.create(ApiService::class.java)

    try {
        service.getUsers().execute()
    } catch (e: NetworkRequestAttemptedException) {
        throw AssertionError("Localhost should be allowed", e)
    } catch (e: Exception) {
        // Connection refused is OK
    }
}
```

### Allow Specific Hosts

```kotlin
@Test
@BlockNetworkRequests
@AllowRequestsToHosts(["*.staging.example.com"])
fun testStagingApiAllowed() {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.staging.example.com/")
        .build()

    // ✅ api.staging.example.com - allowed
    // ❌ api.production.example.com - blocked
}
```

## Testing Production Code

### Repository Example

```kotlin
class UserRepository(private val apiService: ApiService) {
    fun getUser(id: Int): User {
        val response = apiService.getUser(id).execute()
        return response.body() ?: throw IllegalStateException("User not found")
    }
}
```

### Test with Network Blocking

```kotlin
@Test
@BlockNetworkRequests
fun testRepositoryBlocked() {
    val service = retrofit.create(ApiService::class.java)
    val repository = UserRepository(service)

    try {
        repository.getUser(1)
        throw AssertionError("Should have thrown exception")
    } catch (e: Exception) {
        assertTrue(e.message?.contains("Network request blocked") == true)
    }
}
```

### Test with Mocked Service

```kotlin
@Test
fun testWithMockedService() {
    val mockService = mock<ApiService>()
    val mockCall = mock<Call<User>>()
    val mockResponse = Response.success(User(id = 1, name = "John"))

    whenever(mockService.getUser(1)).thenReturn(mockCall)
    whenever(mockCall.execute()).thenReturn(mockResponse)

    val repository = UserRepository(mockService)
    val user = repository.getUser(1)

    assertEquals(1, user.id)
}
```

## Android with Robolectric

```kotlin
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RetrofitAndroidTest {
    @Test
    @BlockNetworkRequests
    fun testRetrofitWithAndroidContext() {
        // Can use Android framework
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)

        // But network is blocked
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        try {
            service.getUsers().execute()
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Network request blocked") == true)
        }
    }
}
```

## See Also

- [OkHttp Client Guide](okhttp.md) - Retrofit uses OkHttp under the hood
- [Ktor Client Guide](ktor.md) - Alternative HTTP client
- [JVM + JUnit 5 Setup](../setup-guides/jvm-junit5.md)
- [Android Setup](../setup-guides/android-junit4.md)
