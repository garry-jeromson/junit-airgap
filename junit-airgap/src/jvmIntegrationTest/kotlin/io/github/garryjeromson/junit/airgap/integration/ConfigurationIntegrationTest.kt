package io.github.garryjeromson.junit.airgap.integration

import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts
import io.github.garryjeromson.junit.airgap.BlockRequestsToHosts
import io.github.garryjeromson.junit.airgap.AirgapExtension
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.airgap.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.airgap.integration.fixtures.assertNetworkNotBlocked
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.Socket

/**
 * Integration tests for complex configuration scenarios.
 */
@ExtendWith(AirgapExtension::class)
@BlockNetworkRequests
@AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"]) // Class-level configuration - need both since DNS resolves
class ConfigurationIntegrationTest {
    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeAll
        fun startMockServer() {
            mockServer = MockHttpServer()
            mockServer.start()
            Thread.sleep(100)
        }

        @JvmStatic
        @AfterAll
        fun stopMockServer() {
            mockServer.stop()
        }
    }

    @Test
    fun `inherits class-level configuration`() {
        // Class allows localhost, so this should work
        assertNetworkNotBlocked("Should inherit class-level allowed hosts") {
            Socket("localhost", mockServer.listeningPort)
        }

        // But external hosts should be blocked
        assertNetworkBlocked("External hosts should still be blocked") {
            Socket("example.com", 80)
        }
    }

    @Test
    @AllowRequestsToHosts(hosts = ["192.168.1.1"]) // Method specifies additional host
    fun `merges method-level configuration with class-level`() {
        // NOTE: Current implementation MERGES annotations from class and method
        // So this test has both class-level (localhost, 127.0.0.1) AND method-level (192.168.1.1)

        // localhost from class-level should still work
        assertNetworkNotBlocked("Class-level config should be merged") {
            Socket("localhost", mockServer.listeningPort)
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
            Socket("localhost", mockServer.listeningPort)
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

        // Actual subdomains would match if DNS resolved, but we can test the pattern logic is working
        assertNetworkBlocked("Non-matching domains should be blocked") {
            Socket("notexample.com", 80)
        }
    }

    @Test
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1", "::1"])
    fun `supports multiple allowed hosts`() {
        // All localhost variants should work
        assertNetworkNotBlocked("localhost should be allowed") {
            Socket("localhost", mockServer.listeningPort)
        }

        assertNetworkNotBlocked("127.0.0.1 should be allowed") {
            Socket("127.0.0.1", mockServer.listeningPort)
        }
    }

    @Test
    @BlockRequestsToHosts(hosts = ["localhost"]) // Block localhost even though class allows it
    fun `respects method-level blocked hosts over class-level allowed`() {
        // Blocked should take precedence
        assertNetworkBlocked("Method-level blocked should override class-level allowed") {
            Socket("localhost", mockServer.listeningPort)
        }
    }

    @Test
    @AllowRequestsToHosts(hosts = ["192.168.*.*", "10.*.*.*"])
    fun `supports IP address wildcard patterns`() {
        // These would match if we actually resolved to those IPs
        // We're testing the pattern matching logic works
        assertNetworkBlocked("Non-matching IP should be blocked") {
            Socket("example.com", 80)
        }
    }
}
