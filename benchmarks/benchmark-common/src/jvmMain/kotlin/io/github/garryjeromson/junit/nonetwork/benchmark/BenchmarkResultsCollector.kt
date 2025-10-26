package io.github.garryjeromson.junit.nonetwork.benchmark

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

/**
 * Simple result for a single benchmark test run.
 * This captures timing for ONE side of the comparison (either control or treatment).
 */
data class SingleBenchmarkResult(
    val name: String,
    val medianNs: Double,
    val stdDevNs: Double,
)

/**
 * JUnit 5 extension that collects benchmark results and writes them to JSON.
 *
 * Usage: Add @ExtendWith(BenchmarkResultsCollector::class) to test classes.
 */
class BenchmarkResultsCollector : AfterAllCallback {
    override fun afterAll(context: ExtensionContext) {
        val results = benchmarkResults.values.toList()

        if (results.isEmpty()) {
            println("No benchmark results to export")
            return
        }

        val outputDir = File("build/benchmark-results")
        outputDir.mkdirs()

        val outputFile = File(outputDir, "results.json")
        val json = resultsToJson(results)

        outputFile.writeText(json)
        println("Benchmark results written to: ${outputFile.absolutePath}")
    }

    companion object {
        // Thread-safe storage for benchmark results
        private val benchmarkResults = mutableMapOf<String, SingleBenchmarkResult>()

        /**
         * Register a benchmark result.
         */
        fun addResult(result: SingleBenchmarkResult) {
            synchronized(benchmarkResults) {
                benchmarkResults[result.name] = result
            }
        }

        /**
         * Convert results to JSON format.
         */
        private fun resultsToJson(results: List<SingleBenchmarkResult>): String {
            val jsonResults =
                results.joinToString(",\n    ") { result ->
                    """
                    {
                        "name": "${result.name}",
                        "medianNs": ${result.medianNs},
                        "stdDevNs": ${result.stdDevNs}
                    }
                    """.trimIndent()
                }

            return """
                {
                    "results": [
                        $jsonResults
                    ]
                }
                """.trimIndent()
        }
    }
}
