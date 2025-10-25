package io.github.garryjeromson.junit.nonetwork

import kotlin.test.Test

/**
 * Test written in commonTest to verify bytecode enhancement works
 * for tests executed on Android and JVM platforms.
 *
 * This test deliberately has NO manual @Rule setup to verify
 * that JUnit4RuleInjectionTask properly injects the NoNetworkRule field.
 *
 * Key validation points:
 * 1. Test is defined in commonTest (not platform-specific)
 * 2. Test uses @NoNetworkTest annotation
 * 3. NO @Rule field is manually defined
 * 4. Bytecode enhancement should inject the rule during compilation
 * 5. Network blocking should work when executed on JVM/Android
 *
 * Note: This test uses expect/actual pattern for platform-specific network operations.
 * The actual network test implementation is in platform-specific source sets.
 */
class CommonTestJUnit4NetworkTest {
    // NO @Rule field here!
    // Should be injected by JUnit4RuleInjectionTask during compilation

    @Test
    @NoNetworkTest
    fun `commonTest should block network via bytecode-injected rule`() {
        // This test will be implemented with actual platform-specific code
        // in jvmTest and androidUnitTest source sets using expect/actual pattern
        performNetworkBlockingTest()
    }

    @Test
    fun `commonTest without NoNetworkTest should not block network`() {
        // Without @NoNetworkTest annotation, network should NOT be blocked
        // This verifies the rule is correctly scoped
        performNonBlockingNetworkTest()
    }
}

/**
 * Expected function to perform network blocking test.
 * Actual implementation will be in platform-specific source sets.
 */
expect fun performNetworkBlockingTest()

/**
 * Expected function to verify network is not blocked without annotation.
 * Actual implementation will be in platform-specific source sets.
 */
expect fun performNonBlockingNetworkTest()
