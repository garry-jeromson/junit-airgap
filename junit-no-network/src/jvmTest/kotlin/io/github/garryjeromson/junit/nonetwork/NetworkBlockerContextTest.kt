package io.github.garryjeromson.junit.nonetwork

import io.github.garryjeromson.junit.nonetwork.bytebuddy.NetworkBlockerContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for NetworkBlockerContext functionality.
 */
class NetworkBlockerContextTest {
    @AfterEach
    fun cleanup() {
        // Clear configuration after each test
        NetworkBlockerContext.clearConfiguration()
    }

    @Test
    fun `checkConnection includes caller in exception message`() {
        // Set up configuration that blocks all hosts
        val configuration = NetworkConfiguration(
            allowedHosts = emptySet(),
            blockedHosts = emptySet(),
        )
        NetworkBlockerContext.setConfiguration(configuration)

        // Attempt connection with custom caller string
        val exception = assertThrows<NetworkRequestAttemptedException> {
            NetworkBlockerContext.checkConnection("example.com", 443, "test-caller")
        }

        // Verify caller appears in exception message
        assertTrue(exception.message!!.contains("via test-caller")) {
            "Exception message should contain 'via test-caller', but was: ${exception.message}"
        }
    }

    @Test
    fun `checkConnection uses default caller when not specified`() {
        // Set up configuration that blocks all hosts
        val configuration = NetworkConfiguration(
            allowedHosts = emptySet(),
            blockedHosts = emptySet(),
        )
        NetworkBlockerContext.setConfiguration(configuration)

        // Attempt connection without specifying caller (uses default)
        val exception = assertThrows<NetworkRequestAttemptedException> {
            NetworkBlockerContext.checkConnection("example.com", 443)
        }

        // Verify default caller appears in exception message
        assertTrue(exception.message!!.contains("via unknown")) {
            "Exception message should contain 'via unknown', but was: ${exception.message}"
        }
    }

    @Test
    fun `checkConnection includes JVMTI-Agent caller in exception message`() {
        // Set up configuration that blocks all hosts
        val configuration = NetworkConfiguration(
            allowedHosts = emptySet(),
            blockedHosts = emptySet(),
        )
        NetworkBlockerContext.setConfiguration(configuration)

        // Attempt connection with JVMTI-Agent caller string (as used by native code)
        val exception = assertThrows<NetworkRequestAttemptedException> {
            NetworkBlockerContext.checkConnection("example.com", 443, "JVMTI-Agent")
        }

        // Verify JVMTI-Agent caller appears in exception message
        assertTrue(exception.message!!.contains("via JVMTI-Agent")) {
            "Exception message should contain 'via JVMTI-Agent', but was: ${exception.message}"
        }
    }

    @Test
    fun `checkConnection includes JVMTI-DNS caller in exception message`() {
        // Set up configuration that blocks all hosts
        val configuration = NetworkConfiguration(
            allowedHosts = emptySet(),
            blockedHosts = emptySet(),
        )
        NetworkBlockerContext.setConfiguration(configuration)

        // Attempt DNS lookup with JVMTI-DNS caller string (as used by native DNS interceptor)
        val exception = assertThrows<NetworkRequestAttemptedException> {
            NetworkBlockerContext.checkConnection("example.com", -1, "JVMTI-DNS")
        }

        // Verify JVMTI-DNS caller appears in exception message
        assertTrue(exception.message!!.contains("via JVMTI-DNS")) {
            "Exception message should contain 'via JVMTI-DNS', but was: ${exception.message}"
        }
    }

    @Test
    fun `checkConnection includes host and port in exception message`() {
        // Set up configuration that blocks all hosts
        val configuration = NetworkConfiguration(
            allowedHosts = emptySet(),
            blockedHosts = emptySet(),
        )
        NetworkBlockerContext.setConfiguration(configuration)

        // Attempt connection
        val exception = assertThrows<NetworkRequestAttemptedException> {
            NetworkBlockerContext.checkConnection("blocked.com", 8080, "test-caller")
        }

        // Verify host and port appear in exception message
        assertTrue(exception.message!!.contains("blocked.com:8080")) {
            "Exception message should contain 'blocked.com:8080', but was: ${exception.message}"
        }
    }
}
