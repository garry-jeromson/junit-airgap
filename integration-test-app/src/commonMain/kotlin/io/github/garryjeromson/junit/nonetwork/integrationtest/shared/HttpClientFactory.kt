package io.github.garryjeromson.junit.nonetwork.integrationtest.shared

import io.ktor.client.HttpClient

/**
 * Platform-specific factory for creating configured HttpClient instances.
 * Each platform provides its own engine implementation:
 * - JVM: CIO engine
 * - Android: OkHttp engine
 * - iOS: Darwin engine
 */
expect object HttpClientFactory {
    /**
     * Creates a platform-specific HttpClient instance with appropriate engine.
     */
    fun create(): HttpClient
}
