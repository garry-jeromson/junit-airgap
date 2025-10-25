package io.github.garryjeromson.junit.nonetwork.integration.fixtures

import fi.iki.elonen.NanoHTTPD

/**
 * Simple mock HTTP server for integration testing.
 * Runs on localhost at a configurable port.
 */
class MockHttpServer(
    port: Int = 8089,
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
        const val DEFAULT_PORT = 8089
    }
}
