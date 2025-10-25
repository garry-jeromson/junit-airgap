package io.github.garryjeromson.junit.nonetwork

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for NetworkBlockerImplementation enum.
 *
 * TDD: These tests are written first to define expected behavior.
 */
class NetworkBlockerImplementationTest {
    @Test
    fun `has BYTE_BUDDY implementation`() {
        val impl = NetworkBlockerImplementation.BYTE_BUDDY
        assertEquals("BYTE_BUDDY", impl.name)
    }

    @Test
    fun `has SECURITY_MANAGER implementation`() {
        val impl = NetworkBlockerImplementation.SECURITY_MANAGER
        assertEquals("SECURITY_MANAGER", impl.name)
    }

    @Test
    fun `has AUTO implementation`() {
        val impl = NetworkBlockerImplementation.AUTO
        assertEquals("AUTO", impl.name)
    }

    @Test
    fun `parses bytebuddy string to BYTE_BUDDY`() {
        assertEquals(NetworkBlockerImplementation.BYTE_BUDDY, NetworkBlockerImplementation.fromString("bytebuddy"))
        assertEquals(NetworkBlockerImplementation.BYTE_BUDDY, NetworkBlockerImplementation.fromString("byte-buddy"))
        assertEquals(NetworkBlockerImplementation.BYTE_BUDDY, NetworkBlockerImplementation.fromString("BYTEBUDDY"))
    }

    @Test
    fun `parses securitymanager string to SECURITY_MANAGER`() {
        assertEquals(
            NetworkBlockerImplementation.SECURITY_MANAGER,
            NetworkBlockerImplementation.fromString("securitymanager"),
        )
        assertEquals(
            NetworkBlockerImplementation.SECURITY_MANAGER,
            NetworkBlockerImplementation.fromString("security-manager"),
        )
        assertEquals(
            NetworkBlockerImplementation.SECURITY_MANAGER,
            NetworkBlockerImplementation.fromString("SECURITYMANAGER"),
        )
    }

    @Test
    fun `parses auto string to AUTO`() {
        assertEquals(NetworkBlockerImplementation.AUTO, NetworkBlockerImplementation.fromString("auto"))
        assertEquals(NetworkBlockerImplementation.AUTO, NetworkBlockerImplementation.fromString("AUTO"))
    }

    @Test
    fun `returns default for null string`() {
        assertEquals(NetworkBlockerImplementation.SECURITY_MANAGER, NetworkBlockerImplementation.fromString(null))
    }

    @Test
    fun `throws exception for invalid string`() {
        assertFailsWith<IllegalArgumentException> {
            NetworkBlockerImplementation.fromString("invalid")
        }
    }

    @Test
    fun `default should be SECURITY_MANAGER`() {
        assertEquals(NetworkBlockerImplementation.SECURITY_MANAGER, NetworkBlockerImplementation.default())
    }
}
