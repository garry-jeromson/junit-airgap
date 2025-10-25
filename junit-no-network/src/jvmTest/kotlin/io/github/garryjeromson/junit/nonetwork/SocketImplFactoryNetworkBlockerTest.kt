package io.github.garryjeromson.junit.nonetwork

import java.net.Socket
import java.net.URL
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests for SocketImplFactoryNetworkBlocker implementation.
 *
 * NOTE: These tests may affect other tests in the same JVM because
 * Socket.setSocketImplFactory() can only be called once per JVM.
 */
class SocketImplFactoryNetworkBlockerTest {
    @AfterTest
    fun cleanup() {
        // Note: Cannot remove SocketImplFactory once installed
        // The factory remains but configurations are removed via uninstall()
    }

    @Test
    fun `blocks all network requests with empty allowed hosts`() {
        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = SocketImplFactoryNetworkBlocker(config)

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
        val blocker = SocketImplFactoryNetworkBlocker(config)

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
        val blocker = SocketImplFactoryNetworkBlocker(config)

        blocker.install()
        try {
            // Localhost should always be allowed
            // This might fail if nothing is listening, but should NOT throw NetworkRequestAttemptedException
            try {
                Socket("localhost", 8080)
            } catch (e: NetworkRequestAttemptedException) {
                fail("Should not throw NetworkRequestAttemptedException for localhost: ${e.message}")
            } catch (e: Exception) {
                // Connection refused or other exceptions are fine
            }
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `allows 127_0_0_1 connections by default`() {
        val config = NetworkConfiguration()
        val blocker = SocketImplFactoryNetworkBlocker(config)

        blocker.install()
        try {
            // 127.0.0.1 should always be allowed
            try {
                Socket("127.0.0.1", 8080)
            } catch (e: NetworkRequestAttemptedException) {
                fail("Should not throw NetworkRequestAttemptedException for 127.0.0.1: ${e.message}")
            } catch (e: Exception) {
                // Connection refused or other exceptions are fine
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
        val blocker = SocketImplFactoryNetworkBlocker(config)

        blocker.install()
        try {
            // Should not throw NetworkRequestAttemptedException
            // May throw other exceptions (connection refused, etc.)
            try {
                Socket("allowed.example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                fail("Should not throw NetworkRequestAttemptedException for allowed host: ${e.message}")
            } catch (e: Exception) {
                // Other exceptions (connection refused, timeout, etc.) are acceptable
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
        val blocker = SocketImplFactoryNetworkBlocker(config)

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
        val blocker = SocketImplFactoryNetworkBlocker(config)

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
        val blocker = SocketImplFactoryNetworkBlocker(config)

        blocker.install()
        try {
            // Should allow subdomain
            try {
                Socket("api.example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                fail("Should not throw NetworkRequestAttemptedException for wildcard subdomain: ${e.message}")
            } catch (e: Exception) {
                // Other exceptions (connection refused, timeout, etc.) are acceptable
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
        val blocker = SocketImplFactoryNetworkBlocker(config)

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
        val blocker = SocketImplFactoryNetworkBlocker(config)

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
        val blocker = SocketImplFactoryNetworkBlocker(config)

        blocker.install()
        blocker.uninstall()
        blocker.uninstall() // Second uninstall should not cause issues
        blocker.uninstall() // Third uninstall should not cause issues

        // Should not throw
        assertTrue(true)
    }

    @Test
    fun `uninstall should stop blocking network requests`() {
        val config = NetworkConfiguration()
        val blocker = SocketImplFactoryNetworkBlocker(config)

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
        val blocker = SocketImplFactoryNetworkBlocker(config)

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
        val blocker = SocketImplFactoryNetworkBlocker(config)

        blocker.install()
        try {
            val exception =
                assertFailsWith<NetworkRequestAttemptedException> {
                    Socket("example.com", 8443)
                }

            assertNotNull(exception.requestDetails)
            assertEquals(8443, exception.requestDetails!!.port)
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `isAvailable should return true on standard JVM`() {
        val config = NetworkConfiguration()
        val blocker = SocketImplFactoryNetworkBlocker(config)

        assertTrue(blocker.isAvailable())
    }

    @Test
    fun `getImplementation should return SOCKET_IMPL_FACTORY`() {
        val config = NetworkConfiguration()
        val blocker = SocketImplFactoryNetworkBlocker(config)

        assertEquals(NetworkBlockerImplementation.SOCKET_IMPL_FACTORY, blocker.getImplementation())
    }

    @Test
    fun `multiple configurations can coexist`() {
        val config1 =
            NetworkConfiguration(
                allowedHosts = setOf("allowed1.com"),
            )
        val config2 =
            NetworkConfiguration(
                allowedHosts = setOf("allowed2.com"),
            )

        val blocker1 = SocketImplFactoryNetworkBlocker(config1)
        val blocker2 = SocketImplFactoryNetworkBlocker(config2)

        blocker1.install()
        blocker2.install()

        try {
            // Both allowed1.com and allowed2.com should be allowed
            try {
                Socket("allowed1.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                fail("allowed1.com should be allowed: ${e.message}")
            } catch (e: Exception) {
                // Connection errors are fine
            }

            try {
                Socket("allowed2.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                fail("allowed2.com should be allowed: ${e.message}")
            } catch (e: Exception) {
                // Connection errors are fine
            }

            // Other hosts should be blocked
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("blocked.com", 80)
            }
        } finally {
            blocker1.uninstall()
            blocker2.uninstall()
        }
    }
}
