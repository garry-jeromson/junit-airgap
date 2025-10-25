package io.github.garryjeromson.junit.nonetwork.integration

import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkNotBlocked
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.Socket
import java.net.URL

/**
 * Integration tests simulating real-world testing scenarios.
 */
@ExtendWith(NoNetworkExtension::class)
class RealWorldScenariosTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    @BlockNetworkRequests
    fun `blocks API calls in unit tests`() {
        // Simulate a test that accidentally calls a real API
        assertNetworkBlocked("Real API calls should be blocked in unit tests") {
            val url = URL("https://api.github.com/users/test")
            url.openConnection().connect()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks database connections to remote hosts`() {
        // Simulate attempt to connect to remote database
        assertNetworkBlocked("Remote database connections should be blocked") {
            Socket("db.example.com", 5432) // PostgreSQL port
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"]) // Need both since DNS resolves
    fun `allows local database connections`() {
        // Simulate connection to local database (would need actual DB running)
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
        // Verify that file operations still work
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Hello, World!")

        val content = testFile.readText()
        assert(content == "Hello, World!")
    }

    @Test
    @BlockNetworkRequests
    fun `blocks HTTP requests to CDNs`() {
        assertNetworkBlocked("CDN requests should be blocked") {
            URL("https://cdn.jsdelivr.net/npm/package@1.0.0/file.js").openConnection().connect()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks analytics and tracking`() {
        assertNetworkBlocked("Analytics should be blocked") {
            Socket("analytics.google.com", 443)
        }

        assertNetworkBlocked("Tracking should be blocked") {
            Socket("track.hubspot.com", 443)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1", "testcontainers.local"])
    fun `allows TestContainers-like scenarios`() {
        // Simulate testcontainers usage where we allow localhost
        assertNetworkNotBlocked("Local test containers should work") {
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
            // Use URL.openConnection() which calls connect with actual port
            val url = URL("http://api.example.com:8080/test")
            url.openConnection().connect()
            throw AssertionError("Should have thrown NetworkRequestAttemptedException")
        } catch (e: io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException) {
            // Verify error message is helpful
            assert(e.message!!.contains("api.example.com")) {
                "Error message should contain host (got: ${e.message})"
            }
            assert(e.requestDetails != null) {
                "Should include request details"
            }
            // Port might be -1 for DNS lookups, or the actual port
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
            } catch (e: Throwable) {
                // Other exceptions are fine, just not NetworkRequestAttemptedException
                assert(e !is io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException) {
                    "Should not throw NetworkRequestAttemptedException without @BlockNetworkRequests"
                }
            }
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*.internal", "*.local"])
    fun `supports internal network patterns`() {
        // Test that internal network patterns work
        assertNetworkBlocked("External networks should be blocked") {
            Socket("public-api.example.com", 80)
        }
    }
}
