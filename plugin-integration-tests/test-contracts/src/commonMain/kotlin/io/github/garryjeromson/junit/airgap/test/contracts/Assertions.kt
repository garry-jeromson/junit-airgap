package io.github.garryjeromson.junit.airgap.test.contracts

/**
 * Generic assertion helpers for network blocking tests.
 *
 * These functions are client-agnostic - they work with any code that makes network requests:
 * - Socket connections
 * - HTTP clients (Ktor, Retrofit, OkHttp, etc.)
 * - Any other network operation
 *
 * Usage:
 * ```
 * @Test
 * @BlockNetworkRequests
 * fun myTest() {
 *     assertRequestBlocked {
 *         // Any network operation here
 *         ktorClient.get("https://example.com")
 *     }
 * }
 * ```
 */

/**
 * Assert that a network request is blocked.
 * Should throw NetworkRequestAttemptedException (possibly wrapped on some platforms).
 *
 * @param block Lambda containing the network operation to test
 */
expect fun assertRequestBlocked(block: () -> Unit)

/**
 * Assert that a network request is allowed.
 * Should NOT throw NetworkRequestAttemptedException.
 * Other exceptions (IOException, timeouts, DNS failures, etc.) are acceptable.
 *
 * @param block Lambda containing the network operation to test
 */
expect fun assertRequestAllowed(block: () -> Unit)
