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
 *
 * @property dataset 数据集规模标识（small/medium/large） / dataset size identifier (small/medium/large)
 * @property shape 三维形状 / three-dimensional shape
 * @property vectors 索引向量列表 / list of index vectors
 * @property dense 密集可变多数组 / dense mutable multi-array
 * @property sparse 稀疏分块多数组 / sparse block multi-array
 * @property sourceList 源数据列表 / source data list
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

    /** 初始化基准测试数据 / Initializes benchmark test data */
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

    /**
     * 对稀疏分块数组执行获取和包含检查操作
     * Performs get and contains operations on the sparse block multi-array
     *
     * @return 包含元素的累加和 / accumulated sum of contained elements
    */
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

    /**
     * 对稀疏分块数组执行设置和移除操作
     * Performs set and remove operations on the sparse block multi-array
     *
     * @return 被移除的元素数与数组当前大小之和 / sum of removed element count and current array size
    */
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

    /**
     * 从列表按行主序创建可变多数组并访问中间元素
     * Creates a mutable multi-array from a list in row-major order and accesses the middle element
     *
     * @return 中间位置元素的值 / value at the middle position
    */
    @Benchmark
    fun fromListRowMajor(): Int {
        val arr = MutableMultiArray.fromList(shape, sourceList, AccessOrder.RowMajor).value!!
        return arr[shape.vectorUnchecked(shape.size / 2)]
    }

    /**
     * 按列主序展平密集数组并返回列表大小
     * Flattens the dense array in column-major order and returns the list size
     *
     * @return 展平后列表的大小 / size of the flattened list
    */
    @Benchmark
    fun flattenColumnMajor(): Int {
        val list = dense.flatten(AccessOrder.ColumnMajor)
        return list.size
    }
}
