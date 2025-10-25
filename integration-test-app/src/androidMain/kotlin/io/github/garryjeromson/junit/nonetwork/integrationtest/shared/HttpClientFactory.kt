package io.github.garryjeromson.junit.nonetwork.integrationtest.shared

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Android implementation using OkHttp engine.
 * OkHttp is the recommended engine for Android as it integrates
 * well with the Android platform and is widely used.
 */
actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(OkHttp) {
            // Engine configuration can go here
            engine {
                // OkHttp-specific configuration
            }
        }
    }
}
