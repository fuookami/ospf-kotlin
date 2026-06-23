/**
 * 形状定义模块
 * Shape Definition Module
 *
 * 本模块提供多维数组的形状抽象，包括维度信息、索引计算和存储顺序管理。
 * This module provides shape abstractions for multi-dimensional arrays,
 * including dimension information, index calculation, and storage order management.
 *
 * 主要类型：
 * Main types:
 * - [Shape]: 形状接口
 *   Shape interface
 * - [Shape1]: 一维形状
 *   1-dimensional shape
 * - [Shape2]: 二维形状
 *   2-dimensional shape
 * - [Shape3]: 三维形状
 *   3-dimensional shape
 * - [Shape4]: 四维形状
 *   4-dimensional shape
 * - [DynShape]: 动态维度形状
 *   Dynamic dimension shape
 * - [StorageOrder]: 存储顺序枚举（行主序/列主序）
 *   Storage order enum (RowMajor/ColumnMajor)
 *
 * 索引计算：
 * Index calculation:
 * - [index(vector)]: 将向量索引转换为线性索引结果
 *   Convert vector index to linear index
 * - [vector(index)]: 将线性索引转换为向量索引结果
 *   Convert linear index to vector index
 *
 * 存储顺序：
 * Storage order:
 * - [RowMajor]: 行主序（C 风格），最后一维变化最快
 *   Row-major (C style), last dimension varies fastest
 * - [ColumnMajor]: 列主序（Fortran 风格），第一维变化最快
 *   Column-major (Fortran style), first dimension varies fastest
 *
 * 示例：
 * Example:
 * ```kotlin
 * // 创建 2x3x4 的形状
 * // Create a 2x3x4 shape
 * val shape = Shape3(2, 3, 4)
 *
 * // 向量索引转线性索引
 * // Vector index to linear index
 * val linearIdx = shape.index(intArrayOf(1, 2, 3)).value
 *
 * // 线性索引转向量索引
 * // Linear index to vector index
 * val vectorIdx = shape.vector(15).value
 * ```
 * @see MultiArray
 * @see StorageOrder
 */
package fuookami.ospf.kotlin.multiarray

import kotlin.ConsistentCopyVisibility
import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 维度不匹配异常
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
 * - RowMajor: 行主序（C 风格），最后一个维度变化最快
 *   Row-major (C style), last dimension varies fastest
 * - ColumnMajor: 列主序（Fortran 风格），第一个维度变化最快
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
 * 定义多维数组形状的基本操作。
 * Defines basic operations for multi-dimensional array shapes.
 */
interface Shape {
    /**
     * 维度数量
     * Number of dimensions
     */
    val dimension: Int

    /**
     * 维度数量（无符号）
     * Number of dimensions (unsigned)
     */
    val udimension: ULong get() = dimension.toULong()

    /**
     * 元素总数
     * Total number of elements
     */
    val size: Int

    /**
     * 元素总数（无符号）
     * Total number of elements (unsigned)
     */
    val usize: ULong get() = size.toULong()

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
     * 获取指定维度的长度
     * Get the length of the specified dimension
     */
    operator fun get(index: Int): Int

    /**
     * 将向量索引转换为线性索引
     * Convert vector index to linear index
     */
    fun index(vector: IntArray): Ret<Int>

    /**
     * 安全转换向量索引
     * Safely converts vector index to linear index
     */
    fun indexSafe(vector: IntArray): Ret<Int> {
        return index(vector)
    }

    /**
     * 尝试转换向量索引
     * Tries to convert vector index to linear index
     */
    fun indexOrNull(vector: IntArray): Int? {
        return index(vector).value
    }

    /**
     * 将线性索引转换为向量索引
     * Convert linear index to vector index
     */
    fun vector(index: Int): Ret<IntArray>

    /**
     * 安全转换线性索引
     * Safely converts linear index to vector index
     */
    fun vectorSafe(index: Int): Ret<IntArray> {
        return vector(index)
    }

    /**
     * 尝试转换线性索引
     * Tries to convert linear index to vector index
     */
    fun vectorOrNull(index: Int): IntArray? {
        return vector(index).value
    }

    /**
     * 检查是否为空
     * Check if empty
     */
    fun isEmpty(): Boolean = size == 0

    /**
     * 获取下一个向量索引
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
     * 获取指定维度的步长
     * Get the stride for the specified dimension
     */
    fun offset(dimension: Int): Ret<Int> {
        return offsetSafe(dimension)
    }

