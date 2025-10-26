package io.github.garryjeromson.junit.nonetwork.benchmark

import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertTrue

/**
 * Baseline benchmarks for treatment group (with plugin and JVMTI agent).
 * These tests measure performance with network blocking enabled via @BlockNetworkRequests.
 */
@ExtendWith(BenchmarkResultsCollector::class)
@BlockNetworkRequests
class BaselineBenchmarkTest {
    @Test
    fun `benchmark empty test - no operations`() {
        val (medianNs, stdDevNs) = BenchmarkRunner.measureOperation(
            name = "Empty Test (No Operations)",
        ) {
            repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                // Completely empty - just test overhead
            }
        }

        BenchmarkResultsCollector.addResult(
            SingleBenchmarkResult(
                name = "Empty Test (No Operations)",
                medianNs = medianNs,
                stdDevNs = stdDevNs,
            ),
        )
    }

    @Test
    fun `benchmark simple assertion test`() {
        val (medianNs, stdDevNs) = BenchmarkRunner.measureOperation(
            name = "Simple Assertion Test",
        ) {
            repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                val x = 1 + 1
                assertTrue(x == 2)
            }
        }

        BenchmarkResultsCollector.addResult(
            SingleBenchmarkResult(
                name = "Simple Assertion Test",
                medianNs = medianNs,
                stdDevNs = stdDevNs,
            ),
        )
    }

    @Test
    fun `benchmark multiple simple assertions`() {
        val (medianNs, stdDevNs) = BenchmarkRunner.measureOperation(
            name = "Multiple Simple Assertions",
        ) {
            repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                val a = 5
                val b = 10
                assertTrue(a < b)
                assertTrue(a + b == 15)
                assertTrue(b - a == 5)
                assertTrue(a * 2 == b)
            }
        }

        BenchmarkResultsCollector.addResult(
            SingleBenchmarkResult(
                name = "Multiple Simple Assertions",
                medianNs = medianNs,
                stdDevNs = stdDevNs,
            ),
        )
    }

    @Test
    fun `benchmark function calls`() {
        val (medianNs, stdDevNs) = BenchmarkRunner.measureOperation(
            name = "Function Calls",
        ) {
            repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                val result = calculateSum(1, 2, 3, 4, 5)
                assertTrue(result == 15)
            }
        }

        BenchmarkResultsCollector.addResult(
            SingleBenchmarkResult(
                name = "Function Calls",
                medianNs = medianNs,
                stdDevNs = stdDevNs,
            ),
        )
    }

    @Test
    fun `benchmark arithmetic operations`() {
        val (medianNs, stdDevNs) = BenchmarkRunner.measureOperation(
            name = "Arithmetic Operations",
        ) {
            repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                val result = calculateProduct(2, 3, 4)
                assertTrue(result == 24)
            }
        }

        BenchmarkResultsCollector.addResult(
            SingleBenchmarkResult(
                name = "Arithmetic Operations",
                medianNs = medianNs,
                stdDevNs = stdDevNs,
            ),
        )
    }

    private fun calculateSum(vararg numbers: Int): Int = numbers.sum()

    private fun calculateProduct(vararg numbers: Int): Int = numbers.fold(1) { acc, n -> acc * n }
}
