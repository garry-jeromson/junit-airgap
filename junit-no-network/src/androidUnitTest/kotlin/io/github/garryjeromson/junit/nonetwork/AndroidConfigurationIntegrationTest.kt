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

/**
 * Android integration tests for complex configuration scenarios using Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
@BlockNetworkRequests
@AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"]) // Class-level configuration
class AndroidConfigurationIntegrationTest {
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
    fun `inherits class-level configuration`() {
        // Class allows localhost, so this should work
        assertNetworkNotBlocked("Should inherit class-level allowed hosts") {
            Socket("localhost", MockHttpServer.DEFAULT_PORT)
        }

        // But external hosts should be blocked
        assertNetworkBlocked("External hosts should still be blocked") {
            Socket("example.com", 80)
        }
    }

    @Test
    @AllowRequestsToHosts(hosts = ["192.168.1.1"]) // Method specifies additional host
    fun `merges method-level configuration with class-level`() {
        // localhost from class-level should still work
        assertNetworkNotBlocked("Class-level config should be merged") {
            Socket("localhost", MockHttpServer.DEFAULT_PORT)
        }

        // External hosts should still be blocked
        assertNetworkBlocked("Non-allowed hosts should still be blocked") {
            Socket("example.com", 80)
        }
    }

    @Test
    @AllowRequestsToHosts(hosts = ["*"])
    @BlockRequestsToHosts(hosts = ["evil.com", "malicious.example.com"])
    fun `blocks specific hosts even with wildcard allowed`() {
        // Wildcard allows most hosts
        assertNetworkNotBlocked("Wildcard should allow most hosts") {
            Socket("localhost", MockHttpServer.DEFAULT_PORT)
        }

        // But explicitly blocked hosts should fail
        assertNetworkBlocked("Blocked hosts should fail even with wildcard") {
            Socket("evil.com", 80)
        }

        assertNetworkBlocked("Multiple blocked hosts should work") {
            Socket("malicious.example.com", 80)
        }
    }

    @Test
    @AllowRequestsToHosts(hosts = ["*.example.com"])
    fun `supports wildcard subdomain patterns`() {
        // *.example.com should match subdomains
        assertNetworkBlocked("Root domain should not match subdomain pattern") {
            Socket("example.com", 80)
        }

        // Non-matching domains should be blocked
        assertNetworkBlocked("Non-matching domains should be blocked") {
            Socket("notexample.com", 80)
        }
    }

    @Test
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1", "::1"])
    fun `supports multiple allowed hosts`() {
        // All localhost variants should work
        assertNetworkNotBlocked("localhost should be allowed") {
            Socket("localhost", MockHttpServer.DEFAULT_PORT)
        }

        assertNetworkNotBlocked("127.0.0.1 should be allowed") {
            Socket("127.0.0.1", MockHttpServer.DEFAULT_PORT)
        }
    }

    @Test
    @BlockRequestsToHosts(hosts = ["localhost"]) // Block localhost even though class allows it
    fun `respects method-level blocked hosts over class-level allowed`() {
        // Blocked should take precedence
        assertNetworkBlocked("Method-level blocked should override class-level allowed") {
            Socket("localhost", MockHttpServer.DEFAULT_PORT)
        }
    }

    @Test
    @AllowRequestsToHosts(hosts = ["192.168.*.*", "10.*.*.*"])
    fun `supports IP address wildcard patterns`() {
        // Non-matching IP should be blocked
        assertNetworkBlocked("Non-matching IP should be blocked") {
            Socket("example.com", 80)
        }
    }
}
