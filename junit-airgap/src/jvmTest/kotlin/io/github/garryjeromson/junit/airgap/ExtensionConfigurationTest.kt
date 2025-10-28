package io.github.garryjeromson.junit.airgap

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for ExtensionConfiguration system property parsing.
 */
class ExtensionConfigurationTest {
    @AfterEach
    fun cleanup() {
        // Clear all system properties after each test
        System.clearProperty(APPLY_TO_ALL_TESTS_PROPERTY)
        System.clearProperty(ALLOWED_HOSTS_PROPERTY)
        System.clearProperty(BLOCKED_HOSTS_PROPERTY)
    }

    @Test
    fun `getAllowedHosts returns empty set when property not set`() {
        val hosts = ExtensionConfiguration.getAllowedHosts()
        assertTrue(hosts.isEmpty())
    }

    @Test
    fun `getAllowedHosts returns empty set when property is empty string`() {
        System.setProperty(ALLOWED_HOSTS_PROPERTY, "")
        val hosts = ExtensionConfiguration.getAllowedHosts()
        assertTrue(hosts.isEmpty())
    }

    @Test
    fun `getAllowedHosts parses single host`() {
        System.setProperty(ALLOWED_HOSTS_PROPERTY, "example.com")
        val hosts = ExtensionConfiguration.getAllowedHosts()
        assertEquals(setOf("example.com"), hosts)
    }

    @Test
    fun `getAllowedHosts parses multiple comma-separated hosts`() {
        System.setProperty(ALLOWED_HOSTS_PROPERTY, "localhost,example.com,google.com")
        val hosts = ExtensionConfiguration.getAllowedHosts()
        assertEquals(setOf("localhost", "example.com", "google.com"), hosts)
    }

    @Test
    fun `getAllowedHosts trims whitespace around hosts`() {
        System.setProperty(ALLOWED_HOSTS_PROPERTY, " localhost , example.com , google.com ")
        val hosts = ExtensionConfiguration.getAllowedHosts()
        assertEquals(setOf("localhost", "example.com", "google.com"), hosts)
    }

    @Test
    fun `getAllowedHosts filters out empty entries`() {
        System.setProperty(ALLOWED_HOSTS_PROPERTY, "localhost,,example.com,")
        val hosts = ExtensionConfiguration.getAllowedHosts()
        assertEquals(setOf("localhost", "example.com"), hosts)
    }

    @Test
    fun `getAllowedHosts supports wildcard pattern`() {
        System.setProperty(ALLOWED_HOSTS_PROPERTY, "*")
        val hosts = ExtensionConfiguration.getAllowedHosts()
        assertEquals(setOf("*"), hosts)
    }

    @Test
    fun `getAllowedHosts supports subdomain wildcard patterns`() {
        System.setProperty(ALLOWED_HOSTS_PROPERTY, "*.example.com,*.google.com")
        val hosts = ExtensionConfiguration.getAllowedHosts()
        assertEquals(setOf("*.example.com", "*.google.com"), hosts)
    }

    @Test
    fun `getBlockedHosts returns empty set when property not set`() {
        val hosts = ExtensionConfiguration.getBlockedHosts()
        assertTrue(hosts.isEmpty())
    }

    @Test
    fun `getBlockedHosts returns empty set when property is empty string`() {
        System.setProperty(BLOCKED_HOSTS_PROPERTY, "")
        val hosts = ExtensionConfiguration.getBlockedHosts()
        assertTrue(hosts.isEmpty())
    }

    @Test
    fun `getBlockedHosts parses single host`() {
        System.setProperty(BLOCKED_HOSTS_PROPERTY, "blocked.com")
        val hosts = ExtensionConfiguration.getBlockedHosts()
        assertEquals(setOf("blocked.com"), hosts)
    }

    @Test
    fun `getBlockedHosts parses multiple comma-separated hosts`() {
        System.setProperty(BLOCKED_HOSTS_PROPERTY, "blocked1.com,blocked2.com,blocked3.com")
        val hosts = ExtensionConfiguration.getBlockedHosts()
        assertEquals(setOf("blocked1.com", "blocked2.com", "blocked3.com"), hosts)
    }

    @Test
    fun `getBlockedHosts trims whitespace around hosts`() {
        System.setProperty(
            BLOCKED_HOSTS_PROPERTY,
            " blocked1.com , blocked2.com , blocked3.com ",
        )
        val hosts = ExtensionConfiguration.getBlockedHosts()
        assertEquals(setOf("blocked1.com", "blocked2.com", "blocked3.com"), hosts)
    }

    @Test
    fun `getBlockedHosts filters out empty entries`() {
        System.setProperty(BLOCKED_HOSTS_PROPERTY, "blocked1.com,,blocked2.com,")
        val hosts = ExtensionConfiguration.getBlockedHosts()
        assertEquals(setOf("blocked1.com", "blocked2.com"), hosts)
    }

    @Test
    fun `getBlockedHosts supports wildcard patterns`() {
        System.setProperty(BLOCKED_HOSTS_PROPERTY, "*.facebook.com,*.twitter.com")
        val hosts = ExtensionConfiguration.getBlockedHosts()
        assertEquals(setOf("*.facebook.com", "*.twitter.com"), hosts)
    }

    @Test
    fun `isApplyToAllTestsEnabled returns false when property not set`() {
        val enabled = ExtensionConfiguration.isApplyToAllTestsEnabled()
        assertEquals(false, enabled)
    }

    @Test
    fun `isApplyToAllTestsEnabled returns true when property is true`() {
        System.setProperty(APPLY_TO_ALL_TESTS_PROPERTY, "true")
        val enabled = ExtensionConfiguration.isApplyToAllTestsEnabled()
        assertEquals(true, enabled)
    }

    @Test
    fun `isApplyToAllTestsEnabled returns false when property is false`() {
        System.setProperty(APPLY_TO_ALL_TESTS_PROPERTY, "false")
        val enabled = ExtensionConfiguration.isApplyToAllTestsEnabled()
        assertEquals(false, enabled)
    }
}
