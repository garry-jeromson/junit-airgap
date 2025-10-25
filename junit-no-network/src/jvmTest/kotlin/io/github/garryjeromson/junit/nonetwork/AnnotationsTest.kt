package io.github.garryjeromson.junit.nonetwork

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnnotationsTest {
    @Test
    fun `NoNetworkTest annotation should be present and applicable to functions`() {
        val annotation =
            NoNetworkTestClass::class.java
                .getMethod("testMethod")
                .getAnnotation(NoNetworkTest::class.java)

        assertNotNull(annotation, "NoNetworkTest annotation should be present on test method")
    }

    @Test
    fun `AllowedHosts annotation should store host values`() {
        val annotation =
            AllowedHostsTestClass::class.java
                .getMethod("testMethod")
                .getAnnotation(AllowedHosts::class.java)

        assertNotNull(annotation)
        assertEquals(2, annotation.hosts.size)
        assertTrue(annotation.hosts.contains("localhost"))
        assertTrue(annotation.hosts.contains("127.0.0.1"))
    }

    @Test
    fun `BlockedHosts annotation should store host values`() {
        val annotation =
            BlockedHostsTestClass::class.java
                .getMethod("testMethod")
                .getAnnotation(BlockedHosts::class.java)

        assertNotNull(annotation)
        assertEquals(2, annotation.hosts.size)
        assertTrue(annotation.hosts.contains("evil.com"))
        assertTrue(annotation.hosts.contains("bad.example.com"))
    }

    @Test
    fun `AllowedHosts annotation should accept empty array`() {
        val annotation =
            EmptyAllowedHostsTestClass::class.java
                .getMethod("testMethod")
                .getAnnotation(AllowedHosts::class.java)

        assertNotNull(annotation)
        assertEquals(0, annotation.hosts.size)
    }

    // Test classes for annotation verification
    private class NoNetworkTestClass {
        @NoNetworkTest
        fun testMethod() {}
    }

    private class AllowedHostsTestClass {
        @AllowedHosts(hosts = ["localhost", "127.0.0.1"])
        fun testMethod() {}
    }

    private class BlockedHostsTestClass {
        @BlockedHosts(hosts = ["evil.com", "bad.example.com"])
        fun testMethod() {}
    }

    private class EmptyAllowedHostsTestClass {
        @AllowedHosts(hosts = [])
        fun testMethod() {}
    }
}
