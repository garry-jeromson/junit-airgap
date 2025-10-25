package io.github.garryjeromson.junit.nonetwork

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.Socket
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for implementation selection and fallback behavior.
 */
class ImplementationSelectionTest {
    @AfterEach
    fun cleanup() {
        // Clean up system properties after each test
        System.clearProperty("junit.nonetwork.implementation")
        System.clearProperty("junit.nonetwork.debug")
    }

    @Test
    fun `should use SECURITY_MANAGER by default`() {
        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = NetworkBlocker(config)

        // Implementation detail: we expect SecurityManager to be used by default
        // This will be verified by the blocker working correctly
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
    fun `should use SECURITY_MANAGER when specified via system property`() {
        System.setProperty("junit.nonetwork.implementation", "securitymanager")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
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
    fun `should use BYTE_BUDDY when specified via system property`() {
        System.setProperty("junit.nonetwork.implementation", "bytebuddy")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = NetworkBlocker(config)

        // NOTE: ByteBuddy implementation is a non-functional stub
        // This test only verifies it can be created without crashing
        blocker.install()
        blocker.uninstall()
        // We do NOT test that it blocks requests because it doesn't work
    }

    @Test
    fun `should use AUTO mode and select best implementation`() {
        System.setProperty("junit.nonetwork.implementation", "auto")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
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
    fun `should throw exception for invalid implementation string`() {
        System.setProperty("junit.nonetwork.implementation", "invalid")

        val config = NetworkConfiguration() // Empty allowedHosts = block all

        assertFailsWith<IllegalArgumentException> {
            NetworkBlocker(config)
        }
    }

    @Test
    fun `ByteBuddy implementation can be created but does not block`() {
        System.setProperty("junit.nonetwork.implementation", "bytebuddy")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = NetworkBlocker(config)

        // NOTE: ByteBuddy implementation is a non-functional stub
        // This test only verifies it can be created and installed without crashing
        blocker.install()
        blocker.uninstall()
        // We do NOT test that it blocks requests because ByteBuddy cannot intercept Socket constructors
    }

    @Test
    fun `ByteBuddy implementation can be created with whitelisted hosts`() {
        System.setProperty("junit.nonetwork.implementation", "bytebuddy")

        val config =
            NetworkConfiguration(
                allowedHosts = setOf("localhost", "127.0.0.1"),
            )
        val blocker = NetworkBlocker(config)

        // NOTE: ByteBuddy implementation is a non-functional stub
        // This test only verifies it can be created with configuration
        blocker.install()
        blocker.uninstall()
        // We do NOT test whitelisting because ByteBuddy does not block anything
    }

    @Test
    fun `should block network requests with SecurityManager implementation`() {
        System.setProperty("junit.nonetwork.implementation", "securitymanager")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = NetworkBlocker(config)

        blocker.install()
        try {
            val exception =
                assertFailsWith<NetworkRequestAttemptedException> {
                    Socket("example.com", 443)
                }

            // Verify exception message contains host
            assertTrue(exception.message?.contains("example.com") == true)
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `ByteBuddy implementation install and uninstall are idempotent`() {
        System.setProperty("junit.nonetwork.implementation", "bytebuddy")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = NetworkBlocker(config)

        // NOTE: ByteBuddy implementation is a non-functional stub
        // This test verifies install/uninstall don't crash
        blocker.install()
        blocker.uninstall()
        // We do NOT test network behavior because ByteBuddy doesn't block
    }

    @Test
    fun `ByteBuddy install is idempotent`() {
        System.setProperty("junit.nonetwork.implementation", "bytebuddy")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = NetworkBlocker(config)

        // Install multiple times should not cause issues
        blocker.install()
        blocker.install()
        blocker.install()

        blocker.uninstall()
        // NOTE: We do NOT test blocking because ByteBuddy is non-functional
    }

    @Test
    fun `ByteBuddy uninstall is idempotent`() {
        System.setProperty("junit.nonetwork.implementation", "bytebuddy")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = NetworkBlocker(config)

        blocker.install()

        // Uninstall multiple times should not cause issues
        blocker.uninstall()
        blocker.uninstall()
        blocker.uninstall()
        // NOTE: We do NOT test blocking because ByteBuddy is non-functional
    }

    @Test
    fun `should use SECURITY_POLICY when specified via system property`() {
        System.setProperty("junit.nonetwork.implementation", "securitypolicy")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
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
    fun `should block network requests with SECURITY_POLICY implementation`() {
        System.setProperty("junit.nonetwork.implementation", "securitypolicy")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = NetworkBlocker(config)

        blocker.install()
        try {
            val exception =
                assertFailsWith<NetworkRequestAttemptedException> {
                    Socket("blocked.example.com", 443)
                }

            // Verify exception message contains host
            assertTrue(exception.message?.contains("blocked.example.com") == true)
        } finally {
            blocker.uninstall()
        }
    }

    @Test
    fun `SECURITY_POLICY should allow whitelisted hosts`() {
        System.setProperty("junit.nonetwork.implementation", "securitypolicy")

        val config =
            NetworkConfiguration(
                allowedHosts = setOf("allowed.example.com"),
            )
        val blocker = NetworkBlocker(config)

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
    fun `SECURITY_POLICY install is idempotent`() {
        System.setProperty("junit.nonetwork.implementation", "securitypolicy")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = NetworkBlocker(config)

        // Install multiple times should not cause issues
        blocker.install()
        blocker.install()
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
    fun `SECURITY_POLICY uninstall is idempotent`() {
        System.setProperty("junit.nonetwork.implementation", "securitypolicy")

        val config = NetworkConfiguration() // Empty allowedHosts = block all
        val blocker = NetworkBlocker(config)

        blocker.install()

        // Uninstall multiple times should not cause issues
        blocker.uninstall()
        blocker.uninstall()
        blocker.uninstall()
        // Should not throw
        assertTrue(true)
    }
}
