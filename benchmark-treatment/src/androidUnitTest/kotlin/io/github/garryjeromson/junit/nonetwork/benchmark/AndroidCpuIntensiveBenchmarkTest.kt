package io.github.garryjeromson.junit.nonetwork.benchmark

import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.BlockNetworkRequests
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Android CPU-intensive benchmarks to verify no overhead for computation-heavy tests.
 * These tests perform significant CPU work to ensure SecurityManager overhead
 * is negligible compared to actual test work.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidCpuIntensiveBenchmarkTest {
    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @BlockNetworkRequests
    fun `benchmark fibonacci calculation`() {
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android CPU-Intensive (Fibonacci)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        fibonacci(20) // Calculate 20th Fibonacci number
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        fibonacci(20)
                    }
                },
            )
    }

    @Test
    @BlockNetworkRequests
    fun `benchmark prime number generation`() {
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android CPU-Intensive (Prime Numbers)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        generatePrimes(100) // Generate first 100 primes
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        generatePrimes(100)
                    }
                },
            )
    }

    @Test
    @BlockNetworkRequests
    fun `benchmark array sorting`() {
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android CPU-Intensive (Array Sorting)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val array = (1..1000).shuffled().toIntArray()
                        array.sort()
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val array = (1..1000).shuffled().toIntArray()
                        array.sort()
                    }
                },
            )
    }

    @Test
    @BlockNetworkRequests
    fun `benchmark string manipulation`() {
        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android CPU-Intensive (String Operations)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        var result = "hello"
                        repeat(100) {
                            result = result.uppercase().lowercase()
                            result = result.replace("l", "L")
                        }
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        var result = "hello"
                        repeat(100) {
                            result = result.uppercase().lowercase()
                            result = result.replace("l", "L")
                        }
                    }
                },
            )
    }

    @Test
    @BlockNetworkRequests
    fun `benchmark regex operations`() {
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val text = "Contact us at support@example.com or sales@example.org for more info"

        val result =
            BenchmarkRunner.runBenchmark(
                name = "Android CPU-Intensive (Regex Matching)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        emailPattern.findAll(text).toList()
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        emailPattern.findAll(text).toList()
                    }
                },
            )
    }

    // Helper functions

    private fun fibonacci(n: Int): Long {
        if (n <= 1) return n.toLong()
        var a = 0L
        var b = 1L
        repeat(n - 1) {
            val temp = a + b
            a = b
            b = temp
        }
        return b
    }

    private fun generatePrimes(count: Int): List<Int> {
        val primes = mutableListOf<Int>()
        var candidate = 2
        while (primes.size < count) {
            if (isPrime(candidate)) {
                primes.add(candidate)
            }
            candidate++
        }
        return primes
    }

    private fun isPrime(n: Int): Boolean {
        if (n < 2) return false
        if (n == 2) return true
        if (n % 2 == 0) return false
        val sqrt = kotlin.math.sqrt(n.toDouble()).toInt()
        for (i in 3..sqrt step 2) {
            if (n % i == 0) return false
        }
        return true
    }
}
