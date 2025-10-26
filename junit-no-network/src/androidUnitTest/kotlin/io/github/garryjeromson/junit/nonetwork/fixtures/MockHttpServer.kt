package io.github.garryjeromson.junit.nonetwork.integration.fixtures

import fi.iki.elonen.NanoHTTPD

/**
 * Simple mock HTTP server for integration testing.
 * Runs on localhost at a random available port (or configurable port if specified).
 * Use [listeningPort] to get the actual port number after calling [start].
 */
class MockHttpServer(
    port: Int = 0,
) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return when {
            uri == "/api/test" && method == Method.GET -> {
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"status": "success", "message": "Test endpoint"}""",
                )
            }
            uri == "/api/echo" && method == Method.POST -> {
                newFixedLengthResponse(
                    Response.Status.OK,
                    "text/plain",
                    "Echo received",
                )
            }
            else -> {
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "text/plain",
                    "Not found: $uri",
                )
            }
        }
    }

    companion object {
        const val DEFAULT_PORT = 0 // 0 = random available port
    }
}
