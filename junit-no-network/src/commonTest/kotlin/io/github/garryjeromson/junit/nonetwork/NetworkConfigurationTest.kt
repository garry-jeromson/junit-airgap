package io.github.garryjeromson.junit.nonetwork

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkConfigurationTest {
    @Test
    fun `default configuration should block all hosts`() {
        val config = NetworkConfiguration()

        assertFalse(config.isAllowed("example.com"))
        assertFalse(config.isAllowed("google.com"))
        assertFalse(config.isAllowed("localhost"))
    }

    @Test
    fun `configuration with allowed hosts should permit those hosts`() {
        val config = NetworkConfiguration(allowedHosts = setOf("localhost", "127.0.0.1"))

        assertTrue(config.isAllowed("localhost"))
        assertTrue(config.isAllowed("127.0.0.1"))
        assertFalse(config.isAllowed("example.com"))
    }

    @Test
    fun `configuration with blocked hosts should block those specific hosts`() {
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("*"), // Allow all by default
                blockedHosts = setOf("evil.com", "bad.example.com"),
            )

        assertFalse(config.isAllowed("evil.com"))
        assertFalse(config.isAllowed("bad.example.com"))
        assertTrue(config.isAllowed("good.example.com"))
    }

    @Test
    fun `wildcard in allowed hosts should allow all except blocked`() {
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("*"),
                blockedHosts = setOf("blocked.com"),
            )

        assertTrue(config.isAllowed("example.com"))
        assertTrue(config.isAllowed("google.com"))
        assertFalse(config.isAllowed("blocked.com"))
    }

    @Test
    fun `blocked hosts should take precedence over allowed hosts`() {
        val config =
            NetworkConfiguration(
                allowedHosts = setOf("example.com", "*"),
                blockedHosts = setOf("example.com"),
            )

        assertFalse(config.isAllowed("example.com"), "Blocked hosts should take precedence")
    }

    @Test
    fun `host matching should be case-insensitive`() {
        val config = NetworkConfiguration(allowedHosts = setOf("Example.COM"))

        assertTrue(config.isAllowed("example.com"))
        assertTrue(config.isAllowed("EXAMPLE.COM"))
        assertTrue(config.isAllowed("Example.Com"))
    }

    @Test
    fun `merge should combine allowed hosts from two configurations`() {
        val config1 = NetworkConfiguration(allowedHosts = setOf("localhost"))
        val config2 = NetworkConfiguration(allowedHosts = setOf("127.0.0.1"))

        val merged = config1.merge(config2)

        assertTrue(merged.isAllowed("localhost"))
        assertTrue(merged.isAllowed("127.0.0.1"))
    }

    @Test
    fun `merge should combine blocked hosts from two configurations`() {
        val config1 =
            NetworkConfiguration(
                allowedHosts = setOf("*"),
                blockedHosts = setOf("evil.com"),
            )
        val config2 =
            NetworkConfiguration(
                allowedHosts = setOf("*"),
                blockedHosts = setOf("bad.com"),
            )

        val merged = config1.merge(config2)

        assertFalse(merged.isAllowed("evil.com"))
        assertFalse(merged.isAllowed("bad.com"))
    }

    @Test
    fun `pattern matching should support wildcards`() {
        val config = NetworkConfiguration(allowedHosts = setOf("*.example.com"))

        assertTrue(config.isAllowed("api.example.com"))
        assertTrue(config.isAllowed("www.example.com"))
        assertTrue(config.isAllowed("subdomain.example.com"))
        assertFalse(config.isAllowed("example.com"))
        assertFalse(config.isAllowed("notexample.com"))
    }

    @Test
    fun `pattern matching should support multiple wildcard levels`() {
        val config = NetworkConfiguration(allowedHosts = setOf("*.*.example.com"))

        assertTrue(config.isAllowed("api.v1.example.com"))
        assertTrue(config.isAllowed("www.cdn.example.com"))
        assertFalse(config.isAllowed("api.example.com"))
        assertFalse(config.isAllowed("example.com"))
    }
}
