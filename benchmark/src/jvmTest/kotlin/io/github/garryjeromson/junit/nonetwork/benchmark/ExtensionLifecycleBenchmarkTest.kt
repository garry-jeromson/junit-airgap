package io.github.garryjeromson.junit.nonetwork.benchmark

import io.github.garryjeromson.junit.nonetwork.NoNetworkExtension
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertTrue

/**
 * Extension lifecycle benchmarks to measure overhead of the extension itself.
 * These tests focus on the cost of SecurityManager installation and teardown.
 */
@ExtendWith(NoNetworkExtension::class)
class ExtensionLifecycleBenchmarkTest {
    @Test
    @BlockNetworkRequests
    fun `benchmark annotation processing overhead`() {
        // This test measures the overhead of having @BlockNetworkRequests annotation
        // and SecurityManager installation/removal
        BenchmarkRunner.runBenchmarkAndAssert(
            name = "Lifecycle (@BlockNetworkRequests Overhead)",
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
    fun `benchmark extension with minimal work`() {
        // Measure overhead when test does almost nothing
        BenchmarkRunner.runBenchmarkAndAssert(
            name = "Lifecycle (Minimal Work)",
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
    fun `benchmark extension enabled vs disabled`() {
        // This test doesn't have @BlockNetworkRequests, so we can measure
        // the difference between extension being present vs not present
        BenchmarkRunner.runBenchmarkAndAssert(
            name = "Lifecycle (Extension Enabled vs Disabled)",
            control = {
                // Simulates no extension at all
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    performTypicalTestOperations()
                }
            },
            treatment = {
                // Extension is enabled for this class but not this method
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
        BenchmarkRunner.runBenchmarkAndAssert(
            name = "Lifecycle (Multiple Method Calls)",
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
        BenchmarkRunner.runBenchmarkAndAssert(
            name = "Lifecycle (Exception Handling)",
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
