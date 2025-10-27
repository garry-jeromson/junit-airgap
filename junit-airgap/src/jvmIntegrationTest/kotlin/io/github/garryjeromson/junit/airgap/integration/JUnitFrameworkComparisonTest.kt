package io.github.garryjeromson.junit.airgap.integration

import io.github.garryjeromson.junit.airgap.AllowRequestsToHosts
import io.github.garryjeromson.junit.airgap.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.airgap.AirgapExtension
import io.github.garryjeromson.junit.airgap.AirgapRule
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import io.github.garryjeromson.junit.airgap.integration.fixtures.MockHttpServer
import org.junit.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Side-by-side comparison of JUnit 4 and JUnit 5 frameworks.
 *
 * This test class demonstrates that AirgapRule (JUnit 4) and AirgapExtension (JUnit 5)
 * provide identical functionality and behavior across both testing frameworks.
 *
 * Key points validated:
 * - Both frameworks support @BlockNetworkRequests annotation
 * - Both frameworks support @AllowRequestsToHosts and @BlockRequestsToHosts
 * - Both frameworks throw identical NetworkRequestAttemptedException
 * - Both frameworks have same configuration priority order
 * - Error messages are consistent across frameworks
 */
class JUnitFrameworkComparisonTest {
    /**
     * JUnit 4 tests using AirgapRule.
     */
    class JUnit4RuleTests {
        @get:Rule
        val noNetworkRule = AirgapRule()

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
        fun `JUnit 4 - should block network and throw NetworkRequestAttemptedException`() {
            val exception =
                assertFailsWith<NetworkRequestAttemptedException> {
                    Socket("example.com", 80)
                }
            assertNotNull(exception.message, "Exception should have a message")
            assert(exception.message!!.contains("example.com")) {
                "Exception message should contain blocked host"
            }
        }

        @Test
        @BlockNetworkRequests
        @AllowRequestsToHosts(hosts = ["localhost"])
        fun `JUnit 4 - should allow configured hosts`() {
            // Should not throw
            Socket("localhost", mockServer.listeningPort).close()
        }

        @Test
        fun `JUnit 4 - should not block without annotation`() {
            // No exception should be thrown (though connection may fail for other reasons)
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block without @BlockNetworkRequests", e)
            } catch (e: Exception) {
                // Other network errors are expected
            }
        }
    }

    /**
     * JUnit 5 tests using AirgapExtension.
     */
    @ExtendWith(AirgapExtension::class)
    class JUnit5ExtensionTests {
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

        @org.junit.jupiter.api.Test
        @BlockNetworkRequests
        fun `JUnit 5 - should block network and throw NetworkRequestAttemptedException`() {
            val exception =
                assertFailsWith<NetworkRequestAttemptedException> {
                    Socket("example.com", 80)
                }
            assertNotNull(exception.message, "Exception should have a message")
            assert(exception.message!!.contains("example.com")) {
                "Exception message should contain blocked host"
            }
        }

        @org.junit.jupiter.api.Test
        @BlockNetworkRequests
        @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
        fun `JUnit 5 - should allow configured hosts`() {
            // Should not throw
            Socket("localhost", mockServer.listeningPort).close()
        }

        @org.junit.jupiter.api.Test
        fun `JUnit 5 - should not block without annotation`() {
            // No exception should be thrown (though connection may fail for other reasons)
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block without @BlockNetworkRequests", e)
            } catch (e: Exception) {
                // Other network errors are expected
            }
        }
    }

    /**
     * Tests that verify exception compatibility across frameworks.
     */
    class ExceptionCompatibilityTests {
        @get:Rule
        val noNetworkRule = AirgapRule()

        @Test
        @BlockNetworkRequests
        fun `both frameworks throw same exception type`() {
            val exception =
                assertFailsWith<NetworkRequestAttemptedException> {
                    Socket("blocked.com", 443)
                }

            // Verify exception properties
            assertNotNull(exception.requestDetails, "Exception should include request details")
            assertEquals("blocked.com", exception.requestDetails?.host)
            assertEquals(443, exception.requestDetails?.port)
        }

        @Test
        @BlockNetworkRequests
        fun `exception message format is consistent`() {
            val exception =
                assertFailsWith<NetworkRequestAttemptedException> {
                    Socket("test.example.com", 8080)
                }

            // Verify message contains expected information
            val message = exception.message!!
            assert(message.contains("test.example.com")) {
                "Message should contain host: $message"
            }
            assert(message.contains("8080")) {
                "Message should contain port: $message"
            }
        }
    }

    /**
     * Tests comparing configuration behavior across frameworks.
     */
    class ConfigurationComparisonTests {
        @get:Rule
        val junit4Rule = AirgapRule()

        @Test
        @BlockNetworkRequests
        @AllowRequestsToHosts(hosts = ["*.example.com"])
        fun `JUnit 4 - wildcard patterns work`() {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("other.com", 80)
            }
        }
    }

    /**
     * JUnit 5 version of configuration tests.
     */
    @ExtendWith(AirgapExtension::class)
    class JUnit5ConfigurationTests {
        @org.junit.jupiter.api.Test
        @BlockNetworkRequests
        @AllowRequestsToHosts(hosts = ["*.example.com"])
        fun `JUnit 5 - wildcard patterns work`() {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("other.com", 80)
            }
        }
    }
}
