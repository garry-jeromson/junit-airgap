package io.github.garryjeromson.junit.nonetwork

import java.net.Socket
import java.net.URL
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for SecurityPolicyNetworkBlocker implementation.
 */
class SecurityPolicyNetworkBlockerTest {
    @AfterTest
    fun cleanup() {
        // Ensure we don't leave any security manager or policy installed
        try {
            // First restore the original policy (needed to allow setSecurityManager)
            java.security.Policy.setPolicy(null)
        } catch (e: Exception) {
            // Ignore
        }
        try {
            @Suppress("DEPRECATION")
            System.setSecurityManager(null)
        } catch (e: Exception) {
            // Ignore
        }
    }

    @Test
    fun `blocks all network requests with empty allowed hosts`() {
        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = SecurityPolicyNetworkBlocker(config)

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
    fun `blocks HTTPS connections`() {
        val config = NetworkConfiguration()
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                val url = URL("https://example.com:443")
                url.openConnection().connect()
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `allows localhost connections by default`() {
        val config = NetworkConfiguration()
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        try {
            // Localhost should always be allowed
            // This might fail if nothing is listening, but should NOT throw NetworkRequestAttemptedException
            try {
                Socket("localhost", 8080)
            } catch (e: Exception) {
                // Connection refused or other exceptions are fine
                // We just care that it's not NetworkRequestAttemptedException
                assertTrue(e !is NetworkRequestAttemptedException)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `allows 127_0_0_1 connections by default`() {
        val config = NetworkConfiguration()
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        try {
            // 127.0.0.1 should always be allowed
            try {
                Socket("127.0.0.1", 8080)
            } catch (e: Exception) {
                assertTrue(e !is NetworkRequestAttemptedException)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `allows connections to hosts in allowedHosts`() {
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("allowed.example.com"),
            )
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        try {
            // Should not throw NetworkRequestAttemptedException
            // May throw other exceptions (connection refused, etc.)
            try {
                Socket("allowed.example.com", 80)
            } catch (e: Exception) {
                assertTrue(e !is NetworkRequestAttemptedException)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `blocks connections to hosts not in allowedHosts`() {
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("allowed.example.com"),
            )
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("blocked.example.com", 80)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `blocks hosts in blockedHosts even if allowedHosts contains wildcard`() {
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("*"),
                blockedHosts = setOf("evil.com"),
            )
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("evil.com", 80)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `allows wildcard patterns in allowedHosts`() {
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("*.example.com"),
            )
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        try {
            // Should allow subdomain
            try {
                Socket("api.example.com", 80)
            } catch (e: Exception) {
                assertTrue(e !is NetworkRequestAttemptedException)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `blocks root domain when wildcard pattern is used`() {
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("*.example.com"),
            )
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        try {
            // *.example.com should NOT match example.com itself
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("example.com", 80)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `install should be idempotent`() {
        val config = NetworkConfiguration()
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        blocker.install() // Second install should not cause issues
        blocker.install() // Third install should not cause issues

        try {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("example.com", 80)
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `uninstall should be idempotent`() {
        val config = NetworkConfiguration()
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        blocker.uninstall()
        blocker.uninstall() // Second uninstall should not cause issues
        blocker.uninstall() // Third uninstall should not cause issues

        // Should not throw
        assertTrue(true)
    }

    @Test
    fun `uninstall should restore original policy`() {
        val config = NetworkConfiguration()
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        blocker.uninstall()

        // After uninstall, network should work (no NetworkRequestAttemptedException)
        try {
            // This will likely fail with connection error, but should NOT be NetworkRequestAttemptedException
            Socket("example.com", 80)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Should not block network after uninstall", e)
        } catch (e: Exception) {
            // Other exceptions are fine
            assertTrue(true)
        }
    }

    @Test
    fun `exception should include host details`() {
        val config = NetworkConfiguration()
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        try {
            val exception =
                assertFailsWith<NetworkRequestAttemptedException> {
                    Socket("api.blocked.com", 443)
                }

            assertNotNull(exception.requestDetails)
            assertTrue(exception.requestDetails!!.host.contains("api.blocked.com"))
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `exception should include port details`() {
        val config = NetworkConfiguration()
        val blocker = SecurityPolicyNetworkBlocker(config)

        blocker.install()
        try {
            val exception =
                assertFailsWith<NetworkRequestAttemptedException> {
                    Socket("example.com", 8443)
                }

            assertNotNull(exception.requestDetails)
            // Port can be -1 (for DNS resolution) or the actual port
            assertTrue(exception.requestDetails!!.port == 8443 || exception.requestDetails!!.port == -1)
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `isAvailable should return true on standard JVM`() {
        val config = NetworkConfiguration()
        val blocker = SecurityPolicyNetworkBlocker(config)

        assertTrue(blocker.isAvailable())
    }

    @Test
    fun `getImplementation should return SECURITY_POLICY`() {
        val config = NetworkConfiguration()
        val blocker = SecurityPolicyNetworkBlocker(config)

        assertEquals(NetworkBlockerImplementation.SECURITY_POLICY, blocker.getImplementation())
    }
}