    /**
     * 安全获取指定维度的步长
     * Safely gets the stride for the specified dimension
     */
    fun offsetSafe(dimension: Int): Ret<Int> {
        if (dimension >= this.dimension || dimension < 0) {
            return Failed(
                code = ErrorCode.IllegalArgument,
                message = "维度不匹配：期望 0..${this.dimension - 1}，实际 $dimension / Dimension mismatch: expected 0..${this.dimension - 1}, got $dimension"
            )
        }
        return Ok(offsets[dimension])
    }

    /**
     * 尝试获取指定维度的步长
     * Tries to get the stride for the specified dimension
     */
    fun offsetOrNull(dimension: Int): Int? {
        return offsetSafe(dimension).value
    }

    /**
     * 计算实际索引
     * Calculate actual index
     *
     * 处理负数索引（从末尾计数）。
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
     * 创建零向量
     * Create zero vector
     */
    fun zero(): IntArray = IntArray(dimension)

    /**
     * 将虚拟索引转换为迭代器向量
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
     * 将映射向量转换为迭代器向量
     * Convert map vector to iterator vector
     */
    fun mapToIteratorVector(mapVector: MapVector): IteratorVector {
        return mapVector.mapIndexed { i, mapIndex ->
            when (mapIndex) {
                is MapIndex.Dummy -> mapIndex.dummy.iteratorOf(this, i)
                is MapIndex.Map -> {
                    // 映射索引：创建全范围迭代器
                    DummyIndexIterator.Continuous(0 until this[mapIndex.index])
                }
            }
        }
    }

    /**
     * 从任意类型数组创建虚拟向量
     * Create dummy vector from any type array
     */
    fun dummyVector(vararg v: Any): Ret<DummyVector> {
        return dummyVectorSafe(*v)
    }

    /**
     * 安全创建虚拟向量
     * Safely creates dummy vector
     */
    fun dummyVectorSafe(vararg v: Any): Ret<DummyVector> {
        if (v.size != dimension) {
            return Failed(
                code = ErrorCode.IllegalArgument,
                message = "虚拟向量维度不匹配：期望 $dimension，实际 ${v.size} / Dummy vector dimension mismatch: expected $dimension, got ${v.size}"
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

                else -> {
                    return Failed(
                        code = ErrorCode.IllegalArgument,
                        message = "未知虚拟索引类型：${index.javaClass.kotlin} / Unknown dummy index type: ${index.javaClass.kotlin}"
                    )
                }
            }
        }
        return Ok(vector)
    }

    /**
     * 尝试创建虚拟向量
     * Tries to create dummy vector
     */
    fun dummyVectorOrNull(vararg v: Any): DummyVector? {
        return dummyVectorSafe(*v).value
    }
}

/**
 * 构建维度不匹配的失败结果。
 * Build failure result for dimension mismatch.
 *
 * @param dimension 期望维度 / Expected dimension
 * @param vectorDimension 实际维度 / Actual dimension
 * @return 失败的 Ret 结果 / Failed Ret result
 */
private fun <T> dimensionMismatchingFailure(
    dimension: Int,
    vectorDimension: Int
): Ret<T> {
    return Failed(
        ErrorCode.IllegalArgument,
        "维度不匹配：期望 $dimension，实际 $vectorDimension / Dimension mismatch: expected $dimension, got $vectorDimension"
    )
}

/**
 * 构建形状索引越界的失败结果。
 * Build failure result for shape index out of bounds.
 *
 * @param dimension 越界维度 / Out-of-bounds dimension
 * @param length 维度长度 / Dimension length
 * @param vectorIndex 实际索引 / Actual index
 * @return 失败的 Ret 结果 / Failed Ret result
 */
private fun <T> outOfShapeFailure(
    dimension: Int,
    length: Int,
    vectorIndex: Int
): Ret<T> {
    return Failed(
        ErrorCode.IllegalArgument,
        "形状索引越界：第 $dimension 维长度为 $length，实际索引 $vectorIndex / Shape index out of bounds: dimension $dimension length is $length, got $vectorIndex"
    )
}

/**
 * 构建线性索引越界的失败结果。
 * Build failure result for linear index out of bounds.
 *
 * @param index 实际索引 / Actual index
 * @param size 数组大小 / Array size
 * @return 失败的 Ret 结果 / Failed Ret result
 */
