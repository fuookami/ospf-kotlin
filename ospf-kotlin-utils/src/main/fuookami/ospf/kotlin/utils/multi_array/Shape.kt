package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.math.algebra.concept.Integer
import fuookami.ospf.kotlin.utils.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import kotlin.ConsistentCopyVisibility
import kotlin.reflect.KClass

/**
 * 维度不匹配异�?
 * Dimension mismatching exception
 */
class DimensionMismatchingException(
    val dimension: Int,
    val vectorDimension: Int
) : Throwable() {
    override val message: String = "Dimension should be $dimension, not $vectorDimension."
}

/**
 * 形状越界异常
 * Out of shape exception
 */
class OutOfShapeException(
    val dimension: Int,
    val length: Int,
    val vectorIndex: Int
) : Throwable() {
    override val message: String = "Length of dimension $dimension is $length, but it get $vectorIndex."
}

/**
 * 未知虚拟索引类型异常
 * Unknown dummy index type exception
 */
class UnknownDummyIndexTypeException(
    val cls: KClass<*>
) : Throwable() {
    override val message: String = "Unknown dummy index type: $cls."
}

/**
 * 存储顺序枚举
 * Storage order enum
 *
 * - RowMajor: 行主序（C 风格），最后一个维度变化最�?
 *   Row-major (C style), last dimension varies fastest
 * - ColumnMajor: 列主序（Fortran 风格），第一个维度变化最�?
 *   Column-major (Fortran style), first dimension varies fastest
 */
enum class StorageOrder {
    RowMajor,
    ColumnMajor;

    companion object {
        val Default = RowMajor
    }
}

/**
 * 形状接口
 * Shape interface
 *
 * 定义多维数组形状的基本操作�?
 * Defines basic operations for multi-dimensional array shapes.
 */
interface Shape {
    /**
     * 维度数量
     * Number of dimensions
     */
    val dimension: Int

    /**
     * 维度数量（无符号�?
     * Number of dimensions (unsigned)
     */
    val udimension: UInt64 get() = UInt64(dimension)

    /**
     * 元素总数
     * Total number of elements
     */
    val size: Int

    /**
     * 元素总数（无符号�?
     * Total number of elements (unsigned)
     */
    val usize: UInt64 get() = UInt64(size)

    /**
     * 维度索引范围
     * Dimension index range
     */
    val indices: IntRange get() = 0 until dimension

    /**
     * 存储顺序
     * Storage order
     */
    val storageOrder: StorageOrder get() = StorageOrder.Default

    /**
     * 各维度的步长
     * Strides for each dimension
     */
    val offsets: IntArray

    /**
     * 获取指定维度的长�?
     * Get the length of the specified dimension
     */
    operator fun get(index: Int): Int

    /**
     * 将向量索引转换为线性索�?
     * Convert vector index to linear index
     */
    fun index(vector: IntArray): Int

    /**
     * 将线性索引转换为向量索引
     * Convert linear index to vector index
     */
    fun vector(index: Int): IntArray

    /**
     * 检查是否为�?
     * Check if empty
     */
    fun isEmpty(): Boolean = size == 0

    /**
     * 获取下一个向量索�?
     * Get the next vector index
     *
     * @param vector 当前向量索引
     * @return 下一个向量索引，如果已到达末尾则返回 null
     */
    fun next(vector: IntArray): IntArray? {
        val temp = vector.copyOf()
        var i = dimension - 1
        while (i >= 0) {
            if (this[i] == 0 || temp[i] == (this[i] - 1)) {
                temp[i] = 0
            } else {
                temp[i] = temp[i] + 1
                return temp
            }
            --i
        }
        return null
    }

    /**
     * 获取指定维度的步�?
     * Get the stride for the specified dimension
     */
    fun offset(dimension: Int): Int {
        if (dimension >= this.dimension || dimension < 0) {
            throw DimensionMismatchingException(this.dimension, dimension)
        }
        return offsets[dimension]
    }

    /**
     * 计算实际索引
     * Calculate actual index
     *
     * 处理负数索引（从末尾计数）�?
     * Handles negative indices (counting from the end).
     *
     * @param dimension 维度索引
     * @param index 原始索引
     * @return 实际索引，如果越界则返回 null
     */
    fun actualIndex(dimension: Int, index: Int): Int? {
        val len = try {
            this[dimension]
        } catch (e: ArrayIndexOutOfBoundsException) {
            return null
        }

        return when {
            index >= len || index < -len -> null
            index >= 0 -> index
            else -> len + index
        }
    }

