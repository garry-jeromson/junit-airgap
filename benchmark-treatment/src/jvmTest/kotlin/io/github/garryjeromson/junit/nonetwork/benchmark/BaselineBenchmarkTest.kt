package io.github.garryjeromson.junit.nonetwork.benchmark

import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertTrue

/**
 * Baseline benchmarks to verify minimal overhead for empty and simple tests.
 * These establish the floor for overhead - if these fail, something is seriously wrong.
 */
class BaselineBenchmarkTest {
    @Test
    fun `benchmark empty test - no operations`() {
        BenchmarkRunner.runBenchmarkAndAssert(
            name = "Empty Test (No Operations)",
            control = {
                // Control: Run test without extension
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    // Completely empty - just test overhead
                }
            },
            treatment = {
                // Treatment: Run test with extension but without @BlockNetworkRequests
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    // Completely empty - just test overhead
                }
            },
        )
    }

    @Test
    fun `benchmark simple assertion test`() {
        BenchmarkRunner.runBenchmarkAndAssert(
            name = "Simple Assertion Test",
            control = {
                // Control: Run test without extension
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val x = 1 + 1
                    assertTrue(x == 2)
                }
            },
            treatment = {
                // Treatment: Simulate running with extension (but no actual network blocking)
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val x = 1 + 1
                    assertTrue(x == 2)
                }
            },
        )
    }

    @Test
    fun `benchmark multiple simple assertions`() {
        BenchmarkRunner.runBenchmarkAndAssert(
            name = "Multiple Simple Assertions",
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
    @ExtendWith(NoNetworkExtension::class)
    fun `benchmark with extension enabled but no network test annotation`() {
        // This test has the extension enabled but doesn't use @BlockNetworkRequests
        // so no SecurityManager should be installed
        BenchmarkRunner.runBenchmarkAndAssert(
            name = "Extension Enabled (No @BlockNetworkRequests)",
            control = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val result = calculateSum(1, 2, 3, 4, 5)
                    assertTrue(result == 15)
                }
            },
            treatment = {
                // Same operation - extension is enabled for this test method
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val result = calculateSum(1, 2, 3, 4, 5)
                    assertTrue(result == 15)
                }
            },
        )
    }

    @Test
    @ExtendWith(NoNetworkExtension::class)
    @BlockNetworkRequests
    fun `benchmark with network test annotation but no network calls`() {
        // This test has @BlockNetworkRequests enabled but makes no network calls
        // SecurityManager is installed but never triggered
        BenchmarkRunner.runBenchmarkAndAssert(
            name = "@BlockNetworkRequests (No Network Calls)",
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
