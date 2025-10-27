package io.github.garryjeromson.junit.airgap

import org.junit.Rule
import org.junit.Test
import java.net.Socket
import kotlin.test.assertFailsWith

class AirgapRuleTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun shouldBlockNetworkRequestsWhenAnnotated() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    @BlockRequestsToHosts(hosts = ["evil.com"])
    fun shouldRespectBlockedHostsAnnotation() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("evil.com", 80)
        }
    }

    @Test
    fun shouldNotBlockWhenAnnotationIsAbsent() {
        // This test doesn't have @BlockNetworkRequests, so blocking should not occur
        // The actual connection may fail, but it should not throw NetworkRequestAttemptedException
        try {
            Socket("example.com", 80)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Should not block without @BlockNetworkRequests")
        } catch (e: Exception) {
            // Other exceptions are fine
        }
    }
}
