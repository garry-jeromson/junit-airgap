package io.github.garryjeromson.junit.nonetwork.integration

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.coroutines.awaitString
import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.MockHttpServer
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkBlocked
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.assertNetworkNotBlocked
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Integration tests verifying that Fuel HTTP client is properly blocked.
 * Fuel is a popular Kotlin-first HTTP client with idiomatic DSL and coroutines support.
 */
@ExtendWith(NoNetworkExtension::class)
class FuelClientIntegrationTest {
    companion object {
        private lateinit var mockServer: MockHttpServer

        @JvmStatic
        @BeforeAll
        fun startMockServer() {
            mockServer = MockHttpServer()
            mockServer.start()
            Thread.sleep(100)
        }

        @JvmStatic
        @AfterAll
        fun stopMockServer() {
            mockServer.stop()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Fuel GET requests to external host`() {
        assertNetworkBlocked("Fuel should be blocked") {
            val (_, _, result) = Fuel.get("http://example.com/api/test").responseString()
            // Access result to trigger actual network call
            result.get()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Fuel POST requests to external host`() {
        assertNetworkBlocked("Fuel POST should be blocked") {
            val (_, _, result) =
                Fuel.post("https://api.example.com/api/submit")
                    .body("test data")
                    .responseString()
            // Access result to trigger actual network call
            result.get()
        }
    }

    @Test
    @BlockNetworkRequests
    fun `blocks Fuel coroutine requests to external host`() {
        assertNetworkBlocked("Fuel coroutines should be blocked") {
            runBlocking {
                Fuel.get("http://example.com/api/test").awaitString()
            }
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `allows Fuel to localhost`() {
        assertNetworkNotBlocked("Fuel to localhost should work") {
            val (_, _, result) = Fuel.get("http://localhost:${mockServer.listeningPort}/api/test").responseString()
            // Verify response was received
            result.get()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost"])
    fun `allows Fuel coroutines to localhost`() {
        assertNetworkNotBlocked("Fuel coroutines to localhost should work") {
            runBlocking {
                val response = Fuel.get("http://localhost:${mockServer.listeningPort}/api/test").awaitString()
                response
            }
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1"])
    fun `allows Fuel to allowed IP address`() {
        assertNetworkNotBlocked("Fuel to 127.0.0.1 should work") {
            val (_, _, result) = Fuel.get("http://127.0.0.1:${mockServer.listeningPort}/api/test").responseString()
            result.get()
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    fun `allows Fuel with wildcard configuration`() {
        assertNetworkNotBlocked("Fuel should work with wildcard") {
            runBlocking {
                val response = Fuel.get("http://localhost:${mockServer.listeningPort}/api/test").awaitString()
                response
            }
        }
    }
}
