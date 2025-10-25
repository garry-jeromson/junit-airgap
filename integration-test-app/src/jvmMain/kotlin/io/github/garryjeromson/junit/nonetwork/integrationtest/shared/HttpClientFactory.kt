package io.github.garryjeromson.junit.nonetwork.integrationtest.shared

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/**
 * JVM implementation using CIO engine.
 * CIO is a Kotlin coroutine-based engine that works well on JVM.
 */
actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(CIO) {
            // Engine configuration can go here
            engine {
                // CIO-specific configuration
            }
        }
    }
}
