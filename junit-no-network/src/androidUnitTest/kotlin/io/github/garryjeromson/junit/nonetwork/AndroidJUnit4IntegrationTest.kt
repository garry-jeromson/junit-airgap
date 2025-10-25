package io.github.garryjeromson.junit.nonetwork

import io.github.garryjeromson.junit.nonetwork.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkNotBlocked
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.Socket
import java.net.URL

/**
 * Comprehensive Android integration tests for JUnit 4 Rule support using Robolectric.
 *
 * This test validates that NoNetworkRule works correctly on Android with JUnit 4.
 * Mirrors the JVM JUnit4IntegrationTest to ensure feature parity across platforms.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26]) // Test on API 26 (Android 8.0)
class AndroidJUnit4IntegrationTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeClass
        fun startMockServer() {
            mockServer = MockHttpServer(MockHttpServer.DEFAULT_PORT)
            mockServer.start()
            Thread.sleep(100)
        }

        @JvmStatic
        @AfterClass
        fun stopMockServer() {
            mockServer.stop()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `should block network requests with JUnit 4 Rule on Android`() {
        assertNetworkBlocked("JUnit 4 Rule should block network on Android") {
            Socket("example.com", 80)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `should allow configured hosts with Rule on Android`() {
        assertNetworkNotBlocked("JUnit 4 Rule should allow configured hosts on Android") {
            Socket("localhost", MockHttpServer.DEFAULT_PORT)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    @BlockRequestsToHosts(hosts = ["evil.com"])
    fun `should respect blocked hosts with Rule on Android`() {
        assertNetworkBlocked("JUnit 4 Rule should respect blocked hosts on Android") {
            Socket("evil.com", 80)
        }
    }

    @Test
    fun `should not block without annotation on Android`() {
        // Without @BlockNetworkRequests, network should not be blocked
        assertNetworkNotBlocked("JUnit 4 should not block without annotation on Android") {
            try {
                URL("http://example.com").openConnection()
            } catch (e: Exception) {
                // Other exceptions are fine
            }
        }
    }

    @Test
    @BlockNetworkRequests
    fun `should work across multiple tests in same class on Android`() {
        // Verify that the rule properly installs/uninstalls between tests
        assertNetworkBlocked("Multiple JUnit 4 tests should work on Android") {
            Socket("example.com", 80)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1"])
    fun `should support IP addresses with Rule on Android`() {
        assertNetworkNotBlocked("JUnit 4 Rule should support IP addresses on Android") {
            Socket("127.0.0.1", MockHttpServer.DEFAULT_PORT)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*.trusted.com", "localhost", "127.0.0.1"])
    fun `should support multiple allowed hosts with wildcards on Android`() {
        assertNetworkNotBlocked("Multiple allowed hosts should work on Android") {
            Socket("localhost", MockHttpServer.DEFAULT_PORT)
        }

        assertNetworkBlocked("Non-whitelisted hosts should be blocked on Android") {
            Socket("example.com", 80)
        }
    }

    @Test
    @BlockNetworkRequests
    fun `should block HTTPS connections with Rule on Android`() {
        assertNetworkBlocked("HTTPS should be blocked with JUnit 4 Rule on Android") {
            try {
                URL("https://example.com").openConnection().connect()
            } catch (e: NetworkRequestAttemptedException) {
                throw e
            } catch (e: Exception) {
                // SSL errors are fine, we just want to verify blocking happens
                throw AssertionError("Expected NetworkRequestAttemptedException", e)
            }
        }
    }
}
