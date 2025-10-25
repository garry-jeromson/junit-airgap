# Migration Guide: Java 24+ and Beyond

## ⚠️ Critical Information

**This library will stop working when you upgrade to Java 24 or later.**

Java 24 permanently removes `SecurityManager` ([JEP 486](https://openjdk.org/jeps/486)), which this library relies on for network blocking. Oracle will NOT provide a replacement for SecurityManager's interception capabilities.

## Timeline

| Java Version | Status | Action Required |
|--------------|--------|-----------------|
| Java 17-23 | ✅ Supported | Use this library |
| Java 24+ | ❌ Not Supported | **Migrate to alternatives** |

**When to start planning**: Now. Even if you're on Java 17-21, start planning your migration strategy.

## Why SecurityManager Cannot Be Replaced

### Technical Limitations

SecurityManager worked because:
1. It's called by the JVM itself from **native code**
2. It intercepts socket connections at the lowest possible level
3. It works for **all** HTTP clients (OkHttp, Ktor, Apache HttpClient, etc.)

No Java-based replacement can achieve this because:
- Bytecode instrumentation (ByteBuddy, etc.) only works on **Java bytecode**, not native calls
- Socket constructors call native code directly
- HTTP clients use these native socket constructors
- No Java API can intercept native calls

This is a **fundamental limitation**, not a bug that can be fixed.

## Migration Strategies

### Strategy 1: Mocking/Stubbing (Recommended for Unit Tests)

**Use case**: Pure unit tests that should never touch the network

**Approach**: Mock network layers using frameworks like MockK, Mockito, or WireMock.

#### Before (with JUnit No-Network Extension)

```kotlin
@Test
@BlockNetworkRequests
fun testUserService() {
    val service = UserService()
    val user = service.getUser(1)
    // Test logic...
}
```

#### After (with Mocking)

```kotlin
@Test
fun testUserService() {
    val mockHttpClient = mock<HttpClient>()
    val service = UserService(mockHttpClient) // Inject dependency

    // Mock the network call
    whenever(mockHttpClient.get<User>(any())).thenReturn(User(1, "John"))

    val user = service.getUser(1)
    assertEquals("John", user.name)
}
```

**Benefits**:
- Complete control over responses
- Fast tests (no actual network calls)
- Works on Java 24+

**Drawbacks**:
- Requires refactoring to inject dependencies
- More setup code per test
- Need to maintain mocks

**Recommended tools**:
- [MockK](https://mockk.io/) - Kotlin mocking library
- [Mockito](https://site.mockito.org/) - Java mocking framework
- [WireMock](http://wiremock.org/) - HTTP mocking server

### Strategy 2: Dependency Injection

**Use case**: Make code testable by injecting HTTP clients

**Approach**: Design code to accept HTTP clients as constructor parameters.

#### Before

```kotlin
class UserService {
    private val client = HttpClient(CIO)

    suspend fun getUser(id: Int): User {
        return client.get("https://api.example.com/users/$id")
    }
}
```

#### After

```kotlin
class UserService(
    private val client: HttpClient = HttpClient(CIO), // Default for production
    private val baseUrl: String = "https://api.example.com"
) {
    suspend fun getUser(id: Int): User {
        return client.get("$baseUrl/users/$id")
    }
}

// Test
@Test
fun testUserService() {
    val mockClient = mock<HttpClient>()
    val service = UserService(client = mockClient, baseUrl = "http://localhost")

    // Test with mocked client
}
```

**Benefits**:
- Clean architecture
- Easy to test
- Production code unchanged
- Works on Java 24+

**Drawbacks**:
- Requires initial refactoring
- Need dependency injection framework (or manual DI)

### Strategy 3: Repository Pattern

**Use case**: Abstract network calls behind interfaces

**Approach**: Create repository interfaces with fake implementations for testing.

#### Production Code

```kotlin
interface UserRepository {
    suspend fun getUser(id: Int): User
    suspend fun getUsers(): List<User>
}

// Production implementation
class NetworkUserRepository(
    private val apiService: ApiService
) : UserRepository {
    override suspend fun getUser(id: Int): User {
        return apiService.getUser(id)
    }

    override suspend fun getUsers(): List<User> {
        return apiService.getUsers()
    }
}

// Test implementation
class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<Int, User>()

    override suspend fun getUser(id: Int): User {
        return users[id] ?: throw NoSuchElementException("User $id not found")
    }

    override suspend fun getUsers(): List<User> {
        return users.values.toList()
    }

    fun addUser(user: User) {
        users[user.id] = user
    }
}
```

#### Test Usage

```kotlin
@Test
fun testUserViewModel() {
    val fakeRepository = FakeUserRepository()
    fakeRepository.addUser(User(1, "John", "john@example.com"))

    val viewModel = UserViewModel(fakeRepository)
    val user = viewModel.loadUser(1)

    assertEquals("John", user.name)
}
```

**Benefits**:
- Clean separation of concerns
- Easy to test
- No mocking framework needed
- Fake implementations reusable across tests
- Works on Java 24+

**Drawbacks**:
- Requires architectural refactoring
- Need to maintain fake implementations

### Strategy 4: Test Containers

**Use case**: Integration tests with real services

**Approach**: Use Testcontainers to run real services in Docker containers.

```kotlin
@Test
fun testRealApiIntegration() {
    // Start a real service in Docker
    val container = GenericContainer("myapi:latest")
        .withExposedPorts(8080)
    container.start()

    val baseUrl = "http://${container.host}:${container.getMappedPort(8080)}"
    val client = HttpClient(CIO)
    val response = client.get<User>("$baseUrl/users/1")

    assertEquals("John", response.name)

    container.stop()
}
```

**Benefits**:
- Tests against real implementations
- Catches integration issues
- Works on Java 24+

**Drawbacks**:
- Slower than unit tests
- Requires Docker
- More complex setup

**Recommended tools**:
- [Testcontainers](https://www.testcontainers.org/)

### Strategy 5: MockWebServer

**Use case**: HTTP client testing without real network

**Approach**: Use MockWebServer to simulate API responses locally.

```kotlin
@Test
fun testWithMockWebServer() {
    val server = MockWebServer()
    server.enqueue(MockResponse().setBody("""{"id":1,"name":"John"}"""))
    server.start()

    val client = HttpClient(OkHttp)
    val response = client.get<User>(server.url("/users/1").toString())

    assertEquals("John", response.name)

    server.shutdown()
}
```

**Benefits**:
- Simulates real HTTP behavior
- No mocking framework needed
- Works on Java 24+

**Drawbacks**:
- Slower than unit tests
- More setup code

**Recommended tools**:
- [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver) (OkHttp)

### Strategy 6: Separate Unit and Integration Tests

**Use case**: Clear separation of fast unit tests and slower integration tests

**Approach**: Split tests into two categories.

```kotlin
// Unit tests (fast, mocked)
class UserServiceUnitTest {
    @Test
    fun testUserService() {
        val mockClient = mock<HttpClient>()
        val service = UserService(mockClient)
        // Test with mocks
    }
}

// Integration tests (slower, real network or Docker)
class UserServiceIntegrationTest {
    @Test
    fun testRealApi() {
        val service = UserService()
        // Test with real network (or Testcontainers)
    }
}
```

Run unit tests frequently, integration tests less often.

## Comparison of Strategies

| Strategy | Complexity | Speed | Accuracy | Best For |
|----------|-----------|-------|----------|----------|
| **Mocking** | Low | ⚡ Fast | Medium | Unit tests |
| **Dependency Injection** | Medium | ⚡ Fast | Medium | Clean architecture |
| **Repository Pattern** | High | ⚡ Fast | Medium | Domain-driven design |
| **Testcontainers** | High | 🐢 Slow | High | Integration tests |
| **MockWebServer** | Low | ⚡ Medium | High | HTTP client tests |
| **Separate Tests** | Medium | Mixed | High | Mixed test suites |

## Migration Steps

### Phase 1: Assessment (Before Upgrading to Java 24)

1. **Inventory tests**: Find all tests using `@BlockNetworkRequests`
   ```bash
   grep -r "@BlockNetworkRequests" src/test/
   ```

2. **Categorize tests**:
   - Pure unit tests → Migrate to mocking
   - Integration tests → Migrate to Testcontainers or MockWebServer
   - API client tests → Migrate to MockWebServer

3. **Identify refactoring needs**:
   - Classes with hardcoded HTTP clients
   - Classes without dependency injection
   - Repository pattern opportunities

### Phase 2: Refactor Production Code

1. **Add dependency injection**:
   ```kotlin
   // Before
   class UserService {
       private val client = HttpClient(CIO)
   }

   // After
   class UserService(private val client: HttpClient = HttpClient(CIO))
   ```

2. **Extract interfaces** (if using Repository pattern):
   ```kotlin
   interface UserRepository {
       suspend fun getUser(id: Int): User
   }
   ```

3. **Add default parameters** for backward compatibility

### Phase 3: Migrate Tests

1. **Start with unit tests** (fastest migration):
   - Replace `@BlockNetworkRequests` with mocks
   - Inject mock dependencies

2. **Migrate integration tests**:
   - Set up Testcontainers or MockWebServer
   - Update test assertions

3. **Remove library**:
   - Remove plugin from `build.gradle.kts`
   - Remove `@BlockNetworkRequests` annotations
   - Remove `NoNetworkRule` / `NoNetworkExtension`

### Phase 4: Verify and Clean Up

1. **Run all tests**: `./gradlew test`
2. **Remove library dependency**
3. **Remove unused imports**
4. **Update documentation**

## Example: Complete Migration

### Before (Java 17-23)

```kotlin
class UserService {
    private val client = HttpClient(CIO)

    suspend fun getUser(id: Int): User {
        return client.get("https://api.example.com/users/$id")
    }
}

class UserServiceTest {
    @Test
    @BlockNetworkRequests
    fun testGetUser() {
        val service = UserService()
        assertFailsWith<NetworkRequestAttemptedException> {
            service.getUser(1)
        }
    }
}
```

### After (Java 24+)

```kotlin
// Production code - add dependency injection
class UserService(
    private val client: HttpClient = HttpClient(CIO),
    private val baseUrl: String = "https://api.example.com"
) {
    suspend fun getUser(id: Int): User {
        return client.get("$baseUrl/users/$id")
    }
}

// Unit test - use mocking
class UserServiceTest {
    @Test
    fun testGetUser() {
        val mockClient = mock<HttpClient>()
        val service = UserService(mockClient, "https://api.example.com")

        whenever(mockClient.get<User>(any())).thenReturn(User(1, "John"))

        val user = service.getUser(1)
        assertEquals("John", user.name)

        // Verify no real network call
        verify(mockClient).get<User>(any())
    }
}
```

## When to Start

**Start now**, even if you're on Java 17-21:
1. New code: Use dependency injection from the start
2. Existing code: Gradually refactor as you touch it
3. Tests: Migrate high-value tests first

## Resources

- [JEP 486: Permanently Disable and Deprecate for Removal Security Manager](https://openjdk.org/jeps/486)
- [MockK Documentation](https://mockk.io/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [MockWebServer Guide](https://github.com/square/okhttp/tree/master/mockwebserver)

## Support

This library will continue to work on Java 17-23. Bug fixes and updates will be provided for these versions. However, there will be no Java 24+ support due to SecurityManager removal.

## See Also

- [Compatibility Matrix](compatibility-matrix.md)
- [Setup Guides](setup-guides/)
- [Advanced Configuration](advanced-configuration.md)
