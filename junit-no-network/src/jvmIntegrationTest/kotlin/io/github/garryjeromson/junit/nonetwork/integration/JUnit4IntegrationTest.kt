package io.github.garryjeromson.junit.nonetwork.integration

import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.BlockRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkNotBlocked
import org.junit.*
import java.net.Socket
import java.net.URL

/**
 * Integration tests for JUnit 4 Rule support.
 */
class JUnit4IntegrationTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeClass
        fun startMockServer() {
            mockServer = MockHttpServer()
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
    fun shouldBlockNetworkRequestsWithRule() {
        assertNetworkBlocked("JUnit 4 Rule should block network") {
            Socket("example.com", 80)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost"])
    fun shouldAllowConfiguredHostsWithRule() {
        assertNetworkNotBlocked("JUnit 4 Rule should allow configured hosts") {
            Socket("localhost", mockServer.listeningPort)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    @BlockRequestsToHosts(hosts = ["evil.com"])
    fun shouldRespectBlockedHostsWithRule() {
        assertNetworkBlocked("JUnit 4 Rule should respect blocked hosts") {
            Socket("evil.com", 80)
        }
    }

    @Test
    fun shouldNotBlockWithoutAnnotation() {
        // Without @BlockNetworkRequests, network should not be blocked
        assertNetworkNotBlocked("JUnit 4 should not block without annotation") {
            try {
                URL("http://example.com").openConnection()
            } catch (e: Exception) {
                // Other exceptions are fine
            }
        }
    }

    @Test
    @BlockNetworkRequests
    fun shouldWorkAcrossMultipleTestsInSameClass() {
        // Verify that the rule properly installs/uninstalls between tests
        assertNetworkBlocked("Multiple tests should work") {
            Socket("example.com", 80)
        }
    }
}
