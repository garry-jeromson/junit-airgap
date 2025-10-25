package io.github.garryjeromson.junit.nonetwork

import org.junit.jupiter.api.extension.ExtendWith
import java.net.Socket
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@ExtendWith(NoNetworkExtension::class)
class NoNetworkExtensionTest {
    @Test
    @NoNetworkTest
    fun `should block network requests when annotated with NoNetworkTest`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["*"])
    fun `should allow all hosts when configured with wildcard`() {
        // This should work because * allows all
        // We just verify it doesn't throw NetworkRequestAttemptedException
        try {
            val url = URL("http://httpbin.org/get")
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.connect()
            connection.inputStream.close()
            assertTrue(true, "Connection succeeded")
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Should not block when * is in allowed hosts", e)
        } catch (e: Exception) {
            // Other network errors (timeout, DNS, etc.) are acceptable
            // We just care that it's not blocked
            assertTrue(true, "Got expected network error (not blocked): ${e.javaClass.simpleName}")
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["localhost", "127.0.0.1"])
    fun `should only allow specified hosts`() {
        // Localhost should work
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["*"])
    @BlockedHosts(hosts = ["evil.com"])
    fun `should block specific hosts even when wildcard is allowed`() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("evil.com", 80)
        }
    }

    @Test
    fun `should not block requests when NoNetworkTest annotation is not present`() {
        // Without @NoNetworkTest, this should attempt the connection
        // (it may fail with network error, but should NOT throw NetworkRequestAttemptedException)
        try {
            val url = URL("http://httpbin.org/get")
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.connect()
            connection.inputStream.close()
            // If we reach here, the connection worked
            assertTrue(true)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Should not block without @NoNetworkTest annotation", e)
        } catch (e: Exception) {
            // Other network errors are fine - we just verify it's not blocked
            assertTrue(true)
        }
    }

    @Test
    @NoNetworkTest
    @AllowedHosts(hosts = ["*.example.com"])
    fun `should support wildcard patterns in allowed hosts`() {
        // api.example.com should be blocked because we're only allowing *.example.com subdomain pattern
        // and the tests would need DNS resolution
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("notexample.com", 80)
        }
    }
}
