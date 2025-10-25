package io.github.garryjeromson.junit.nonetwork.integration

import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkNotBlocked
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.Socket
import java.net.URL

/**
 * Android integration tests simulating real-world testing scenarios.
 * Tests common Android development patterns and use cases.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class AndroidRealWorldScenariosTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun `blocks API calls in Android unit tests`() {
        // Simulate a test that accidentally calls a real API
        assertNetworkBlocked("Real API calls should be blocked in unit tests") {
            val url = URL("https://api.github.com/users/test")
            url.openConnection().connect()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks OkHttp API calls`() {
        // OkHttp is very common in Android apps
        assertNetworkBlocked("OkHttp API calls should be blocked") {
            val client = OkHttpClient()
            val request =
                Request
                    .Builder()
                    .url("https://api.example.com/data")
                    .build()
            client.newCall(request).execute()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks remote database connections`() {
        // Simulate attempt to connect to remote database
        assertNetworkBlocked("Remote database connections should be blocked") {
            Socket("db.example.com", 5432) // PostgreSQL port
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `allows local database connections`() {
        // Simulate connection to local database
        assertNetworkNotBlocked("Local database should be allowed") {
            try {
                Socket("localhost", 5432)
            } catch (e: java.net.ConnectException) {
                // Connection refused is fine - we just verify it's not blocked
            }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `does not block file I-O operations`() {
        // Verify that file operations still work on Android
        val tempDir =
            File.createTempFile("test", "dir").apply {
                delete()
                mkdir()
            }

        try {
            val testFile = File(tempDir, "test.txt")
            testFile.writeText("Hello, Android!")

            val content = testFile.readText()
            assert(content == "Hello, Android!")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks image loading from CDN`() {
        // Common pattern in Android apps - loading images from CDN
        assertNetworkBlocked("CDN requests should be blocked") {
            URL("https://cdn.example.com/images/photo.jpg").openConnection().connect()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks analytics and crash reporting`() {
        // Block analytics services
        assertNetworkBlocked("Analytics should be blocked") {
            Socket("analytics.google.com", 443)
        }

        // Block crash reporting
        assertNetworkBlocked("Crash reporting should be blocked") {
            Socket("crashlytics.com", 443)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1", "10.0.2.2"]) // 10.0.2.2 is Android emulator host
    fun `allows Android emulator localhost scenarios`() {
        // Android emulator uses 10.0.2.2 to access host machine
        assertNetworkNotBlocked("Localhost should work") {
            try {
                Socket("localhost", 8080)
            } catch (e: java.net.ConnectException) {
                // Expected if no service running
            }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `provides clear error messages for blocked requests`() {
        try {
            val url = URL("http://api.example.com:8080/test")
            url.openConnection().connect()
            throw AssertionError("Should have thrown NetworkRequestAttemptedException")
        } catch (e: NetworkRequestAttemptedException) {
            // Verify error message is helpful
            assert(e.message!!.contains("api.example.com")) {
                "Error message should contain host (got: ${e.message})"
            }
            assert(e.requestDetails != null) {
                "Should include request details"
            }
            assert(e.requestDetails!!.host.contains("example.com")) {
                "Request details should have the host"
            }
        }
    }

    @Test
    fun `does not block requests when annotation is absent`() {
        // Without @BlockNetworkRequests, network should not be blocked
        assertNetworkNotBlocked("Network should not be blocked without annotation") {
            try {
                Socket("example.com", 80)
            } catch (e: Exception) {
                // Other exceptions are fine, just not NetworkRequestAttemptedException
                assert(e !is NetworkRequestAttemptedException) {
                    "Should not throw NetworkRequestAttemptedException without @BlockNetworkRequests"
                }
            }
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*.internal", "*.local"])
    fun `supports internal network patterns for enterprise apps`() {
        // Test that internal network patterns work (common in enterprise Android apps)
        assertNetworkBlocked("External networks should be blocked") {
            Socket("public-api.example.com", 80)
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks retrofit-style REST API calls`() {
        // Simulate Retrofit/REST API pattern (very common in Android)
        assertNetworkBlocked("REST API calls should be blocked") {
            val client = OkHttpClient()
            val request =
                Request
                    .Builder()
                    .url("https://api.example.com/v1/users")
                    .addHeader("Authorization", "Bearer token")
                    .build()
            client.newCall(request).execute()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks GraphQL API calls`() {
        // GraphQL is increasingly common in Android apps
        assertNetworkBlocked("GraphQL API calls should be blocked") {
            val client = OkHttpClient()
            val body =
                okhttp3.RequestBody.create(
                    "application/json".toMediaType(),
                    """{"query": "{ user(id: 1) { name } }"}""",
                )
            val request =
                Request
                    .Builder()
                    .url("https://api.example.com/graphql")
                    .post(body)
                    .build()
            client.newCall(request).execute()
        }
    }
}
