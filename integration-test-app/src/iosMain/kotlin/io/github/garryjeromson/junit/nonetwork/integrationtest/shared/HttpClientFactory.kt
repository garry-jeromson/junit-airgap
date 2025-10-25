package io.github.garryjeromson.junit.nonetwork.integrationtest.shared

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS implementation using Darwin engine.
 * Darwin engine is built on top of NSURLSession, the native iOS networking API.
 *
 * Note: Network blocking is not actively enforced on iOS in this library
 * (API structure only), but this demonstrates the proper KMP pattern.
 */
actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(Darwin) {
            // Engine configuration can go here
            engine {
                // Darwin-specific configuration
            }
        }
    }
}