private fun <T> linearIndexOutOfBoundsFailure(
    index: Int,
    size: Int
): Ret<T> {
    return Failed(
        ErrorCode.IllegalArgument,
        "线性索引越界：索引 $index 不在形状大小 $size 内 / Linear index out of bounds: index $index is outside shape size $size"
    )
}

/**
 * 不校验地转换向量索引
 * Converts vector index without validation
 *
 * @param vector 向量索引 / Vector index
 * @return 线性索引；仅应在调用方已保证索引合法时使用 / Linear index; use only when caller has guaranteed validity
 */
fun Shape.indexUnchecked(vector: IntArray): Int {
    return indexOrNull(vector) ?: -1
}

/**
 * 不校验地转换线性索引
 * Converts linear index without validation
 *
 * @param index 线性索引 / Linear index
 * @return 向量索引；仅应在调用方已保证索引合法时使用 / Vector index; use only when caller has guaranteed validity
 */
fun Shape.vectorUnchecked(index: Int): IntArray {
    return vectorOrNull(index) ?: IntArray(dimension)
}

/**
 * 不校验地创建虚拟向量
 * Creates dummy vector without validation
 *
 * @param v 虚拟索引参数 / Dummy index arguments
 * @return 虚拟向量；仅应在调用方已保证参数合法时使用 / Dummy vector; use only when caller has guaranteed validity
 */
fun Shape.dummyVectorUnchecked(vararg v: Any): DummyVector {
    return dummyVectorOrNull(*v) ?: emptyList()
}

/**
 * 一维形状
 * One-dimensional shape
 */
