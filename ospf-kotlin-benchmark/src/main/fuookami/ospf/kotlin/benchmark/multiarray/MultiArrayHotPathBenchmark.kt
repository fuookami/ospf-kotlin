package fuookami.ospf.kotlin.benchmark.multiarray

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import fuookami.ospf.kotlin.multiarray.AccessOrder
import fuookami.ospf.kotlin.multiarray.BlockMultiArray
import fuookami.ospf.kotlin.multiarray.flatten
import fuookami.ospf.kotlin.multiarray.fromList
import fuookami.ospf.kotlin.multiarray.MultiArray
import fuookami.ospf.kotlin.multiarray.MutableMultiArray
import fuookami.ospf.kotlin.multiarray.Shape3
import fuookami.ospf.kotlin.multiarray.StorageOrder
import fuookami.ospf.kotlin.multiarray.vectorUnchecked

/**
 * multiarray 热点路径基准
 * Multiarray hot path benchmark
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class MultiArrayHotPathBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    private lateinit var shape: Shape3
    private lateinit var vectors: List<IntArray>
    private lateinit var dense: MutableMultiArray<Int, Shape3>
    private lateinit var sparse: BlockMultiArray<Int, Shape3>
    private lateinit var sourceList: List<Int>

    @Setup
    fun setup() {
        val n = when (dataset) {
            "small" -> 12
            "medium" -> 28
            "large" -> 48
            else -> 12
        }
        shape = Shape3.withOrder(n, n, n, StorageOrder.RowMajor)
        val size = shape.size
        sourceList = List(size) { it % 97 }
        dense = MutableMultiArray.newWith(shape, 0)
        vectors = List(size) { shape.vectorUnchecked(it) }

        for (i in vectors.indices) {
            dense[vectors[i]] = sourceList[i]
        }
        sparse = BlockMultiArray.fromMultiArray(dense.toImmutable()) { it % 5 == 0 }
    }

    @Benchmark
    fun blockGetAndContains(): Int {
        var sum = 0
        for (vector in vectors) {
            if (sparse.contains(vector)) {
                sum += sparse.get(*vector) ?: 0
            }
        }
        return sum
    }

    @Benchmark
    fun blockSetAndRemove(): Int {
        var touched = 0
        for (i in vectors.indices step 3) {
            val v = vectors[i]
            sparse[v] = i
            if ((i and 1) == 0) {
                sparse.remove(v)
                touched++
            }
        }
        return touched + sparse.size
    }

    @Benchmark
    fun fromListRowMajor(): Int {
        val arr = MutableMultiArray.fromList(shape, sourceList, AccessOrder.RowMajor).value!!
        return arr[shape.vectorUnchecked(shape.size / 2)]
    }

    @Benchmark
    fun flattenColumnMajor(): Int {
        val list = dense.flatten(AccessOrder.ColumnMajor)
        return list.size
    }
}
