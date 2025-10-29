package io.github.garryjeromson.junit.airgap

import airgap.AirgapURLProtocol
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Configures the Ktor Darwin HTTP client to use Airgap network blocking.
 *
 * This extension adds the AirgapURLProtocol to the NSURLSession's protocolClasses array,
 * enabling network request interception and blocking. The actual blocking behavior is
 * controlled by the NetworkBlocker which must be installed separately.
 *
 * Example usage:
 * ```kotlin
 * val config = NetworkConfiguration(
 *     allowedHosts = setOf("api.example.com"),
 *     blockedHosts = emptySet()
 * )
 *
 * // Install the blocker
 * val blocker = NetworkBlocker(config)
 * blocker.install()
 *
 * try {
 *     // Create client with Airgap support
 *     val client = HttpClient(Darwin) {
 *         installAirgap()
 *     }
 *     // Use client...
 * } finally {
 *     blocker.uninstall()
 * }
 * ```
 *
 * **Important Notes**:
 * - You must call `NetworkBlocker(config).install()` before creating the client
 * - Remember to call `blocker.uninstall()` when done to clean up
 * - This must be called during client creation as it configures the underlying NSURLSession
 *
 * @receiver HttpClientConfig<DarwinClientEngineConfig> The Ktor client configuration DSL.
 *
 * @see NetworkBlocker
 * @see createAirgapHttpClient for a convenience function that handles installation
 */
@OptIn(ExperimentalForeignApi::class)
fun HttpClientConfig<DarwinClientEngineConfig>.installAirgap() {
    // Configure the Darwin engine to use our custom URLProtocol
    engine {
        configureSession {
            // Add AirgapURLProtocol to the session's protocol classes
            // This is required for NSURLSession to use our custom protocol
            setProtocolClasses(listOf(AirgapURLProtocol))
        }
    }
}

/**
 * Result of creating an Airgap-enabled HTTP client.
 *
 * Contains both the client and the blocker to allow proper cleanup.
 */
data class AirgapHttpClient(
    /**
     * The configured HttpClient with Airgap support.
     */
    val client: io.ktor.client.HttpClient,

    /**
     * The NetworkBlocker instance. Call `blocker.uninstall()` when done.
     */
    val blocker: NetworkBlocker
)

/**
 * Creates a pre-configured Ktor HTTP client with Airgap network blocking enabled.
 *
 * This is a convenience function that:
 * 1. Creates and installs a NetworkBlocker
 * 2. Creates an HttpClient with the Darwin engine configured for Airgap
 * 3. Returns both so you can properly clean up when done
 *
 * Example usage:
 * ```kotlin
 * val config = NetworkConfiguration(
 *     allowedHosts = setOf("api.example.com"),
 *     blockedHosts = emptySet()
 * )
 *
 * val (client, blocker) = createAirgapHttpClient(config)
 * try {
 *     val response = client.get("https://api.example.com/data")
 *     // Process response...
 * } finally {
 *     client.close()
 *     blocker.uninstall()
 * }
 * ```
 *
 * @param configuration The network blocking configuration specifying allowed/blocked hosts.
 * @param block Optional additional client configuration.
 * @return An AirgapHttpClient containing the client and blocker.
 *
 * @see NetworkConfiguration
 * @see installAirgap
 * @see AirgapHttpClient
 */
@OptIn(ExperimentalForeignApi::class)
fun createAirgapHttpClient(
    configuration: NetworkConfiguration,
    block: HttpClientConfig<DarwinClientEngineConfig>.() -> Unit = {}
): AirgapHttpClient {
    // Create and install the blocker
    val blocker = NetworkBlocker(configuration)
    blocker.install()

    // Create the client with Airgap support
    val client = io.ktor.client.HttpClient(io.ktor.client.engine.darwin.Darwin) {
        installAirgap()
        block()
    }

    return AirgapHttpClient(client, blocker)
}
