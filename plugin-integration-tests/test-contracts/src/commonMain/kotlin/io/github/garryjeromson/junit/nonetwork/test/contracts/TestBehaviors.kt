package io.github.garryjeromson.junit.nonetwork.test.contracts

/**
 * Common test behaviors that should be verified across all integration test projects.
 *
 * These behaviors are independent of:
 * - JUnit version (4 or 5)
 * - Platform (JVM, Android)
 * - HTTP client library (Ktor, Retrofit, etc.)
 *
 * Test projects implement these behaviors by:
 * 1. Providing their specific HTTP client request implementation
 * 2. Applying appropriate test annotations (@Test, @BlockNetworkRequests, etc.)
 * 3. Using the shared assertion helpers from this module
 */

/**
 * Behavior: Network requests should be blocked when @BlockNetworkRequests annotation is present.
 *
 * Test implementations should:
 * - Apply @BlockNetworkRequests annotation
 * - Call `assertRequestBlocked { makeYourHttpRequest() }`
 * - Verify NetworkRequestAttemptedException is thrown
 */
interface BlocksNetworkRequests {
    fun networkRequestsAreBlocked()
}

/**
 * Behavior: Network requests should be allowed when @AllowNetworkRequests annotation is present.
 *
 * Test implementations should:
 * - Apply @AllowNetworkRequests annotation
 * - Call `assertRequestAllowed { makeYourHttpRequest() }`
 * - Verify NetworkRequestAttemptedException is NOT thrown (other errors are OK)
 */
interface AllowsNetworkRequests {
    fun networkRequestsAreAllowed()
}

/**
 * Behavior: Test names with spaces should work correctly.
 *
 * This verifies annotation processing handles all test name formats.
 */
interface HandlesSpacesInTestNames {
    fun `test with spaces in name works`()
}

/**
 * Standard test contract combining all common behaviors.
 * Test classes can implement this interface and provide their specific HTTP client logic.
 */
interface StandardNetworkTestBehaviors :
    BlocksNetworkRequests,
    AllowsNetworkRequests,
    HandlesSpacesInTestNames
