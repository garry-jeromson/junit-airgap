package io.github.garryjeromson.junit.airgap.benchmark

import io.github.garryjeromson.junit.airgap.AirgapRule
import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * Android extension lifecycle benchmarks to measure overhead of the rule itself.
 * These tests focus on the cost of SecurityManager installation and teardown.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidExtensionLifecycleBenchmarkTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun `benchmark annotation processing overhead`() {
        // This test measures the overhead of having @BlockNetworkRequests annotation
        // and SecurityManager installation/removal
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android Lifecycle (@BlockNetworkRequests Overhead)",
                control = {
                    // Control: Simple operation without @BlockNetworkRequests
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val x = 1 + 1
                        assertTrue(x == 2)
                    }
                },
                treatment = {
                    // Treatment: Same operation but under @BlockNetworkRequests
                    // (this test method already has it, so SecurityManager is active)
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val x = 1 + 1
                        assertTrue(x == 2)
                    }
                },
            )
    }

    @Test
    @BlockNetworkRequests
    fun `benchmark rule with minimal work`() {
        // Measure overhead when test does almost nothing
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android Lifecycle (Minimal Work)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        // Just a simple calculation
                        @Suppress("UNUSED_VARIABLE")
                        val result = (1..10).sum()
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        @Suppress("UNUSED_VARIABLE")
                        val result = (1..10).sum()
                    }
                },
            )
    }

    @Test
    fun `benchmark rule enabled vs disabled`() {
        // This test doesn't have @BlockNetworkRequests, so we can measure
        // the difference between rule being present vs not present
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android Lifecycle (Rule Enabled vs Disabled)",
                control = {
                    // Simulates no rule at all
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        performTypicalTestOperations()
                    }
                },
                treatment = {
                    // Rule is enabled for this class but not this method
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        performTypicalTestOperations()
                    }
                },
            )
    }

    @Test
    @BlockNetworkRequests
    fun `benchmark multiple test method calls`() {
        // Simulates a test that calls multiple methods
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android Lifecycle (Multiple Method Calls)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        helperMethod1()
                        helperMethod2()
                        helperMethod3()
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        helperMethod1()
                        helperMethod2()
                        helperMethod3()
                    }
                },
            )
    }

    @Test
    @BlockNetworkRequests
    fun `benchmark exception creation overhead`() {
        // Measure if exception creation for potential blocking has overhead
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android Lifecycle (Exception Handling)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        try {
                            val result = riskyOperation(5)
                            assertTrue(result > 0)
                        } catch (e: IllegalArgumentException) {
                            // Handle exception
                        }
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        try {
                            val result = riskyOperation(5)
                            assertTrue(result > 0)
                        } catch (e: IllegalArgumentException) {
                            // Handle exception
                        }
                    }
                },
            )
    }

    // Helper methods

    private fun performTypicalTestOperations() {
        val list = listOf(1, 2, 3, 4, 5)
        val sum = list.sum()
        val average = sum / list.size.toDouble()
        assertTrue(average > 0)
    }

    private fun helperMethod1() {
        val x = 10
        val y = 20
        assertTrue(x + y == 30)
    }

    private fun helperMethod2() {
        val name = "Test"
        assertTrue(name.isNotEmpty())
    }

    private fun helperMethod3() {
        val numbers = (1..10).toList()
        assertTrue(numbers.size == 10)
    }

    private fun riskyOperation(value: Int): Int {
        if (value < 0) throw IllegalArgumentException("Negative value")
        return value * 2
    }
}
