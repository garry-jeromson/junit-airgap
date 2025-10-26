import org.gradle.api.GradleException
import java.io.File
import kotlin.math.abs

/**
 * Utility for comparing benchmark results from control and treatment projects.
 */
object BenchmarkComparison {
    data class BenchmarkResult(
        val name: String,
        val medianNs: Double,
        val stdDevNs: Double,
    )

    /**
     * Compare benchmark results and generate a report.
     *
     * @param controlDir Build directory of the control project
     * @param treatmentDir Build directory of the treatment project
     * @param maxOverheadPercent Maximum acceptable per-test overhead percentage
     * @return Comparison report as markdown
     * @throws GradleException if thresholds are exceeded
     */
    fun compare(
        controlDir: File,
        treatmentDir: File,
        maxOverheadPercent: Double = 50.0,
    ): String {
        // Load per-test results
        val controlResults = loadBenchmarkResults(File(controlDir, "benchmark-results/results.json"))
        val treatmentResults = loadBenchmarkResults(File(treatmentDir, "benchmark-results/results.json"))

        // Match results by name
        val comparisons = mutableListOf<Triple<String, Double, String>>()
        var allPass = true

        controlResults.forEach { control ->
            val treatment = treatmentResults.find { it.name == control.name }
            if (treatment != null) {
                val overheadPercent = ((treatment.medianNs - control.medianNs) / control.medianNs) * 100.0
                val pass = overheadPercent <= maxOverheadPercent
                if (!pass) allPass = false

                comparisons.add(
                    Triple(
                        control.name,
                        overheadPercent,
                        if (pass) "✅" else "❌"
                    )
                )
            }
        }

        // Build report
        val report = buildString {
            appendLine("# Benchmark Comparison Report")
            appendLine()
            appendLine("## Per-Test Overhead")
            appendLine()
            appendLine("| Test | Control (median) | Treatment (median) | Overhead | Status |")
            appendLine("|------|------------------|-------------------|----------|--------|")

            controlResults.forEach { control ->
                val treatment = treatmentResults.find { it.name == control.name }
                if (treatment != null) {
                    val overheadPercent = ((treatment.medianNs - control.medianNs) / control.medianNs) * 100.0
                    val pass = overheadPercent <= maxOverheadPercent
                    val status = if (pass) "✅" else "❌"

                    appendLine(
                        "| ${control.name} | " +
                        "${formatNanos(control.medianNs)} | " +
                        "${formatNanos(treatment.medianNs)} | " +
                        "${String.format("%+.1f", overheadPercent)}% | " +
                        "$status |"
                    )
                }
            }

            appendLine()
            appendLine("**Maximum Overhead Threshold:** ${maxOverheadPercent}%")
            appendLine()
            appendLine("## Summary")
            appendLine()

            if (allPass) {
                appendLine("✅ All benchmarks passed!")
                appendLine()
                appendLine("The JVMTI agent overhead is within acceptable limits for all tests.")
            } else {
                appendLine("❌ Some benchmarks exceeded the overhead threshold!")
                appendLine()
                val failures = comparisons.filter { it.third == "❌" }
                appendLine("Failed tests:")
                failures.forEach { (name, overhead, _) ->
                    appendLine("- $name: ${String.format("%+.1f", overhead)}% (threshold: ${maxOverheadPercent}%)")
                }
            }
        }

        // Throw if thresholds exceeded
        if (!allPass) {
            val failures = comparisons.filter { it.third == "❌" }
            throw GradleException(
                "Benchmark comparison failed:\n" +
                failures.joinToString("\n") { (name, overhead, _) ->
                    "  $name: ${String.format("%+.1f", overhead)}% overhead (threshold: ${maxOverheadPercent}%)"
                }
            )
        }

        return report
    }

    private fun loadBenchmarkResults(file: File): List<BenchmarkResult> {
        if (!file.exists()) {
            throw GradleException("Benchmark results file not found: ${file.absolutePath}")
        }

        val content = file.readText()
        val results = mutableListOf<BenchmarkResult>()

        // Parse JSON manually (simple approach for this structure)
        val resultPattern = Regex(
            """"name":\s*"([^"]+)",\s*"medianNs":\s*([0-9.]+),\s*"stdDevNs":\s*([0-9.]+)"""
        )

        resultPattern.findAll(content).forEach { match ->
            results.add(
                BenchmarkResult(
                    name = match.groupValues[1],
                    medianNs = match.groupValues[2].toDouble(),
                    stdDevNs = match.groupValues[3].toDouble()
                )
            )
        }

        if (results.isEmpty()) {
            throw GradleException("No benchmark results found in: ${file.absolutePath}")
        }

        return results
    }

    private fun formatNanos(nanos: Double): String {
        return when {
            nanos < 1_000 -> String.format("%.0f ns", nanos)
            nanos < 1_000_000 -> String.format("%.2f μs", nanos / 1_000)
            nanos < 1_000_000_000 -> String.format("%.2f ms", nanos / 1_000_000)
            else -> String.format("%.2f s", nanos / 1_000_000_000)
        }
    }
}