@ConsistentCopyVisibility
data class Shape1 private constructor(
    private val d1: Int,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        /**
         * 使用默认行主序创建一维形状
         * Create 1D shape with default row-major order
         *
         * @param d1 第一维度长度 / Length of the first dimension
         * @return 一维形状实例 / 1D shape instance
         */
        operator fun invoke(d1: Int): Shape1 = Shape1(d1, StorageOrder.Default)

        /**
         * 使用默认行主序从 ULong 创建一维形状
         * Create 1D shape from ULong with default row-major order
         *
         * @param d1 第一维度长度（ULong） / Length of the first dimension (ULong)
         * @return 一维形状实例 / 1D shape instance
         */
        operator fun invoke(d1: ULong): Shape1 = Shape1(d1.toInt(), StorageOrder.Default)

        /**
         * 使用默认行主序从集合大小创建一维形状
         * Create 1D shape from collection size with default row-major order
         *
         * @param d1 用于推断维度大小的集合 / Collection used to infer dimension size
         * @return 一维形状实例 / 1D shape instance
         */
        operator fun invoke(d1: Collection<*>): Shape1 = Shape1(d1.size, StorageOrder.Default)

        /**
         * 使用指定存储顺序创建一维形状
         * Create 1D shape with specified storage order
         *
         * @param d1 第一维度长度 / Length of the first dimension
         * @param order 存储顺序 / Storage order
         * @return 一维形状实例 / 1D shape instance
         */
        fun withOrder(d1: Int, order: StorageOrder): Shape1 = Shape1(d1, order)

        /**
         * 使用指定存储顺序从 ULong 创建一维形状
         * Create 1D shape from ULong with specified storage order
         *
         * @param d1 第一维度长度（ULong） / Length of the first dimension (ULong)
         * @param order 存储顺序 / Storage order
         * @return 一维形状实例 / 1D shape instance
         */
        fun withOrder(d1: ULong, order: StorageOrder): Shape1 = Shape1(d1.toInt(), order)
    }

    override val dimension = 1
    override val size by ::d1
    private val dimensions = intArrayOf(d1)

    override val offsets: IntArray by lazy { intArrayOf(1) }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override operator fun get(index: Int): Int {
        return dimensions[index]
    }

    /** 将索引向量转换为线性索引 / Convert index vector to linear index */
    override fun index(vector: IntArray): Ret<Int> {
        return when (vector.size) {
            1 -> if (vector[0] < 0 || vector[0] >= d1) {
                outOfShapeFailure(
                    dimension = 0,
                    length = d1,
                    vectorIndex = vector[0]
                )
            } else {
                Ok(vector[0] * offsets[0])
            }

            else -> dimensionMismatchingFailure(
                dimension = 1,
                vectorDimension = vector.size
            )
        }
    }

    /** 将线性索引转换为索引向量 / Convert linear index to index vector */
    override fun vector(index: Int): Ret<IntArray> {
        return if (index < 0 || index >= d1) {
            linearIndexOutOfBoundsFailure(
                index = index,
                size = d1
            )
        } else {
            Ok(intArrayOf(index / offsets[0]))
        }
    }

    /**
     * 使用指定存储顺序创建一维形状副本
     * Create 1D shape copy with specified storage order
     *
     * @param order 存储顺序 / Storage order
     * @return 使用指定存储顺序的一维形状副本 / 1D shape copy with specified storage order
     */
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
        /**
         * 使用默认行主序创建二维形状
         * Create 2D shape with default row-major order
         *
         * @param d1 第一维度长度 / Length of the first dimension
         * @param d2 第二维度长度 / Length of the second dimension
         * @return 二维形状实例 / 2D shape instance
         */
        operator fun invoke(d1: Int, d2: Int): Shape2 = Shape2(d1, d2, StorageOrder.Default)

        /**
         * 使用默认行主序从 ULong 创建二维形状
         * Create 2D shape from ULong with default row-major order
         *
         * @param d1 第一维度长度（ULong） / Length of the first dimension (ULong)
         * @param d2 第二维度长度（ULong） / Length of the second dimension (ULong)
         * @return 二维形状实例 / 2D shape instance
         */
        operator fun invoke(d1: ULong, d2: ULong): Shape2 = Shape2(d1.toInt(), d2.toInt(), StorageOrder.Default)

        /**
         * 使用默认行主序从集合大小创建二维形状
         * Create 2D shape from collection sizes with default row-major order
         *
         * @param d1 用于推断第一维度大小的集合 / Collection used to infer first dimension size
         * @param d2 用于推断第二维度大小的集合 / Collection used to infer second dimension size
         * @return 二维形状实例 / 2D shape instance
         */
        operator fun invoke(d1: Collection<*>, d2: Collection<*>): Shape2 = Shape2(d1.size, d2.size, StorageOrder.Default)

        /**
         * 使用指定存储顺序创建二维形状
         * Create 2D shape with specified storage order
         *
         * @param d1 第一维度长度 / Length of the first dimension
         * @param d2 第二维度长度 / Length of the second dimension
         * @param order 存储顺序 / Storage order
         * @return 二维形状实例 / 2D shape instance
         */
        fun withOrder(d1: Int, d2: Int, order: StorageOrder): Shape2 = Shape2(d1, d2, order)

        /**
         * 使用指定存储顺序从 ULong 创建二维形状
         * Create 2D shape from ULong with specified storage order
         *
         * @param d1 第一维度长度（ULong） / Length of the first dimension (ULong)
         * @param d2 第二维度长度（ULong） / Length of the second dimension (ULong)
         * @param order 存储顺序 / Storage order
         * @return 二维形状实例 / 2D shape instance
         */
        fun withOrder(d1: ULong, d2: ULong, order: StorageOrder): Shape2 = Shape2(d1.toInt(), d2.toInt(), order)
    }

    private val totalSize by lazy { d1 * d2 }
    override val dimension = 2
    override val size get() = totalSize
    private val dimensions = intArrayOf(d1, d2)

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> intArrayOf(d2, 1)
            StorageOrder.ColumnMajor -> intArrayOf(1, d1)
        }
    }

    override operator fun get(index: Int): Int {
        return dimensions[index]
    }

    /** 将索引向量转换为线性索引 / Convert index vector to linear index */
    override fun index(vector: IntArray): Ret<Int> {
        return when (vector.size) {
            2 -> {
                if (vector[0] < 0 || vector[0] >= d1) {
                    outOfShapeFailure(
                        dimension = 0,
                        length = d1,
                        vectorIndex = vector[0]
                    )
                } else if (vector[1] < 0 || vector[1] >= d2) {
                    outOfShapeFailure(
                        dimension = 1,
                        length = d2,
                        vectorIndex = vector[1]
                    )
                } else {
                    Ok(vector[0] * offsets[0] + vector[1] * offsets[1])
                }
            }

            else -> dimensionMismatchingFailure(
                dimension = 2,
                vectorDimension = vector.size
            )
        }
    }

    /** 将线性索引转换为索引向量 / Convert linear index to index vector */
    override fun vector(index: Int): Ret<IntArray> {
        return if (index < 0 || index >= totalSize) {
            linearIndexOutOfBoundsFailure(
                index = index,
                size = totalSize
            )
        } else {
            Ok(when (storageOrder) {
                StorageOrder.RowMajor -> {
                    intArrayOf(index / offsets[0], index % offsets[0] / offsets[1])
                }

                StorageOrder.ColumnMajor -> {
                    // For ColumnMajor: index = v[0] + v[1] * d1 / 列主序公式：index = v[0] + v[1] * d1
                    // So: v[0] = index % d1, v[1] = index / d1 / 因此：v[0] = index % d1, v[1] = index / d1
                    intArrayOf(index % d1, index / d1)
                }
            })
        }
    }

    /**
     * 使用指定存储顺序创建二维形状副本
     * Create 2D shape copy with specified storage order
     *
     * @param order 存储顺序 / Storage order
     * @return 使用指定存储顺序的二维形状副本 / 2D shape copy with specified storage order
     */
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
        /**
         * 使用默认行主序创建三维形状
         * Create 3D shape with default row-major order
         *
         * @param d1 第一维度长度 / Length of the first dimension
         * @param d2 第二维度长度 / Length of the second dimension
         * @param d3 第三维度长度 / Length of the third dimension
         * @return 三维形状实例 / 3D shape instance
         */
        operator fun invoke(d1: Int, d2: Int, d3: Int): Shape3 = Shape3(d1, d2, d3, StorageOrder.Default)

        /**
         * 使用默认行主序从 ULong 创建三维形状
         * Create 3D shape from ULong with default row-major order
         *
         * @param d1 第一维度长度（ULong） / Length of the first dimension (ULong)
         * @param d2 第二维度长度（ULong） / Length of the second dimension (ULong)
         * @param d3 第三维度长度（ULong） / Length of the third dimension (ULong)
         * @return 三维形状实例 / 3D shape instance
         */
        operator fun invoke(d1: ULong, d2: ULong, d3: ULong): Shape3 = Shape3(d1.toInt(), d2.toInt(), d3.toInt(), StorageOrder.Default)

        /**
         * 使用默认行主序从集合大小创建三维形状
         * Create 3D shape from collection sizes with default row-major order
         *
         * @param d1 用于推断第一维度大小的集合 / Collection used to infer first dimension size
         * @param d2 用于推断第二维度大小的集合 / Collection used to infer second dimension size
         * @param d3 用于推断第三维度大小的集合 / Collection used to infer third dimension size
         * @return 三维形状实例 / 3D shape instance
         */
        operator fun invoke(d1: Collection<*>, d2: Collection<*>, d3: Collection<*>): Shape3 = Shape3(d1.size, d2.size, d3.size, StorageOrder.Default)

        /**
         * 使用指定存储顺序创建三维形状
         * Create 3D shape with specified storage order
         *
         * @param d1 第一维度长度 / Length of the first dimension
         * @param d2 第二维度长度 / Length of the second dimension
         * @param d3 第三维度长度 / Length of the third dimension
         * @param order 存储顺序 / Storage order
         * @return 三维形状实例 / 3D shape instance
         */
        fun withOrder(d1: Int, d2: Int, d3: Int, order: StorageOrder): Shape3 = Shape3(d1, d2, d3, order)

        /**
         * 使用指定存储顺序从 ULong 创建三维形状
         * Create 3D shape from ULong with specified storage order
         *
         * @param d1 第一维度长度（ULong） / Length of the first dimension (ULong)
         * @param d2 第二维度长度（ULong） / Length of the second dimension (ULong)
         * @param d3 第三维度长度（ULong） / Length of the third dimension (ULong)
         * @param order 存储顺序 / Storage order
         * @return 三维形状实例 / 3D shape instance
         */
        fun withOrder(d1: ULong, d2: ULong, d3: ULong, order: StorageOrder): Shape3 = Shape3(d1.toInt(), d2.toInt(), d3.toInt(), order)
    }

    private val totalSize by lazy { d1 * d2 * d3 }
    override val dimension = 3
    override val size get() = totalSize
    private val dimensions = intArrayOf(d1, d2, d3)

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> intArrayOf(d2 * d3, d3, 1)
            StorageOrder.ColumnMajor -> intArrayOf(1, d1, d1 * d2)
        }
    }

    override operator fun get(index: Int): Int {
        return dimensions[index]
    }

    /** 将索引向量转换为线性索引 / Convert index vector to linear index */
    override fun index(vector: IntArray): Ret<Int> {
        return when (vector.size) {
            3 -> {
                if (vector[0] < 0 || vector[0] >= d1) {
                    outOfShapeFailure(
                        dimension = 0,
                        length = d1,
                        vectorIndex = vector[0]
                    )
                } else if (vector[1] < 0 || vector[1] >= d2) {
                    outOfShapeFailure(
                        dimension = 1,
                        length = d2,
                        vectorIndex = vector[1]
                    )
                } else if (vector[2] < 0 || vector[2] >= d3) {
                    outOfShapeFailure(
                        dimension = 2,
                        length = d3,
                        vectorIndex = vector[2]
                    )
                } else {
                    Ok(vector[0] * offsets[0] + vector[1] * offsets[1] + vector[2] * offsets[2])
                }
            }

            else -> dimensionMismatchingFailure(
                dimension = 3,
                vectorDimension = vector.size
            )
        }
    }

    /** 将线性索引转换为索引向量 / Convert linear index to index vector */
    override fun vector(index: Int): Ret<IntArray> {
        return if (index < 0 || index >= totalSize) {
            linearIndexOutOfBoundsFailure(
                index = index,
                size = totalSize
            )
        } else {
            Ok(when (storageOrder) {
                StorageOrder.RowMajor -> {
                    var currentIndex = index
                    intArrayOf(
                        currentIndex / offsets[0],
                        currentIndex % offsets[0] / offsets[1],
                        currentIndex % offsets[1]
                    )
                }

                StorageOrder.ColumnMajor -> {
                    var currentIndex = index
                    val v0 = currentIndex % d1
                    currentIndex /= d1
                    val v1 = currentIndex % d2
                    currentIndex /= d2
                    intArrayOf(v0, v1, currentIndex)
                }
            })
        }
    }

    /**
     * 使用指定存储顺序创建三维形状副本
     * Create 3D shape copy with specified storage order
     *
     * @param order 存储顺序 / Storage order
     * @return 使用指定存储顺序的三维形状副本 / 3D shape copy with specified storage order
     */
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
        /**
         * 使用默认行主序创建四维形状
         * Create 4D shape with default row-major order
         *
         * @param d1 第一维度长度 / Length of the first dimension
         * @param d2 第二维度长度 / Length of the second dimension
         * @param d3 第三维度长度 / Length of the third dimension
         * @param d4 第四维度长度 / Length of the fourth dimension
         * @return 四维形状实例 / 4D shape instance
         */
        operator fun invoke(d1: Int, d2: Int, d3: Int, d4: Int): Shape4 = Shape4(d1, d2, d3, d4, StorageOrder.Default)

        /**
         * 使用默认行主序从 ULong 创建四维形状
         * Create 4D shape from ULong with default row-major order
         *
         * @param d1 第一维度长度（ULong） / Length of the first dimension (ULong)
         * @param d2 第二维度长度（ULong） / Length of the second dimension (ULong)
         * @param d3 第三维度长度（ULong） / Length of the third dimension (ULong)
         * @param d4 第四维度长度（ULong） / Length of the fourth dimension (ULong)
         * @return 四维形状实例 / 4D shape instance
         */
        operator fun invoke(d1: ULong, d2: ULong, d3: ULong, d4: ULong): Shape4 = Shape4(d1.toInt(), d2.toInt(), d3.toInt(), d4.toInt(), StorageOrder.Default)

        /**
         * 使用默认行主序从集合大小创建四维形状
         * Create 4D shape from collection sizes with default row-major order
         *
         * @param d1 用于推断第一维度大小的集合 / Collection used to infer first dimension size
         * @param d2 用于推断第二维度大小的集合 / Collection used to infer second dimension size
         * @param d3 用于推断第三维度大小的集合 / Collection used to infer third dimension size
         * @param d4 用于推断第四维度大小的集合 / Collection used to infer fourth dimension size
         * @return 四维形状实例 / 4D shape instance
         */
        operator fun invoke(d1: Collection<*>, d2: Collection<*>, d3: Collection<*>, d4: Collection<*>): Shape4 = Shape4(d1.size, d2.size, d3.size, d4.size, StorageOrder.Default)

        /**
         * 使用指定存储顺序创建四维形状
         * Create 4D shape with specified storage order
         *
         * @param d1 第一维度长度 / Length of the first dimension
         * @param d2 第二维度长度 / Length of the second dimension
         * @param d3 第三维度长度 / Length of the third dimension
         * @param d4 第四维度长度 / Length of the fourth dimension
         * @param order 存储顺序 / Storage order
         * @return 四维形状实例 / 4D shape instance
         */
        fun withOrder(d1: Int, d2: Int, d3: Int, d4: Int, order: StorageOrder): Shape4 = Shape4(d1, d2, d3, d4, order)

        /**
         * 使用指定存储顺序从 ULong 创建四维形状
         * Create 4D shape from ULong with specified storage order
         *
         * @param d1 第一维度长度（ULong） / Length of the first dimension (ULong)
         * @param d2 第二维度长度（ULong） / Length of the second dimension (ULong)
         * @param d3 第三维度长度（ULong） / Length of the third dimension (ULong)
         * @param d4 第四维度长度（ULong） / Length of the fourth dimension (ULong)
         * @param order 存储顺序 / Storage order
         * @return 四维形状实例 / 4D shape instance
         */
        fun withOrder(d1: ULong, d2: ULong, d3: ULong, d4: ULong, order: StorageOrder): Shape4 = Shape4(d1.toInt(), d2.toInt(), d3.toInt(), d4.toInt(), order)
    }

    private val totalSize by lazy { d1 * d2 * d3 * d4 }
    override val dimension = 4
    override val size get() = totalSize
    private val dimensions = intArrayOf(d1, d2, d3, d4)

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> intArrayOf(d2 * d3 * d4, d3 * d4, d4, 1)
            StorageOrder.ColumnMajor -> intArrayOf(1, d1, d1 * d2, d1 * d2 * d3)
        }
    }

    override operator fun get(index: Int): Int {
        return dimensions[index]
    }

    /** 将索引向量转换为线性索引 / Convert index vector to linear index */
    override fun index(vector: IntArray): Ret<Int> {
        return when (vector.size) {
            4 -> {
                if (vector[0] < 0 || vector[0] >= d1) {
                    outOfShapeFailure(
                        dimension = 0,
                        length = d1,
                        vectorIndex = vector[0]
                    )
                } else if (vector[1] < 0 || vector[1] >= d2) {
                    outOfShapeFailure(
                        dimension = 1,
                        length = d2,
                        vectorIndex = vector[1]
                    )
                } else if (vector[2] < 0 || vector[2] >= d3) {
                    outOfShapeFailure(
                        dimension = 2,
                        length = d3,
                        vectorIndex = vector[2]
                    )
                } else if (vector[3] < 0 || vector[3] >= d4) {
                    outOfShapeFailure(
                        dimension = 3,
                        length = d4,
                        vectorIndex = vector[3]
                    )
                } else {
                    Ok(vector[0] * offsets[0] + vector[1] * offsets[1] + vector[2] * offsets[2] + vector[3] * offsets[3])
                }
            }

            else -> dimensionMismatchingFailure(
                dimension = 4,
                vectorDimension = vector.size
            )
        }
    }

    /** 将线性索引转换为索引向量 / Convert linear index to index vector */
    override fun vector(index: Int): Ret<IntArray> {
        return if (index < 0 || index >= totalSize) {
            linearIndexOutOfBoundsFailure(
                index = index,
                size = totalSize
            )
        } else {
            Ok(when (storageOrder) {
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
                    var currentIndex = index
                    val v0 = currentIndex % d1
                    currentIndex /= d1
                    val v1 = currentIndex % d2
                    currentIndex /= d2
                    val v2 = currentIndex % d3
                    currentIndex /= d3
                    intArrayOf(v0, v1, v2, currentIndex)
                }
            })
        }
    }

    /**
     * 使用指定存储顺序创建四维形状副本
     * Create 4D shape copy with specified storage order
     *
     * @param order 存储顺序 / Storage order
     * @return 使用指定存储顺序的四维形状副本 / 4D shape copy with specified storage order
     */
    fun withStorageOrder(order: StorageOrder): Shape4 = Shape4(d1, d2, d3, d4, order)
}

