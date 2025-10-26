package io.github.garryjeromson.junit.nonetwork.integration

import io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts
import io.github.garryjeromson.junit.nonetwork.NetworkRequestAttemptedException
import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import io.github.garryjeromson.junit.nonetwork.integration.fixtures.MockHttpServer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.java.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Integration tests verifying that Ktor HTTP client is properly blocked.
 * Tests different Ktor engines (CIO, OkHttp, Java) and both sync/async patterns.
 */
@ExtendWith(NoNetworkExtension::class)
class KtorClientIntegrationTest {
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

    // ==================== CIO Engine Tests ====================

    @Test
    @BlockNetworkRequests
    fun `blocks Ktor CIO client to external host`() =
        runBlocking {
            val client = HttpClient(CIO)
            try {
                assertFailsWith<NetworkRequestAttemptedException> {
                    runBlocking { client.get("http://example.com/api") }
                }
            } finally {
                client.close()
            }
        }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `allows Ktor CIO client to localhost`() =
        runBlocking {
            val client = HttpClient(CIO)
            try {
                val response: HttpResponse = client.get("http://localhost:${mockServer.listeningPort}/api/test")
                val body = response.bodyAsText()
                assertTrue(body.isNotEmpty() || response.status.value == 200)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block localhost", e)
            } finally {
                client.close()
            }
        }

    // ==================== OkHttp Engine Tests ====================

    @Test
    @BlockNetworkRequests
    fun `blocks Ktor OkHttp client to external host`() =
        runBlocking {
            val client = HttpClient(OkHttp)
            try {
                assertFailsWith<NetworkRequestAttemptedException> {
                    runBlocking { client.get("http://example.com/api") }
                }
            } finally {
                client.close()
            }
        }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["localhost", "127.0.0.1"])
    fun `allows Ktor OkHttp client to localhost`() =
        runBlocking {
            val client = HttpClient(OkHttp)
            try {
                val response: HttpResponse = client.get("http://localhost:${mockServer.listeningPort}/api/test")
                val body = response.bodyAsText()
                assertTrue(body.isNotEmpty() || response.status.value == 200)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block localhost with OkHttp engine", e)
            } finally {
                client.close()
            }
        }

    // ==================== Java Engine Tests ====================

    @Test
    @BlockNetworkRequests
    fun `blocks Ktor Java client to external host`() =
        runBlocking {
            val client = HttpClient(Java)
            try {
                assertFailsWith<NetworkRequestAttemptedException> {
                    runBlocking { client.get("http://example.com/api") }
                }
            } finally {
                client.close()
            }
        }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["127.0.0.1", "localhost"])
    fun `allows Ktor Java client to 127_0_0_1`() =
        runBlocking {
            val client = HttpClient(Java)
            try {
                val response: HttpResponse = client.get("http://127.0.0.1:${mockServer.listeningPort}/api/test")
                val body = response.bodyAsText()
                assertTrue(body.isNotEmpty() || response.status.value == 200)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block 127.0.0.1 with Java engine", e)
            } finally {
                client.close()
            }
        }

    // ==================== Request Type Tests ====================

    @Test
    @BlockNetworkRequests
    fun `blocks Ktor GET requests`() =
        runBlocking {
            val client = HttpClient(CIO)
            try {
                assertFailsWith<NetworkRequestAttemptedException> {
                    runBlocking { client.get("http://example.com/api") }
                }
            } finally {
                client.close()
            }
        }

    @Test
    @BlockNetworkRequests
    fun `blocks Ktor POST requests`() =
        runBlocking {
            val client = HttpClient(CIO)
            try {
                assertFailsWith<NetworkRequestAttemptedException> {
                    runBlocking {
                        client.post("http://example.com/api") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"test": "data"}""")
                        }
                    }
                }
            } finally {
                client.close()
            }
        }

    // ==================== Async/Coroutine Tests ====================

    @Test
    @BlockNetworkRequests
    fun `blocks async Ktor requests in runBlocking`() =
        runBlocking {
            val client = HttpClient(CIO)
            try {
                assertFailsWith<NetworkRequestAttemptedException> {
                    runBlocking { client.get("http://api.example.com/data") }
                }
            } finally {
                client.close()
            }
        }

    @Test
    @BlockNetworkRequests
    fun `blocks suspended Ktor calls`() =
        runTest {
            assertFailsWith<NetworkRequestAttemptedException> {
                suspendedHttpCall()
            }
        }

    private suspend fun suspendedHttpCall() {
        val client = HttpClient(CIO)
        try {
            client.get("http://example.com/api")
        } finally {
            client.close()
        }
    }

    // ==================== Configuration Tests ====================

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    fun `allows Ktor client with wildcard configuration`() =
        runBlocking {
            val client = HttpClient(CIO)
            try {
                val response: HttpResponse = client.get("http://localhost:${mockServer.listeningPort}/api/test")
                val body = response.bodyAsText()
                assertTrue(body.isNotEmpty() || response.status.value == 200)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block with wildcard config", e)
            } finally {
                client.close()
            }
        }

    @Test
    @BlockNetworkRequests
    fun `blocks Ktor client even when using different engines`() =
        runBlocking {
            // Test that all engines are blocked
            assertFailsWith<NetworkRequestAttemptedException>("CIO engine should be blocked") {
                runBlocking {
                    HttpClient(CIO).use { client ->
                        client.get("http://example.com")
                    }
                }
            }

            assertFailsWith<NetworkRequestAttemptedException>("OkHttp engine should be blocked") {
                runBlocking {
                    HttpClient(OkHttp).use { client ->
                        client.get("http://example.com")
                    }
                }
            }

            assertFailsWith<NetworkRequestAttemptedException>("Java engine should be blocked") {
                runBlocking {
                    HttpClient(Java).use { client ->
                        client.get("http://example.com")
                    }
                }
            }
        }
}
