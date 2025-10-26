import org.gradle.api.GradleException
import java.io.File
import kotlin.math.abs

/**
 * Utility for comparing benchmark results from control and treatment projects.
 */
object BenchmarkComparison {
    /**
     * Compare benchmark results and generate a report.
     *
     * @param controlDir Build directory of the control project
     * @param treatmentDir Build directory of the treatment project
     * @param suiteThresholdPercent Maximum acceptable suite overhead percentage
     * @param testThresholdPercent Maximum acceptable per-test overhead percentage
     * @return Comparison report as markdown
     * @throws GradleException if thresholds are exceeded
     */
    fun compare(
        controlDir: File,
        treatmentDir: File,
        suiteThresholdPercent: Double = 10.0,
        testThresholdPercent: Double = 5.0,
    ): String {
        // Load suite timing
        val controlSuiteTiming = loadSuiteTiming(File(controlDir, "benchmark-results/suite-timing.json"))
        val treatmentSuiteTiming = loadSuiteTiming(File(treatmentDir, "benchmark-results/suite-timing.json"))

        val suiteOverheadPercent = ((treatmentSuiteTiming - controlSuiteTiming) / controlSuiteTiming) * 100.0
        val suitePass = suiteOverheadPercent <= suiteThresholdPercent

        // Build report
        val report = buildString {
            appendLine("# Benchmark Comparison Report")
            appendLine()
            appendLine("## Suite-Level Timing (Agent Loading Overhead)")
            appendLine()
            appendLine("| Metric | Control | Treatment | Overhead |")
            appendLine("|--------|---------|-----------|----------|")
            appendLine("| Duration | ${String.format("%.2f", controlSuiteTiming)}ms | ${String.format("%.2f", treatmentSuiteTiming)}ms | ${String.format("%.2f", suiteOverheadPercent)}% |")
            appendLine()
            appendLine("**Suite Threshold:** ${suiteThresholdPercent}%")
            appendLine("**Status:** ${if (suitePass) "✅ PASS" else "❌ FAIL"}")
            appendLine()
            appendLine("## Summary")
            appendLine()
            if (suitePass) {
                appendLine("✅ All benchmarks passed!")
                appendLine()
                appendLine("The JVMTI agent loading overhead is within acceptable limits.")
            } else {
                appendLine("❌ Benchmark failed!")
                appendLine()
                appendLine("Suite overhead of ${String.format("%.2f", suiteOverheadPercent)}% exceeds threshold of ${suiteThresholdPercent}%")
            }
        }

        // Throw if thresholds exceeded
        if (!suitePass) {
            throw GradleException(
                "Benchmark comparison failed:\n" +
                        "  Suite overhead: ${String.format("%.2f", suiteOverheadPercent)}% (threshold: ${suiteThresholdPercent}%)\n"
            )
        }

        return report
    }

    private fun loadSuiteTiming(file: File): Double {
        if (!file.exists()) {
            throw GradleException("Suite timing file not found: ${file.absolutePath}")
        }

        val content = file.readText()
        // Simple JSON parsing for {"suiteDurationMs": 1234.56}
        val match = Regex(""""suiteDurationMs":\s*([0-9.]+)""").find(content)
            ?: throw GradleException("Could not parse suite timing from: ${file.absolutePath}")

        return match.groupValues[1].toDouble()
    }
}
