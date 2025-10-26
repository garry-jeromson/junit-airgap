package io.github.garryjeromson.junit.nonetwork.benchmark

/**
 * Simple benchmark runner for measuring performance overhead.
 *
 * This runner executes a control group (without the extension) and a treatment group
 * (with the extension enabled) and compares their performance.
 */
object BenchmarkRunner {
    /**
     * Run a benchmark measuring a single operation.
     * Results are returned for later comparison.
     *
     * @param name Name of the benchmark
     * @param operation Function to benchmark
     * @return Timing statistics (median and stddev in nanoseconds)
     */
    fun measureOperation(
        name: String,
        operation: () -> Unit,
    ): Pair<Double, Double> {
        // Warmup phase
        println("Warming up $name...")
        repeat(BenchmarkConfig.WARMUP_ITERATIONS) {
            operation()
        }

        // Measurement phase
        println("Measuring $name...")
        val times = mutableListOf<Double>()
        repeat(BenchmarkConfig.MEASUREMENT_ITERATIONS) {
            val startTime = System.nanoTime()
            operation()
            val endTime = System.nanoTime()
            times.add((endTime - startTime).toDouble())
        }

        // Remove outliers
        val timesFiltered = Statistics.removeOutliers(times)

        // Calculate statistics
        val median = Statistics.median(timesFiltered)
        val stdDev = Statistics.stdDev(timesFiltered)

        println("  Median: ${median / 1_000_000.0}ms (±${stdDev / 1_000_000.0}ms)")

        return Pair(median, stdDev)
    }

    /**
     * Run a benchmark comparing control vs treatment.
     * This is the legacy API for backward compatibility within a single test.
     *
     * @param name Name of the benchmark
     * @param control Function to benchmark without the extension
     * @param treatment Function to benchmark with the extension
     * @return BenchmarkResult containing timing statistics
     */
    fun runBenchmark(
        name: String,
        control: () -> Unit,
        treatment: () -> Unit,
    ): BenchmarkResult {
        val (controlMedian, controlStdDev) = measureOperation("$name (control)", control)
        val (treatmentMedian, treatmentStdDev) = measureOperation("$name (treatment)", treatment)

        return BenchmarkResult(
            name = name,
            controlMedianNs = controlMedian,
            treatmentMedianNs = treatmentMedian,
            controlStdDevNs = controlStdDev,
            treatmentStdDevNs = treatmentStdDev,
        )
    }

    /**
     * Run a benchmark and assert it passes the overhead threshold.
     *
     * @param name Name of the benchmark
     * @param control Function to benchmark without the extension
     * @param treatment Function to benchmark with the extension
     * @throws AssertionError if overhead exceeds threshold
     */
    fun runBenchmarkAndAssert(
        name: String,
        control: () -> Unit,
        treatment: () -> Unit,
    ) {
        val result = runBenchmark(name, control, treatment)
        println(result.format())

        if (!result.isWithinThreshold()) {
            throw AssertionError(
                "Benchmark '$name' failed: overhead ${String.format("%.2f", result.overheadPercentage)}% " +
                    "exceeds threshold ${BenchmarkConfig.MAX_OVERHEAD_PERCENTAGE}%",
            )
        }
    }
}
