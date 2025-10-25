package io.github.garryjeromson.junit.nonetwork

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for NetworkBlockerImplementation enum.
 */
class NetworkBlockerImplementationTest {
    @Test
    fun `has JVMTI implementation`() {
        val impl = NetworkBlockerImplementation.JVMTI
        assertEquals("JVMTI", impl.name)
    }

    @Test
    fun `parses jvmti string to JVMTI`() {
        assertEquals(NetworkBlockerImplementation.JVMTI, NetworkBlockerImplementation.fromString("jvmti"))
        assertEquals(NetworkBlockerImplementation.JVMTI, NetworkBlockerImplementation.fromString("JVMTI"))
    }

    @Test
    fun `fromString handles null by returning default`() {
        assertEquals(NetworkBlockerImplementation.JVMTI, NetworkBlockerImplementation.fromString(null))
    }

    @Test
    fun `throws exception for invalid string`() {
        assertFailsWith<IllegalArgumentException> {
            NetworkBlockerImplementation.fromString("invalid")
        }
    }

    @Test
    fun `default should be JVMTI`() {
        assertEquals(NetworkBlockerImplementation.JVMTI, NetworkBlockerImplementation.default())
    }
}
