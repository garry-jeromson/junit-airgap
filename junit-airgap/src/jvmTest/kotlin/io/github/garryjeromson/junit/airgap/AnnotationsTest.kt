package io.github.garryjeromson.junit.airgap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnnotationsTest {
    @Test
    fun `BlockNetworkRequests annotation should be present and applicable to functions`() {
        val annotation =
            BlockNetworkRequestsClass::class.java
                .getMethod("testMethod")
                .getAnnotation(BlockNetworkRequests::class.java)

        assertNotNull(annotation, "BlockNetworkRequests annotation should be present on test method")
    }

    @Test
    fun `AllowRequestsToHosts annotation should store host values`() {
        val annotation =
            AllowRequestsToHostsTestClass::class.java
                .getMethod("testMethod")
                .getAnnotation(AllowRequestsToHosts::class.java)

        assertNotNull(annotation)
        assertEquals(2, annotation.hosts.size)
        assertTrue(annotation.hosts.contains("localhost"))
        assertTrue(annotation.hosts.contains("127.0.0.1"))
    }

    @Test
    fun `BlockRequestsToHosts annotation should store host values`() {
        val annotation =
            BlockRequestsToHostsTestClass::class.java
                .getMethod("testMethod")
                .getAnnotation(BlockRequestsToHosts::class.java)

        assertNotNull(annotation)
        assertEquals(2, annotation.hosts.size)
        assertTrue(annotation.hosts.contains("evil.com"))
        assertTrue(annotation.hosts.contains("bad.example.com"))
    }

    @Test
    fun `AllowRequestsToHosts annotation should accept empty array`() {
        val annotation =
            EmptyAllowRequestsToHostsTestClass::class.java
                .getMethod("testMethod")
                .getAnnotation(AllowRequestsToHosts::class.java)

        assertNotNull(annotation)
        assertEquals(0, annotation.hosts.size)
    }

    // Test classes for annotation verification
    private class BlockNetworkRequestsClass {
        @BlockNetworkRequests
        fun testMethod() {}
    }

    private class AllowRequestsToHostsTestClass {
        @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
        fun testMethod() {}
    }

    private class BlockRequestsToHostsTestClass {
        @BlockRequestsToHosts(hosts = ["evil.com", "bad.example.com"])
        fun testMethod() {}
    }

    private class EmptyAllowRequestsToHostsTestClass {
        @AllowRequestsToHosts(hosts = [])
        fun testMethod() {}
    }
}