    /**
     * 创建零向�?
     * Create zero vector
     */
    fun zero(): IntArray = IntArray(dimension)

    /**
     * 将虚拟索引转换为迭代器向�?
     * Convert dummy vector to iterator vector
     */
    fun dummyToIteratorVector(dummyVector: DummyVector): IteratorVector {
        return dummyVector.mapIndexed { i, dummy -> dummy.iteratorOf(this, i) }
    }

    /**
     * 将虚拟向量转换为映射向量
     * Convert dummy vector to map vector
     */
    fun dummyToMapVector(dummyVector: DummyVector): MapVector {
        return dummyVector.mapIndexed { index, dummy ->
            MapIndex.Dummy(dummy)
        }
    }

    /**
     * 将映射向量转换为迭代器向�?
     * Convert map vector to iterator vector
     */
    fun mapToIteratorVector(mapVector: MapVector): IteratorVector {
        return mapVector.mapIndexed { i, mapIndex ->
            when (mapIndex) {
                is MapIndex.Dummy -> mapIndex.dummy.iteratorOf(this, i)
                is MapIndex.Map -> {
                    // 映射索引：创建全范围迭代�?
                    DummyIndexIterator.Continuous(0 until this[mapIndex.index])
                }
            }
        }
    }

    /**
     * 从任意类型数组创建虚拟向�?
     * Create dummy vector from any type array
     */
    fun dummyVector(vararg v: Any): DummyVector {
        if (v.size != dimension) {
            throw DimensionMismatchingException(
                dimension = dimension,
                vectorDimension = v.size
            )
        }
        val vector = ArrayList<DummyIndex>()
        for (i in indices) {
            when (val index = v[i]) {
                _a -> {
                    vector.add(DummyIndex.all())
                }

                is IntRange -> {
                    vector.add(DummyIndex.from(index))
                }

                is Int -> {
                    vector.add(DummyIndex.from(index))
                }

                is Indexed -> {
                    vector.add(DummyIndex.from(index.index))
                }

                is DummyIndex -> {
                    vector.add(index)
                }

                is Integer<*> -> {
                    vector.add(DummyIndex.from((index as RealNumber<*>).toUInt64().toInt()))
                }

                else -> {
                    throw UnknownDummyIndexTypeException(
                        cls = index.javaClass.kotlin
                    )
                }
            }
        }
        return vector
    }
}

/**
 * 一维形�?
 * One-dimensional shape
 */
@ConsistentCopyVisibility
data class Shape1 private constructor(
    private val d1: Int,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        operator fun invoke(d1: Int): Shape1 = Shape1(d1, StorageOrder.Default)
        operator fun invoke(d1: UInt64): Shape1 = Shape1(d1.toInt(), StorageOrder.Default)
        operator fun invoke(d1: Collection<*>): Shape1 = Shape1(d1.size, StorageOrder.Default)

        fun withOrder(d1: Int, order: StorageOrder): Shape1 = Shape1(d1, order)
        fun withOrder(d1: UInt64, order: StorageOrder): Shape1 = Shape1(d1.toInt(), order)
    }

    override val dimension = 1
    override val size by ::d1

    override val offsets: IntArray by lazy { intArrayOf(1) }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            else -> throw ArrayIndexOutOfBoundsException("Dimension index $index out of bounds for shape dimension $dimension")
        }
    }

    @Throws(DimensionMismatchingException::class, OutOfShapeException::class)
    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            1 -> if (vector[0] < 0 || vector[0] >= d1) {
                throw OutOfShapeException(
                    dimension = 0,
                    length = d1,
                    vectorIndex = vector[0]
                )
            } else {
                vector[0] * offsets[0]
            }

            else -> throw DimensionMismatchingException(
                dimension = 1,
                vectorDimension = vector.size
            )
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index < 0 || index >= d1) {
            throw ArrayIndexOutOfBoundsException("Index $index out of bounds for shape size $d1")
        } else {
            intArrayOf(index / offsets[0])
        }
    }

    fun withStorageOrder(order: StorageOrder): Shape1 = Shape1(d1, order)
}

/**
 * 二维形状
 * Two-dimensional shape
 */
