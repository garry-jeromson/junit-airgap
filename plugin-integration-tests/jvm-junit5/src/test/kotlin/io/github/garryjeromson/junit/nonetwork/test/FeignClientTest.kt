package io.github.garryjeromson.junit.nonetwork.test

import feign.Feign
import feign.RequestLine
import feign.okhttp.OkHttpClient
import io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
import org.junit.jupiter.api.Test

/**
 * Tests that verify Feign (OpenFeign) HTTP client network blocking works correctly
 * with the plugin auto-configuration (JUnit 5).
 */
class FeignClientTest {
    interface TestApi {
        @RequestLine("GET /")
        fun getData(): String
    }

    private fun makeFeignRequest(): String {
        val client =
            Feign
                .builder()
                .client(OkHttpClient())
                .target(TestApi::class.java, "https://example.com")

        return client.getData()
    }

    @Test
    @BlockNetworkRequests
    fun feignIsBlockedWithNoNetworkTest() {
        assertRequestBlocked {
            makeFeignRequest()
        }
    }

    @Test
    @AllowNetworkRequests
    fun feignIsAllowedWithAllowNetwork() {
        assertRequestAllowed {
            makeFeignRequest()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `feign with spaces in test name is blocked`() {
        assertRequestBlocked {
            makeFeignRequest()
        }
    }
}
