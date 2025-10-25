package io.github.garryjeromson.junit.nonetwork.benchmark

import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Android I/O operation benchmarks to verify no overhead for file and stream operations.
 * These tests perform non-network I/O to ensure SecurityManager only checks network operations.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidIoOperationsBenchmarkTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @NoNetworkTest
    fun `benchmark file write and read operations`() {
        val tempDir = createTempDirectory("benchmark").toFile()
        try {
            val result = BenchmarkRunner.runBenchmark(
                name = "Android I/O (File Read/Write)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val file = File(tempDir, "test_${System.nanoTime()}.txt")
                        file.writeText("Hello, World!".repeat(100))
                        file.readText()
                        file.delete()
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val file = File(tempDir, "test_${System.nanoTime()}.txt")
                        file.writeText("Hello, World!".repeat(100))
                        file.readText()
                        file.delete()
                    }
                },
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    @NoNetworkTest
    fun `benchmark byte stream operations`() {
        val result = BenchmarkRunner.runBenchmark(
            name = "Android I/O (Byte Streams)",
            control = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val outputStream = ByteArrayOutputStream()
                    outputStream.write("Test data ".repeat(100).toByteArray())
                    val data = outputStream.toByteArray()

                    val inputStream = ByteArrayInputStream(data)
                    inputStream.readBytes()
                }
            },
            treatment = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val outputStream = ByteArrayOutputStream()
                    outputStream.write("Test data ".repeat(100).toByteArray())
                    val data = outputStream.toByteArray()

                    val inputStream = ByteArrayInputStream(data)
                    inputStream.readBytes()
                }
            },
        )
    }

    @Test
    @NoNetworkTest
    fun `benchmark buffered file operations`() {
        val tempDir = createTempDirectory("benchmark").toFile()
        try {
            val result = BenchmarkRunner.runBenchmark(
                name = "Android I/O (Buffered File Operations)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val file = File(tempDir, "buffered_${System.nanoTime()}.txt")
                        file.bufferedWriter().use { writer ->
                            repeat(100) { writer.write("Line $it\n") }
                        }
                        file.bufferedReader().use { reader ->
                            reader.readLines()
                        }
                        file.delete()
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val file = File(tempDir, "buffered_${System.nanoTime()}.txt")
                        file.bufferedWriter().use { writer ->
                            repeat(100) { writer.write("Line $it\n") }
                        }
                        file.bufferedReader().use { reader ->
                            reader.readLines()
                        }
                        file.delete()
                    }
                },
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    @NoNetworkTest
    fun `benchmark directory operations`() {
        val tempRoot = createTempDirectory("benchmark").toFile()
        try {
            val result = BenchmarkRunner.runBenchmark(
                name = "Android I/O (Directory Operations)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val dir = File(tempRoot, "dir_${System.nanoTime()}")
                        dir.mkdir()
                        repeat(10) {
                            File(dir, "file_$it.txt").writeText("content")
                        }
                        dir.listFiles()
                        dir.deleteRecursively()
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val dir = File(tempRoot, "dir_${System.nanoTime()}")
                        dir.mkdir()
                        repeat(10) {
                            File(dir, "file_$it.txt").writeText("content")
                        }
                        dir.listFiles()
                        dir.deleteRecursively()
                    }
                },
            )
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    @NoNetworkTest
    fun `benchmark file metadata operations`() {
        val tempDir = createTempDirectory("benchmark").toFile()
        try {
            val result = BenchmarkRunner.runBenchmark(
                name = "Android I/O (File Metadata)",
                control = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val file = File(tempDir, "meta_${System.nanoTime()}.txt")
                        file.writeText("test")
                        file.exists()
                        file.length()
                        file.lastModified()
                        file.isFile
                        file.isDirectory
                        file.canRead()
                        file.canWrite()
                        file.delete()
                    }
                },
                treatment = {
                    repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                        val file = File(tempDir, "meta_${System.nanoTime()}.txt")
                        file.writeText("test")
                        file.exists()
                        file.length()
                        file.lastModified()
                        file.isFile
                        file.isDirectory
                        file.canRead()
                        file.canWrite()
                        file.delete()
                    }
                },
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
