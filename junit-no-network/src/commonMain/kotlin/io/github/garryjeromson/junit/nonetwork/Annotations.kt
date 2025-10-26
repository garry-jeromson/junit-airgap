package io.github.garryjeromson.junit.nonetwork

/**
 * Annotation to mark a test or class that should have network requests blocked.
 *
 * When applied to a **method**, network requests are blocked for that specific test.
 * When applied to a **class**, network requests are blocked for all tests by default
 * (tests can opt-out using [AllowNetworkRequests]).
 *
 * All network requests will fail with [NetworkRequestAttemptedException] unless
 * specifically allowed.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BlockNetworkRequests

/**
 * Annotation to specify hosts that are allowed to be accessed during a test.
 * This can be combined with [BlockNetworkRequests] to allow specific hosts while blocking others.
 *
 * @param hosts Array of host names or patterns that are allowed. Supports wildcards (e.g., "*.example.com").
 * Use "*" to allow all hosts (useful in combination with [BlockRequestsToHosts]).
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AllowRequestsToHosts(
    val hosts: Array<String>,
)

/**
 * Annotation to specify hosts that should be blocked during a test.
 * Blocked hosts take precedence over allowed hosts.
 *
 * @param hosts Array of host names or patterns that should be blocked. Supports wildcards (e.g., "*.evil.com").
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BlockRequestsToHosts(
    val hosts: Array<String>,
)

/**
 * Annotation to explicitly allow network access for a test.
 * This is useful when network blocking is enabled by default (via class-level [BlockNetworkRequests],
 * system property, or constructor parameter) but you want to opt-out for specific tests.
 *
 * [AllowNetworkRequests] takes precedence over all other network blocking configurations.
 *
 * Example:
 * ```
 * @ExtendWith(NoNetworkExtension::class)
 * @BlockNetworkRequests  // Applied at class level - blocks all tests by default
 * class MyTest {
 *     @Test
 *     @AllowNetworkRequests
 *     fun `can make real network requests`() {
 *         // Network requests are allowed here
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AllowNetworkRequests

// ============================================================================
// Deprecated: Type aliases for backward compatibility
// ============================================================================

/**
 * @deprecated Use [BlockNetworkRequests] instead. This annotation has been renamed for better clarity.
 */
@Deprecated(
    message = "Use @BlockNetworkRequests instead",
    replaceWith = ReplaceWith("BlockNetworkRequests", "io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests"),
)
typealias NoNetworkTest = BlockNetworkRequests

/**
 * @deprecated Use class-level [BlockNetworkRequests] instead. This annotation has been merged into @BlockNetworkRequests.
 */
@Deprecated(
    message = "Use class-level @BlockNetworkRequests instead",
    replaceWith = ReplaceWith("BlockNetworkRequests", "io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests"),
)
typealias NoNetworkByDefault = BlockNetworkRequests

/**
 * @deprecated Use [AllowRequestsToHosts] instead. This annotation has been renamed for better clarity.
 */
@Deprecated(
    message = "Use @AllowRequestsToHosts instead",
    replaceWith = ReplaceWith("AllowRequestsToHosts", "io.github.garryjeromson.junit.nonetwork.AllowRequestsToHosts"),
)
typealias AllowedHosts = AllowRequestsToHosts

/**
 * @deprecated Use [BlockRequestsToHosts] instead. This annotation has been renamed for better clarity.
 */
@Deprecated(
    message = "Use @BlockRequestsToHosts instead",
    replaceWith = ReplaceWith("BlockRequestsToHosts", "io.github.garryjeromson.junit.nonetwork.BlockRequestsToHosts"),
)
typealias BlockedHosts = BlockRequestsToHosts

/**
 * @deprecated Use [AllowNetworkRequests] instead. This annotation has been renamed for better clarity.
 */
@Deprecated(
    message = "Use @AllowNetworkRequests instead",
    replaceWith = ReplaceWith("AllowNetworkRequests", "io.github.garryjeromson.junit.nonetwork.AllowNetworkRequests"),
)
typealias AllowNetwork = AllowNetworkRequests
