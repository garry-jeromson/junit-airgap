package io.github.garryjeromson.junit.airgap.benchmark

import io.github.garryjeromson.junit.airgap.BlockNetworkRequests
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * CPU-intensive benchmarks for treatment group (with plugin and JVMTI agent).
 * These tests measure performance with network blocking enabled for CPU-heavy operations.
 */
@ExtendWith(BenchmarkResultsCollector::class)
@BlockNetworkRequests
class CpuIntensiveBenchmarkTest {
    @Test
    fun `benchmark fibonacci calculation`() {
        val (medianNs, stdDevNs) =
            BenchmarkRunner.measureOperation(
                name = "CPU-Intensive (Fibonacci)",
            ) {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    fibonacci(20) // Calculate 20th Fibonacci number
                }
            }

        BenchmarkResultsCollector.addResult(
            SingleBenchmarkResult(
                name = "CPU-Intensive (Fibonacci)",
                medianNs = medianNs,
                stdDevNs = stdDevNs,
            ),
        )
    }

    @Test
    fun `benchmark prime number generation`() {
        val (medianNs, stdDevNs) =
            BenchmarkRunner.measureOperation(
                name = "CPU-Intensive (Prime Numbers)",
            ) {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    generatePrimes(100) // Generate first 100 primes
                }
            }

        BenchmarkResultsCollector.addResult(
            SingleBenchmarkResult(
                name = "CPU-Intensive (Prime Numbers)",
                medianNs = medianNs,
                stdDevNs = stdDevNs,
            ),
        )
    }

    @Test
    fun `benchmark array sorting`() {
        val (medianNs, stdDevNs) =
            BenchmarkRunner.measureOperation(
                name = "CPU-Intensive (Array Sorting)",
            ) {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val array = (1..1000).shuffled().toIntArray()
                    array.sort()
                }
            }

        BenchmarkResultsCollector.addResult(
            SingleBenchmarkResult(
                name = "CPU-Intensive (Array Sorting)",
                medianNs = medianNs,
                stdDevNs = stdDevNs,
            ),
        )
    }

    @Test
    fun `benchmark string manipulation`() {
        val (medianNs, stdDevNs) =
            BenchmarkRunner.measureOperation(
                name = "CPU-Intensive (String Operations)",
            ) {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    var result = "hello"
                    repeat(100) {
                        result = result.uppercase().lowercase()
                        result = result.replace("l", "L")
                    }
                }
            }

        BenchmarkResultsCollector.addResult(
            SingleBenchmarkResult(
                name = "CPU-Intensive (String Operations)",
                medianNs = medianNs,
                stdDevNs = stdDevNs,
            ),
        )
    }

    @Test
    fun `benchmark regex operations`() {
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val text = "Contact us at support@example.com or sales@example.org for more info"

        val (medianNs, stdDevNs) =
            BenchmarkRunner.measureOperation(
                name = "CPU-Intensive (Regex Matching)",
            ) {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    emailPattern.findAll(text).toList()
                }
            }

        BenchmarkResultsCollector.addResult(
            SingleBenchmarkResult(
                name = "CPU-Intensive (Regex Matching)",
                medianNs = medianNs,
                stdDevNs = stdDevNs,
            ),
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
