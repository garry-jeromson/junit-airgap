package io.github.garryjeromson.junit.nonetwork.benchmark

import io.github.garryjeromson.junit.nonetwork.NoNetworkRule
import io.github.garryjeromson.junit.nonetwork.NoNetworkTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Android memory operation benchmarks to verify no overhead for memory-intensive tests.
 * These tests perform object allocation, collection operations, and memory manipulation.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidMemoryOperationsBenchmarkTest {

    @get:Rule
    val noNetworkRule = NoNetworkRule()

    @Test
    @NoNetworkTest
    fun `benchmark object allocation`() {
        val result = BenchmarkRunner.runBenchmark(
            name = "Android Memory (Object Allocation)",
            control = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val objects = List(1000) { TestObject(it, "value_$it") }
                    objects.size // Prevent optimization
                }
            },
            treatment = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val objects = List(1000) { TestObject(it, "value_$it") }
                    objects.size
                }
            },
        )
    }

    @Test
    @NoNetworkTest
    fun `benchmark list operations`() {
        val result = BenchmarkRunner.runBenchmark(
            name = "Android Memory (List Operations)",
            control = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val list = mutableListOf<Int>()
                    for (i in 1..1000) {
                        list.add(i)
                    }
                    list.filter { it % 2 == 0 }
                    list.map { it * 2 }
                    list.reversed()
                }
            },
            treatment = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val list = mutableListOf<Int>()
                    for (i in 1..1000) {
                        list.add(i)
                    }
                    list.filter { it % 2 == 0 }
                    list.map { it * 2 }
                    list.reversed()
                }
            },
        )
    }

    @Test
    @NoNetworkTest
    fun `benchmark map operations`() {
        val result = BenchmarkRunner.runBenchmark(
            name = "Android Memory (Map Operations)",
            control = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val map = mutableMapOf<String, Int>()
                    for (i in 1..500) {
                        map["key_$i"] = i
                    }
                    map.filter { it.value % 2 == 0 }
                    map.mapValues { it.value * 2 }
                }
            },
            treatment = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val map = mutableMapOf<String, Int>()
                    for (i in 1..500) {
                        map["key_$i"] = i
                    }
                    map.filter { it.value % 2 == 0 }
                    map.mapValues { it.value * 2 }
                }
            },
        )
    }

    @Test
    @NoNetworkTest
    fun `benchmark set operations`() {
        val result = BenchmarkRunner.runBenchmark(
            name = "Android Memory (Set Operations)",
            control = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val set1 = (1..500).toSet()
                    val set2 = (250..750).toSet()
                    set1.intersect(set2)
                    set1.union(set2)
                    set1.subtract(set2)
                }
            },
            treatment = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val set1 = (1..500).toSet()
                    val set2 = (250..750).toSet()
                    set1.intersect(set2)
                    set1.union(set2)
                    set1.subtract(set2)
                }
            },
        )
    }

    @Test
    @NoNetworkTest
    fun `benchmark array copying`() {
        val result = BenchmarkRunner.runBenchmark(
            name = "Android Memory (Array Copying)",
            control = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val source = IntArray(1000) { it }
                    val dest = source.copyOf()
                    dest.copyInto(IntArray(1000), 0, 0, 500)
                }
            },
            treatment = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val source = IntArray(1000) { it }
                    val dest = source.copyOf()
                    dest.copyInto(IntArray(1000), 0, 0, 500)
                }
            },
        )
    }

    @Test
    @NoNetworkTest
    fun `benchmark data class operations`() {
        val result = BenchmarkRunner.runBenchmark(
            name = "Android Memory (Data Class Operations)",
            control = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val original = TestObject(1, "test")
                    val copied = original.copy()
                    val modified = original.copy(value = "modified")
                    listOf(original, copied, modified).hashCode()
                }
            },
            treatment = {
                repeat(BenchmarkConfig.OPERATIONS_PER_ITERATION) {
                    val original = TestObject(1, "test")
                    val copied = original.copy()
                    val modified = original.copy(value = "modified")
                    listOf(original, copied, modified).hashCode()
                }
            },
        )
    }

    // Helper classes
    data class TestObject(val id: Int, val value: String)
}