@ConsistentCopyVisibility
data class Shape2 private constructor(
    private val d1: Int,
    private val d2: Int,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        operator fun invoke(d1: Int, d2: Int): Shape2 = Shape2(d1, d2, StorageOrder.Default)
        operator fun invoke(d1: UInt64, d2: UInt64): Shape2 = Shape2(d1.toInt(), d2.toInt(), StorageOrder.Default)
        operator fun invoke(d1: Collection<*>, d2: Collection<*>): Shape2 = Shape2(d1.size, d2.size, StorageOrder.Default)

        fun withOrder(d1: Int, d2: Int, order: StorageOrder): Shape2 = Shape2(d1, d2, order)
        fun withOrder(d1: UInt64, d2: UInt64, order: StorageOrder): Shape2 = Shape2(d1.toInt(), d2.toInt(), order)
    }

    private val totalSize by lazy { d1 * d2 }
    override val dimension = 2
    override val size get() = totalSize

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> intArrayOf(d2, 1)
            StorageOrder.ColumnMajor -> intArrayOf(1, d1)
        }
    }

    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            1 -> d2
            else -> throw ArrayIndexOutOfBoundsException("Dimension index $index out of bounds for shape dimension $dimension")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            2 -> {
                if (vector[0] < 0 || vector[0] >= d1) {
                    throw OutOfShapeException(
                        dimension = 0,
                        length = d1,
                        vectorIndex = vector[0]
                    )
                } else if (vector[1] < 0 || vector[1] >= d2) {
                    throw OutOfShapeException(
                        dimension = 1,
                        length = d2,
                        vectorIndex = vector[1]
                    )
                } else {
                    vector[0] * offsets[0] + vector[1] * offsets[1]
                }
            }

            else -> throw DimensionMismatchingException(
                dimension = 2,
                vectorDimension = vector.size
            )
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index < 0 || index >= totalSize) {
            throw ArrayIndexOutOfBoundsException("Index $index out of bounds for shape size $totalSize")
        } else {
            when (storageOrder) {
                StorageOrder.RowMajor -> {
                    intArrayOf(index / offsets[0], index % offsets[0] / offsets[1])
                }

                StorageOrder.ColumnMajor -> {
                    // For ColumnMajor: index = v[0] + v[1] * d1
                    // So: v[0] = index % d1, v[1] = index / d1
                    intArrayOf(index % d1, index / d1)
                }
            }
        }
    }

    fun withStorageOrder(order: StorageOrder): Shape2 = Shape2(d1, d2, order)
}

/**
 * 三维形状
 * Three-dimensional shape
 */
@ConsistentCopyVisibility
data class Shape3 private constructor(
    private val d1: Int,
    private val d2: Int,
    private val d3: Int,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        operator fun invoke(d1: Int, d2: Int, d3: Int): Shape3 = Shape3(d1, d2, d3, StorageOrder.Default)
        operator fun invoke(d1: UInt64, d2: UInt64, d3: UInt64): Shape3 = Shape3(d1.toInt(), d2.toInt(), d3.toInt(), StorageOrder.Default)
        operator fun invoke(d1: Collection<*>, d2: Collection<*>, d3: Collection<*>): Shape3 = Shape3(d1.size, d2.size, d3.size, StorageOrder.Default)

        fun withOrder(d1: Int, d2: Int, d3: Int, order: StorageOrder): Shape3 = Shape3(d1, d2, d3, order)
        fun withOrder(d1: UInt64, d2: UInt64, d3: UInt64, order: StorageOrder): Shape3 = Shape3(d1.toInt(), d2.toInt(), d3.toInt(), order)
    }

    private val totalSize by lazy { d1 * d2 * d3 }
    override val dimension = 3
    override val size get() = totalSize

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> intArrayOf(d2 * d3, d3, 1)
            StorageOrder.ColumnMajor -> intArrayOf(1, d1, d1 * d2)
        }
    }

    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            1 -> d2
            2 -> d3
            else -> throw ArrayIndexOutOfBoundsException("Dimension index $index out of bounds for shape dimension $dimension")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            3 -> {
                if (vector[0] < 0 || vector[0] >= d1) {
                    throw OutOfShapeException(
                        dimension = 0,
                        length = d1,
                        vectorIndex = vector[0]
                    )
                } else if (vector[1] < 0 || vector[1] >= d2) {
                    throw OutOfShapeException(
                        dimension = 1,
                        length = d2,
                        vectorIndex = vector[1]
                    )
                } else if (vector[2] < 0 || vector[2] >= d3) {
                    throw OutOfShapeException(
                        dimension = 2,
                        length = d3,
                        vectorIndex = vector[2]
                    )
                } else {
                    vector[0] * offsets[0] + vector[1] * offsets[1] + vector[2] * offsets[2]
                }
            }

            else -> throw DimensionMismatchingException(
                dimension = 3,
                vectorDimension = vector.size
            )
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index < 0 || index >= totalSize) {
            throw ArrayIndexOutOfBoundsException("Index $index out of bounds for shape size $totalSize")
        } else {
            when (storageOrder) {
                StorageOrder.RowMajor -> {
                    var currentIndex = index
                    intArrayOf(
                        currentIndex / offsets[0],
                        currentIndex % offsets[0] / offsets[1],
                        currentIndex % offsets[1]
                    )
                }

                StorageOrder.ColumnMajor -> {
                    intArrayOf(
                        index % offsets[0],
                        index / offsets[0] % offsets[1],
                        index / offsets[0] / offsets[1]
                    )
                }
            }
        }
    }

    fun withStorageOrder(order: StorageOrder): Shape3 = Shape3(d1, d2, d3, order)
}

