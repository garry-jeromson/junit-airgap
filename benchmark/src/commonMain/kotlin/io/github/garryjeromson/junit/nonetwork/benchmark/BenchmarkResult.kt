package io.github.garryjeromson.junit.nonetwork.benchmark

import kotlin.math.sqrt

/**
 * Represents the results of a benchmark run.
 *
 * @property name Name of the benchmark
 * @property controlMedianNs Median time in nanoseconds for control group (no extension)
 * @property treatmentMedianNs Median time in nanoseconds for treatment group (with extension)
 * @property controlStdDevNs Standard deviation for control group
 * @property treatmentStdDevNs Standard deviation for treatment group
 * @property overheadPercentage Percentage overhead (positive means slower, negative means faster)
 */
data class BenchmarkResult(
    val name: String,
    val controlMedianNs: Double,
    val treatmentMedianNs: Double,
    val controlStdDevNs: Double,
    val treatmentStdDevNs: Double,
) {
    /**
     * Calculate the overhead percentage.
     * Positive values mean treatment is slower than control.
     */
    val overheadPercentage: Double
        get() = ((treatmentMedianNs - controlMedianNs) / controlMedianNs) * 100.0

    /**
     * Check if the overhead is within acceptable limits.
     */
    fun isWithinThreshold(maxOverheadPercentage: Double = BenchmarkConfig.MAX_OVERHEAD_PERCENTAGE): Boolean =
        overheadPercentage <= maxOverheadPercentage

    /**
     * Format the result as a human-readable string.
     */
    fun format(): String {
        val controlMs = controlMedianNs / 1_000_000.0
        val treatmentMs = treatmentMedianNs / 1_000_000.0
        val controlStdDevMs = controlStdDevNs / 1_000_000.0
        val treatmentStdDevMs = treatmentStdDevNs / 1_000_000.0
        val status = if (isWithinThreshold()) "✅ PASS" else "❌ FAIL"

        return buildString {
            appendLine("━".repeat(60))
            appendLine("Benchmark: $name")
            appendLine("━".repeat(60))
            appendLine(
                "Control (no extension):  ${String.format(
                    "%.3f",
                    controlMs,
                )}ms (±${String.format("%.3f", controlStdDevMs)}ms)",
            )
            appendLine(
                "Treatment (with ext):    ${String.format(
                    "%.3f",
                    treatmentMs,
                )}ms (±${String.format("%.3f", treatmentStdDevMs)}ms)",
            )
            appendLine("Overhead:                ${String.format("%.2f", overheadPercentage)}%")
            appendLine("Status:                  $status (<${BenchmarkConfig.MAX_OVERHEAD_PERCENTAGE}%)")
            appendLine("━".repeat(60))
        }
    }
}

/**
 * Statistics helper functions for benchmark measurements.
 */
object Statistics {
    /**
     * Calculate median of a list of values.
     */
    fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val size = sorted.size
        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2.0
        } else {
            sorted[size / 2]
        }
    }

    /**
     * Calculate mean (average) of a list of values.
     */
    fun mean(values: List<Double>): Double = values.sum() / values.size

    /**
     * Calculate standard deviation of a list of values.
     */
    fun stdDev(values: List<Double>): Double {
        val meanValue = mean(values)
        val variance = values.map { (it - meanValue) * (it - meanValue) }.sum() / values.size
        return sqrt(variance)
    }

    /**
     * Remove outliers from a list of values.
     * Removes the top and bottom percentage as specified in config.
     */
    fun removeOutliers(
        values: List<Double>,
        outlierPercentage: Double = BenchmarkConfig.OUTLIER_PERCENTAGE,
    ): List<Double> {
        if (values.size < 10) return values // Don't remove outliers from small samples

        val sorted = values.sorted()
        val removeCount = (values.size * outlierPercentage / 100.0).toInt()

        return if (removeCount > 0) {
            sorted.subList(removeCount, sorted.size - removeCount)
        } else {
            sorted
        }
    }
}
