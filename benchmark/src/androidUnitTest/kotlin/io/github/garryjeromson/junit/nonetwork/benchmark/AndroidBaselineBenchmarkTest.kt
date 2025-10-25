package io.github.garryjeromson.junit.nonetwork.benchmark

import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * Android baseline benchmarks to verify minimal overhead for empty and simple tests.
 *
 * Note: Android benchmarks are INFORMATIONAL ONLY because Robolectric adds significant
 * simulation overhead (10-100%+) that makes strict threshold enforcement unreliable.
 * Results are still useful for relative comparison and detecting regressions.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidBaselineBenchmarkTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    fun `benchmark empty test - no operations`() {
        // Android benchmarks are informational only (Robolectric overhead makes strict thresholds unreliable)
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android Empty Test (No Operations)",
                control = {
                    // Control: Run test without @BlockNetworkRequests
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        // Completely empty - just test overhead
                    }
                },
                treatment = {
                    // Treatment: Run test with rule but without @BlockNetworkRequests
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        // Completely empty - just test overhead
                    }
                },
            )
    }

    @Test
    fun `benchmark simple assertion test`() {
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android Simple Assertion Test",
                control = {
                    // Control: Run test without @BlockNetworkRequests
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val x = 1 + 1
                        assertTrue(x == 2)
                    }
                },
                treatment = {
                    // Treatment: Simulate running with rule (but no network blocking)
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val x = 1 + 1
                        assertTrue(x == 2)
                    }
                },
            )
    }

    @Test
    fun `benchmark multiple simple assertions`() {
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android Multiple Simple Assertions",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val a = 5
                        val b = 10
                        assertTrue(a < b)
                        assertTrue(a + b == 15)
                        assertTrue(b - a == 5)
                        assertTrue(a * 2 == b)
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val a = 5
                        val b = 10
                        assertTrue(a < b)
                        assertTrue(a + b == 15)
                        assertTrue(b - a == 5)
                        assertTrue(a * 2 == b)
                    }
                },
            )
    }

    @Test
    fun `benchmark with rule enabled but no network test annotation`() {
        // This test has the rule enabled but doesn't use @BlockNetworkRequests
        // so no SecurityManager should be installed
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android Rule Enabled (No @BlockNetworkRequests)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val result = calculateSum(1, 2, 3, 4, 5)
                        assertTrue(result == 15)
                    }
                },
                treatment = {
                    // Same operation - rule is enabled for this test method
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val result = calculateSum(1, 2, 3, 4, 5)
                        assertTrue(result == 15)
                    }
                },
            )
    }

    @Test
    @BlockNetworkRequests
    fun `benchmark with network test annotation but no network calls`() {
        // This test has @BlockNetworkRequests enabled but makes no network calls
        // SecurityManager is installed but never triggered
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android @BlockNetworkRequests (No Network Calls)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val result = calculateProduct(2, 3, 4)
                        assertTrue(result == 24)
                    }
                },
                treatment = {
                    // Same operation but running under @BlockNetworkRequests
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val result = calculateProduct(2, 3, 4)
                        assertTrue(result == 24)
                    }
                },
            )
    }

    private fun calculateSum(vararg numbers: Int): Int = numbers.sum()

    private fun calculateProduct(vararg numbers: Int): Int = numbers.fold(1) { acc, n -> acc * n }
}