/**
 * 四维形状
 * Four-dimensional shape
 */
@ConsistentCopyVisibility
data class Shape4 private constructor(
    private val d1: Int,
    private val d2: Int,
    private val d3: Int,
    private val d4: Int,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        operator fun invoke(d1: Int, d2: Int, d3: Int, d4: Int): Shape4 = Shape4(d1, d2, d3, d4, StorageOrder.Default)
        operator fun invoke(d1: UInt64, d2: UInt64, d3: UInt64, d4: UInt64): Shape4 = Shape4(d1.toInt(), d2.toInt(), d3.toInt(), d4.toInt(), StorageOrder.Default)
        operator fun invoke(d1: Collection<*>, d2: Collection<*>, d3: Collection<*>, d4: Collection<*>): Shape4 = Shape4(d1.size, d2.size, d3.size, d4.size, StorageOrder.Default)

        fun withOrder(d1: Int, d2: Int, d3: Int, d4: Int, order: StorageOrder): Shape4 = Shape4(d1, d2, d3, d4, order)
        fun withOrder(d1: UInt64, d2: UInt64, d3: UInt64, d4: UInt64, order: StorageOrder): Shape4 = Shape4(d1.toInt(), d2.toInt(), d3.toInt(), d4.toInt(), order)
    }

    private val totalSize by lazy { d1 * d2 * d3 * d4 }
    override val dimension = 4
    override val size get() = totalSize

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> intArrayOf(d2 * d3 * d4, d3 * d4, d4, 1)
            StorageOrder.ColumnMajor -> intArrayOf(1, d1, d1 * d2, d1 * d2 * d3)
        }
    }

    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            1 -> d2
            2 -> d3
            3 -> d4
            else -> throw ArrayIndexOutOfBoundsException("Dimension index $index out of bounds for shape dimension $dimension")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            4 -> {
                if (vector[0] < 0 || vector[0] >= d1) {
                    throw OutOfShapeException(
                        dimension = 0,
                        length = d1,
                        vectorIndex = vector[0]
                    )
                } else if (vector[1] < 0 || vector[1] >= d2) {
                    throw OutOfShapeException(
                        dimension = 1,
                        length = d2,
                        vectorIndex = vector[1]
                    )
                } else if (vector[2] < 0 || vector[2] >= d3) {
                    throw OutOfShapeException(
                        dimension = 2,
                        length = d3,
                        vectorIndex = vector[2]
                    )
                } else if (vector[3] < 0 || vector[3] >= d4) {
                    throw OutOfShapeException(
                        dimension = 3,
                        length = d4,
                        vectorIndex = vector[3]
                    )
                } else {
                    vector[0] * offsets[0] + vector[1] * offsets[1] + vector[2] * offsets[2] + vector[3] * offsets[3]
                }
            }

            else -> throw DimensionMismatchingException(
                dimension = 4,
                vectorDimension = vector.size
            )
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index < 0 || index >= totalSize) {
            throw ArrayIndexOutOfBoundsException("Index $index out of bounds for shape size $totalSize")
        } else {
            when (storageOrder) {
                StorageOrder.RowMajor -> {
                    var currentIndex = index
                    intArrayOf(
                        currentIndex / offsets[0],
                        currentIndex % offsets[0] / offsets[1],
                        currentIndex % offsets[1] / offsets[2],
                        currentIndex % offsets[2]
                    )
                }

                StorageOrder.ColumnMajor -> {
                    intArrayOf(
                        index % offsets[0],
                        index / offsets[0] % offsets[1],
                        index / offsets[0] / offsets[1] % offsets[2],
                        index / offsets[0] / offsets[1] / offsets[2]
                    )
                }
            }
        }
    }

    fun withStorageOrder(order: StorageOrder): Shape4 = Shape4(d1, d2, d3, d4, order)
}

