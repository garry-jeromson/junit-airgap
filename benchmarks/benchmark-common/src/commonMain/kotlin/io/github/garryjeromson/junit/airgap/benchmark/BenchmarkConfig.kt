package io.github.garryjeromson.junit.airgap.benchmark

/**
 * Configuration constants for performance benchmarks.
 */
object BenchmarkConfig {
    /**
     * Number of warmup iterations to run before measuring.
     * Warmup iterations allow JIT compilation to optimize code paths.
     * Increased to ensure JIT fully optimizes hot paths.
     */
    const val WARMUP_ITERATIONS = 20

    /**
     * Number of measurement iterations to run for timing.
     */
    const val MEASUREMENT_ITERATIONS = 1000

    /**
     * Percentage of outliers to remove from top and bottom of results.
     * For example, 5.0 means remove top 5% and bottom 5% of measurements.
     */
    const val OUTLIER_PERCENTAGE = 5.0

    /**
     * Maximum acceptable overhead percentage.
     * Tests will fail if overhead exceeds this threshold.
     *
     * Set to 5.0% to account for measurement noise in microbenchmarks
     * while still ensuring negligible overhead for real-world tests.
     */
    const val MAX_OVERHEAD_PERCENTAGE = 5.0

    /**
     * Number of times to repeat each operation within a single iteration
     * to get more stable measurements.
     */
    const val OPERATIONS_PER_ITERATION = 100
}
