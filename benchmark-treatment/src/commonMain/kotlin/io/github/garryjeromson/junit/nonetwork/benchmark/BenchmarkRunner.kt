package io.github.garryjeromson.junit.nonetwork.benchmark

/**
 * Simple benchmark runner for measuring performance overhead.
 *
 * This runner executes a control group (without the extension) and a treatment group
 * (with the extension enabled) and compares their performance.
 */
object BenchmarkRunner {
    /**
     * Run a benchmark comparing control vs treatment.
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
        // Warmup phase
        println("Warming up $name...")
        repeat(BenchmarkConfig.WARMUP_ITERATIONS) {
            control()
            treatment()
        }

        // Measurement phase - Control
        println("Measuring control group for $name...")
        val controlTimes = mutableListOf<Double>()
        repeat(BenchmarkConfig.MEASUREMENT_ITERATIONS) {
            val startTime = System.nanoTime()
            control()
            val endTime = System.nanoTime()
            controlTimes.add((endTime - startTime).toDouble())
        }

        // Measurement phase - Treatment
        println("Measuring treatment group for $name...")
        val treatmentTimes = mutableListOf<Double>()
        repeat(BenchmarkConfig.MEASUREMENT_ITERATIONS) {
            val startTime = System.nanoTime()
            treatment()
            val endTime = System.nanoTime()
            treatmentTimes.add((endTime - startTime).toDouble())
        }

        // Remove outliers
        val controlTimesFiltered = Statistics.removeOutliers(controlTimes)
        val treatmentTimesFiltered = Statistics.removeOutliers(treatmentTimes)

        // Calculate statistics
        val controlMedian = Statistics.median(controlTimesFiltered)
        val treatmentMedian = Statistics.median(treatmentTimesFiltered)
        val controlStdDev = Statistics.stdDev(controlTimesFiltered)
        val treatmentStdDev = Statistics.stdDev(treatmentTimesFiltered)

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
