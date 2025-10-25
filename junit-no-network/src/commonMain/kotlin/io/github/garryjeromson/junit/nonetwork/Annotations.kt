package io.github.garryjeromson.junit.nonetwork

/**
 * Annotation to mark a test that should have network requests blocked.
 * When applied to a test method or class, all network requests will fail with
 * [NetworkRequestAttemptedException] unless specifically allowed.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoNetworkTest

/**
 * Annotation to specify hosts that are allowed to be accessed during a test.
 * This can be combined with [NoNetworkTest] to allow specific hosts while blocking others.
 *
 * @param hosts Array of host names or patterns that are allowed. Supports wildcards (e.g., "*.example.com").
 * Use "*" to allow all hosts (useful in combination with [BlockedHosts]).
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AllowedHosts(
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
annotation class BlockedHosts(
    val hosts: Array<String>,
)

/**
 * Annotation to apply network blocking to all tests in a class by default.
 * When applied to a test class, all test methods will have network requests blocked
 * unless the test method is annotated with [AllowNetwork].
 *
 * This is equivalent to adding [NoNetworkTest] to every test method in the class.
 *
 * Example:
 * ```
 * @ExtendWith(NoNetworkExtension::class)
 * @NoNetworkByDefault
 * class MyTest {
 *     @Test
 *     fun test1() {
 *         // Network is blocked
 *     }
 *
 *     @Test
 *     @AllowNetwork
 *     fun test2() {
 *         // Network is allowed (opt-out)
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoNetworkByDefault

/**
 * Annotation to explicitly allow network access for a test.
 * This is useful when network blocking is enabled by default (via [NoNetworkByDefault],
 * system property, or constructor parameter) but you want to opt-out for specific tests.
 *
 * [AllowNetwork] takes precedence over all other network blocking configurations.
 *
 * Example:
 * ```
 * @ExtendWith(NoNetworkExtension::class)
 * @NoNetworkByDefault
 * class MyTest {
 *     @Test
 *     @AllowNetwork
 *     fun `can make real network requests`() {
 *         // Network requests are allowed here
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AllowNetwork
