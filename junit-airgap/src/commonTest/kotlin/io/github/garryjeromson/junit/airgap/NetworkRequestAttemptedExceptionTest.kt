package io.github.garryjeromson.junit.airgap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NetworkRequestAttemptedExceptionTest {
    @Test
    fun `exception should contain basic message`() {
        val exception = NetworkRequestAttemptedException("Network request blocked")

        assertEquals("Network request blocked", exception.message)
    }

    @Test
    fun `exception should include host in message when details provided`() {
        val details = NetworkRequestDetails(host = "example.com")
        val exception =
            NetworkRequestAttemptedException(
                "Network request blocked",
                requestDetails = details,
            )

        assertNotNull(exception.message)
        assertTrue(exception.message!!.contains("example.com"))
        assertTrue(exception.message!!.contains("Host: example.com"))
    }

    @Test
    fun `exception should include port in message when provided`() {
        val details =
            NetworkRequestDetails(
                host = "example.com",
                port = 443,
            )
        val exception =
            NetworkRequestAttemptedException(
                "Network request blocked",
                requestDetails = details,
            )

        assertNotNull(exception.message)
        assertTrue(exception.message!!.contains("Port: 443"))
    }

    @Test
    fun `exception should include protocol in message when provided`() {
        val details =
            NetworkRequestDetails(
                host = "example.com",
                protocol = "https",
            )
        val exception =
            NetworkRequestAttemptedException(
                "Network request blocked",
                requestDetails = details,
            )

        assertNotNull(exception.message)
        assertTrue(exception.message!!.contains("Protocol: https"))
    }

    @Test
    fun `exception should include URL in message when provided`() {
        val details =
            NetworkRequestDetails(
                host = "example.com",
                url = "https://example.com/api/users",
            )
        val exception =
            NetworkRequestAttemptedException(
                "Network request blocked",
                requestDetails = details,
            )

        assertNotNull(exception.message)
        assertTrue(exception.message!!.contains("URL: https://example.com/api/users"))
    }

    @Test
    fun `exception should include all details when provided`() {
        val details =
            NetworkRequestDetails(
                host = "api.example.com",
                port = 443,
                protocol = "https",
                url = "https://api.example.com/v1/data",
                stackTrace = "at com.example.MyClass.method(MyClass.kt:42)",
            )
        val exception =
            NetworkRequestAttemptedException(
                "Network request blocked",
                requestDetails = details,
            )

        val message = exception.message!!
        assertTrue(message.contains("api.example.com"))
        assertTrue(message.contains("443"))
        assertTrue(message.contains("https"))
        assertTrue(message.contains("https://api.example.com/v1/data"))
        assertTrue(message.contains("at com.example.MyClass.method"))
    }

    @Test
    fun `exception should preserve cause when provided`() {
        val cause = RuntimeException("Original error")
        val exception =
            NetworkRequestAttemptedException(
                "Network request blocked",
                cause = cause,
            )

        assertEquals(cause, exception.cause)
    }

    @Test
    fun `NetworkRequestDetails should store all properties correctly`() {
        val details =
            NetworkRequestDetails(
                host = "example.com",
                port = 8080,
                protocol = "http",
                url = "http://example.com:8080/path",
                stackTrace = "stack trace here",
            )

        assertEquals("example.com", details.host)
        assertEquals(8080, details.port)
        assertEquals("http", details.protocol)
        assertEquals("http://example.com:8080/path", details.url)
        assertEquals("stack trace here", details.stackTrace)
    }
}
