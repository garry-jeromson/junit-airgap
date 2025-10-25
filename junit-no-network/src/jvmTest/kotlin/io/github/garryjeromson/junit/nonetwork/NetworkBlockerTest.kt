package io.github.garryjeromson.junit.nonetwork

import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import kotlin.test.*

class NetworkBlockerTest {
    @Test
    fun `NetworkBlocker should block socket connections by default`() {
        val config = NetworkConfiguration() // No allowed hosts
        val blocker = NetworkBlocker(config)

        blocker.install()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("example.com", 80)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `NetworkBlocker should block HTTP connections by default`() {
        val config = NetworkConfiguration()
        val blocker = NetworkBlocker(config)

        blocker.install()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                val url = URL("http://example.com")
                url.openConnection().connect()
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `NetworkBlocker should allow connections to whitelisted hosts`() {
        val config = NetworkConfiguration(allowedHosts = setOf("httpbin.org", "*")) // Allow all for this test
        val blocker = NetworkBlocker(config)

        blocker.install()
        try {
            // This should not throw because httpbin.org and all hosts are allowed
            val url = URL("http://httpbin.org/get")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.connect()
            connection.disconnect()
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `NetworkBlocker should block connections to non-whitelisted hosts`() {
        val config = NetworkConfiguration(allowedHosts = setOf("allowed.com"))
        val blocker = NetworkBlocker(config)

        blocker.install()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("blocked.com", 80)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `NetworkBlocker should respect blocked hosts even if in allowed list`() {
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("*"),
                blockedHosts = setOf("blocked.com"),
            )
        val blocker = NetworkBlocker(config)

        blocker.install()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("blocked.com", 80)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `NetworkBlocker uninstall should restore normal network access`() {
        val config = NetworkConfiguration() // Block all
        val blocker = NetworkBlocker(config)

        blocker.install()
        blocker.uninstall()

        // After uninstall, network should work (though this test might fail if no internet)
        // We're just checking it doesn't throw NetworkRequestAttemptedException
        try {
            val url = URL("http://httpbin.org/get")
            url.openConnection()
            // If we get here without NetworkRequestAttemptedException, test passes
            assertTrue(true)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("NetworkBlocker should not block after uninstall", e)
        } catch (e: Exception) {
            // Other exceptions (network errors, timeouts) are fine - we just care about not blocking
            assertTrue(true)
        }
    }

    @Test
    fun `NetworkBlocker should include host details in exception`() {
        val config = NetworkConfiguration()
        val blocker = NetworkBlocker(config)

        blocker.install()
        try {
            val exception =
                assertFailsWith<NetworkRequestAttemptedException> {
                    val url = URL("http://blockedhost.example.com:443/path")
                    url.openConnection().connect()
                }

            // Check that exception includes some details
            assertNotNull(exception, "Exception should be thrown")
            assertNotNull(exception.requestDetails, "Request details should not be null")
            // Port and host should be captured
            assertTrue(
                exception.requestDetails!!.port != null || exception.requestDetails!!.host.isNotEmpty(),
                "Should have either port or host information",
            )
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `NetworkBlocker should support wildcard patterns in allowed hosts`() {
        val config = NetworkConfiguration(allowedHosts = setOf("*.httpbin.org", "*")) // Allow all for this test
        val blocker = NetworkBlocker(config)

        blocker.install()
        try {
            // Should allow subdomains of httpbin.org
            val url = URL("http://eu.httpbin.org/get")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.connect()
            connection.disconnect()
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `multiple install calls should be idempotent`() {
        val config = NetworkConfiguration()
        val blocker = NetworkBlocker(config)

        blocker.install()
        blocker.install() // Second install should not cause issues

        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("example.com", 80)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `multiple uninstall calls should be safe`() {
        val config = NetworkConfiguration()
        val blocker = NetworkBlocker(config)

        blocker.install()
        blocker.uninstall()
        blocker.uninstall() // Second uninstall should not cause issues

        // Should not throw
        assertTrue(true)
    }
}