/**
 * 动态维度形状
 * Dynamic dimension shape
 *
 * @property shape 各维度长度的数组 / Array of dimension lengths
 * @property storageOrder 存储顺序 / Storage order
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
                require(l >= 0) { "Dimension cannot be negative: $l" }
                ret *= l
            }
            return ret
        }

        /**
         * 计算行主序偏移量
         * Calculate row-major offsets
         *
         * @param shape 各维度长度的数组 / Array of dimension lengths
         * @return 行主序偏移量数组 / Row-major offset array
         */
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

        /**
         * 计算列主序偏移量
         * Calculate column-major offsets
         *
         * @param shape 各维度长度的数组 / Array of dimension lengths
         * @return 列主序偏移量数组 / Column-major offset array
         */
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

        /**
         * 使用默认行主序从 IntArray 创建动态形状
         * Create dynamic shape from IntArray with default row-major order
         *
         * @param shape 各维度长度的数组 / Array of dimension lengths
         * @return 动态形状实例 / Dynamic shape instance
         */
        operator fun invoke(shape: IntArray): DynShape {
            // Defensive copy to prevent external mutation / 防御性拷贝以防止外部修改
            return DynShape(shape.copyOf(), StorageOrder.Default)
        }

        /**
         * 使用默认行主序从 ULong 迭代创建动态形状
         * Create dynamic shape from ULong iterable with default row-major order
         *
         * @param shape 各维度长度的 ULong 可迭代对象 / Iterable of ULong dimension lengths
         * @return 动态形状实例 / Dynamic shape instance
         */
        @JvmName("constructByULongList")
        operator fun invoke(shape: Iterable<ULong>): DynShape = DynShape(shape.map { it.toInt() }.toIntArray(), StorageOrder.Default)

        /**
         * 使用默认行主序从集合大小迭代创建动态形状
         * Create dynamic shape from collection sizes iterable with default row-major order
         *
         * @param shape 各维度大小的集合可迭代对象 / Iterable of collections for dimension sizes
         * @return 动态形状实例 / Dynamic shape instance
         */
        @JvmName("constructByCollectionList")
        operator fun invoke(shape: Iterable<Collection<*>>): DynShape = DynShape(shape.map { it.size }.toIntArray(), StorageOrder.Default)

        /**
         * 使用指定存储顺序从 IntArray 创建动态形状
         * Create dynamic shape from IntArray with specified storage order
         *
         * @param shape 各维度长度的数组 / Array of dimension lengths
         * @param order 存储顺序 / Storage order
         * @return 动态形状实例 / Dynamic shape instance
         */
        fun withOrder(shape: IntArray, order: StorageOrder): DynShape {
            // Defensive copy / 防御性拷贝
            return DynShape(shape.copyOf(), order)
        }

        /**
         * 使用指定存储顺序从 ULong 迭代创建动态形状
         * Create dynamic shape from ULong iterable with specified storage order
         *
         * @param shape 各维度长度的 ULong 可迭代对象 / Iterable of ULong dimension lengths
         * @param order 存储顺序 / Storage order
         * @return 动态形状实例 / Dynamic shape instance
         */
        @JvmName("withOrderFromULongList")
        fun withOrder(shape: Iterable<ULong>, order: StorageOrder): DynShape = DynShape(shape.map { it.toInt() }.toIntArray(), order)

        /**
         * 使用指定存储顺序从集合大小迭代创建动态形状
         * Create dynamic shape from collection sizes iterable with specified storage order
         *
         * @param shape 各维度大小的集合可迭代对象 / Iterable of collections for dimension sizes
         * @param order 存储顺序 / Storage order
         * @return 动态形状实例 / Dynamic shape instance
         */
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

    /** 将索引向量转换为线性索引 / Convert index vector to linear index */
    override fun index(vector: IntArray): Ret<Int> {
        if (dimension != vector.size) {
            return dimensionMismatchingFailure(dimension, vector.size)
        }
        var ret = 0
        for (i in shape.indices) {
            if (vector[i] < 0 || vector[i] >= shape[i]) {
                return outOfShapeFailure(
                    dimension = i,
                    length = shape[i],
                    vectorIndex = vector[i]
                )
            }
            ret += vector[i] * offsets[i]
        }
        return Ok(ret)
    }

    /** 将线性索引转换为索引向量 / Convert linear index to index vector */
    override fun vector(index: Int): Ret<IntArray> {
        return if (index < 0 || index >= totalSize) {
            linearIndexOutOfBoundsFailure(
                index = index,
                size = totalSize
            )
        } else {
            Ok(when (storageOrder) {
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
                        if (i == dimension - 1) {
                            currentIndex
                        } else {
                            val result = currentIndex % shape[i]
                            currentIndex /= shape[i]
                            result
                        }
                    }
                }
            })
        }
    }

    /**
     * 使用指定存储顺序创建动态形状副本
     * Create dynamic shape copy with specified storage order
     *
     * @param order 存储顺序 / Storage order
     * @return 使用指定存储顺序的动态形状副本 / Dynamic shape copy with specified storage order
     */
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