/**
 * 动态维度形�?
 * Dynamic dimension shape
 */
@ConsistentCopyVisibility
data class DynShape private constructor(
    private val shape: IntArray,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        @JvmStatic
        private fun calculateTotalSize(shape: IntArray): Int {
            var ret = 1
            for (l in shape) {
                ret *= l
            }
            return ret
        }

        @JvmStatic
        private fun calculateOffsetsRowMajor(shape: IntArray): IntArray {
            if (shape.isEmpty()) return intArrayOf()
            val offsets = IntArray(shape.size)
            offsets[shape.size - 1] = 1
            for (i in (shape.size - 2) downTo 0) {
                offsets[i] = offsets[i + 1] * shape[i + 1]
            }
            return offsets
        }

        @JvmStatic
        private fun calculateOffsetsColumnMajor(shape: IntArray): IntArray {
            if (shape.isEmpty()) return intArrayOf()
            val offsets = IntArray(shape.size)
            offsets[0] = 1
            for (i in 1 until shape.size) {
                offsets[i] = offsets[i - 1] * shape[i - 1]
            }
            return offsets
        }

        operator fun invoke(shape: IntArray): DynShape = DynShape(shape, StorageOrder.Default)

        @JvmName("constructByUInt64List")
        operator fun invoke(shape: Iterable<UInt64>): DynShape = DynShape(shape.map { it.toInt() }.toIntArray(), StorageOrder.Default)

        @JvmName("constructByCollectionList")
        operator fun invoke(shape: Iterable<Collection<*>>): DynShape = DynShape(shape.map { it.size }.toIntArray(), StorageOrder.Default)

        fun withOrder(shape: IntArray, order: StorageOrder): DynShape = DynShape(shape, order)

        @JvmName("withOrderFromUInt64List")
        fun withOrder(shape: Iterable<UInt64>, order: StorageOrder): DynShape = DynShape(shape.map { it.toInt() }.toIntArray(), order)

        @JvmName("withOrderFromCollectionList")
        fun withOrder(shape: Iterable<Collection<*>>, order: StorageOrder): DynShape = DynShape(shape.map { it.size }.toIntArray(), order)
    }

    private val totalSize by lazy { calculateTotalSize(shape) }
    override val dimension get() = shape.size
    override val size get() = totalSize

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> calculateOffsetsRowMajor(shape)
            StorageOrder.ColumnMajor -> calculateOffsetsColumnMajor(shape)
        }
    }

    override operator fun get(index: Int): Int {
        return shape[index]
    }

    override fun index(vector: IntArray): Int {
        if (dimension != vector.size) {
            throw DimensionMismatchingException(dimension, vector.size)
        }
        var ret = 0
        for (i in shape.indices) {
            if (vector[i] < 0 || vector[i] >= shape[i]) {
                throw OutOfShapeException(
                    dimension = i,
                    length = shape[i],
                    vectorIndex = vector[i]
                )
            }
            ret += vector[i] * offsets[i]
        }
        return ret
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index < 0 || index >= totalSize) {
            throw ArrayIndexOutOfBoundsException("Index $index out of bounds for shape size $totalSize")
        } else {
            when (storageOrder) {
                StorageOrder.RowMajor -> {
                    var currentIndex = index
                    IntArray(dimension) { i ->
                        val result = currentIndex / offsets[i]
                        currentIndex %= offsets[i]
                        result
                    }
                }

                StorageOrder.ColumnMajor -> {
                    var currentIndex = index
                    IntArray(dimension) { i ->
                        val result = currentIndex / offsets[i]
                        if (i < dimension - 1) {
                            currentIndex %= offsets[i]
                            result % shape[i + 1]
                        } else {
                            result
                        }
                    }
                }
            }
        }
    }

    fun withStorageOrder(order: StorageOrder): DynShape = DynShape(shape.copyOf(), order)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DynShape

        if (!shape.contentEquals(other.shape)) return false
        if (storageOrder != other.storageOrder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.contentHashCode()
        result = 31 * result + storageOrder.hashCode()
        return result
    }
}



