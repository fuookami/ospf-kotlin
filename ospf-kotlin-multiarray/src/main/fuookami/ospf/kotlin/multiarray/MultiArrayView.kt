/**
 * 多维数组视图模块
 * Multi-dimensional Array View Module
 *
 * 本模块提供多维数组的视图和切片功能，支持无数据复用的数据访问模式。
 * This module provides view and slice functionality for multi-dimensional arrays,
 * supporting data access patterns without data duplication.
 *
 * 主要类型：
 * Main types:
 * - [MultiArrayView]: 多维数组视图，支持切片和索引
 *   Multi-dimensional array view, supporting slicing and indexing
 * - [MappedMultiArrayView]: 映射视图，支持维度重排和转置
 *   Mapped view, supporting dimension reordering and transposition
 *
 * 视图类型：
 * View types:
 * - **切片视图**: 使用虚拟索引（_a、范围、单个索引）创建子数组视图
 *   Slice view: Create sub-array views using dummy indices (_a, ranges, single indices)
 * - **映射视图**: 使用 MapIndex 进行维度重排，如转置操作
 *   Mapped view: Reorder dimensions using MapIndex, e.g., transpose operations
 *
 * 使用场景：
 * Use cases:
 * - 无复制的数组切片
 *   Array slicing without copying
 * - 维度转置和重排
 *   Dimension transposition and reordering
 * - 子数组访问
 *   Sub-array access
 *
 * 示例：
 * Example:
 * ```kotlin
 * // 创建视图（切片）
 * // Create view (slice)
 * val array = MultiArray.newWith(Shape3(2, 3, 4), 0)
 * val slice = array[_a, 1, _a]  // 所有行的第 1 列的所有深度
 *
 * // 创建转置视图
 * // Create transpose view
 * val transposed = MappedMultiArrayView(array, listOf(
 *     MapIndex.Map(2),  // 维度 2 映射到位置 0
 *     MapIndex.Map(0),  // 维度 0 映射到位置 1
 *     MapIndex.Map(1)   // 维度 1 映射到位置 2
 * ))
 * ```
 *
 * @see MultiArray
 * @see DummyIndex
 * @see MapIndex
 */
package fuookami.ospf.kotlin.multiarray

import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 多维数组视图
 * Multi-dimensional array view
 *
 * 提供对多维数组的切片、索引和视图操作。
 * Provides slicing, indexing, and view operations for multi-dimensional arrays.
 *
 * @param T 元素类型
 * @param S 形状类型
 */
class MultiArrayView<out T : Any, S : Shape>(
    private val origin: AbstractMultiArray<T, S>,
    private val dummyVector: DummyVector
) : Collection<T> {

    constructor(origin: AbstractMultiArray<T, S>) : this(
        origin, (0..<origin.dimension).map { DummyIndex.all() }
    )

    /**
     * 视图的形状
     * Shape of the view
     */
    val shape: DynShape

    /**
     * 迭代器向量
     * Iterator vector
     */
    private val iteratorVector: IteratorVector

    /**
     * 虚拟维度（保持为虚拟索引的维度）
     * Dummy dimensions (dimensions that remain as dummy indices)
     */
    private val dummyDimensions: Set<Int>

    init {
        val shapeList = ArrayList<Int>()
        val dummyDims = HashSet<Int>()

        for ((dimension, dummyIndex) in dummyVector.withIndex()) {
            val len = dummyIndex.lenOf(origin.shape, dimension)
            if (len > 1) {
                shapeList.add(len)
                dummyDims.add(dimension)
            }
        }

        shape = DynShape(shapeList.toIntArray())
        iteratorVector = origin.shape.dummyToIteratorVector(dummyVector)
        dummyDimensions = dummyDims
    }

    override val size: Int get() = shape.size

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean {
        return elements.all { contains(it) }
    }

    override fun contains(element: @UnsafeVariance T): Boolean {
        for (item in this) {
            if (item == element) return true
        }
        return false
    }

    override fun isEmpty(): Boolean = shape.isEmpty()

    override fun iterator(): Iterator<T> {
        return ElementIterator(this)
    }

    /**
     * 通过线性索引获取元素
     * Get element by linear index
     */
    operator fun get(i: Int): T {
        return origin[actualVectorUnchecked(shape.vectorUnchecked(i))]
    }

    /**
     * 通过 ULong 线性索引获取元素
     * Get element by ULong linear index
     */
    operator fun get(i: ULong): T {
        return get(i.toInt())
    }

    /**
     * 通过 Indexed 接口获取元素
     * Get element by Indexed interface
     */
    operator fun get(e: Indexed): T {
        return origin[actualVectorUnchecked(shape.vectorUnchecked(e.index))]
    }

    /**
     * 通过向量索引获取元素
     * Get element by vector index
     */
    @JvmName("getByIntArray")
    operator fun get(v: IntArray): Ret<T> {
        return actualVector(v).map { origin[it] }
    }

    /**
     * 通过可变参数获取元素
     * Get element by vararg
     */
    @JvmName("getByInts")
    operator fun get(vararg v: Int): Ret<T> {
        return actualVector(v).map { origin[it] }
    }

    /**
     * 通过 ULong 迭代获取元素
     * Get element by ULong iterable
     */
    operator fun get(v: Iterable<ULong>): Ret<T> {
        return actualVector(v.map { it.toInt() }.toIntArray()).map { origin[it] }
    }

    /**
     * 通过 Indexed 可变参数获取元素
     * Get element by Indexed vararg
     */
    operator fun get(vararg v: Indexed): Ret<T> {
        return actualVector(v.map { it.index }.toIntArray()).map { origin[it] }
    }

    /**
     * 通过任意类型数组创建子视图
     * Create sub-view by any type array
     */
    operator fun get(vararg v: Any): MultiArrayView<T, S> {
        val newDummyVector = ArrayList<DummyIndex>()
        val subDummyVector = shape.dummyVectorUnchecked(*v)
        var j = 0

        for (i in origin.shape.indices) {
            if (i in dummyDimensions) {
                // 这个维度在视图中保持为虚拟索引
                if (j < subDummyVector.size) {
                    newDummyVector.add(subDummyVector[j])
                }
                j++
            } else {
                // 这个维度已被固定
                newDummyVector.add(dummyVector[i])
            }
        }

        return MultiArrayView(origin, newDummyVector)
    }

    /**
     * 计算实际向量索引
     * Calculate actual vector index
     *
     * 将视图索引转换为原数组的实际索引。
     * Converts view indices to actual indices in the original array.
     */
    private fun actualVector(v: IntArray): Ret<IntArray> {
        if (v.size != shape.dimension) {
            return Failed(
                ErrorCode.IllegalArgument,
                "View index dimension mismatch: expected ${shape.dimension}, got ${v.size}."
            )
        }
        return Ok(actualVectorUnchecked(v))
    }

    private fun actualVectorUnchecked(v: IntArray): IntArray {
        val result = IntArray(origin.dimension)
        var viewIndex = 0

        for (i in origin.shape.indices) {
            if (i in dummyDimensions) {
                // 从迭代器向量获取实际索引
                result[i] = iteratorVector[i].get(v[viewIndex++]) ?: 0
            } else {
                // 维度已被固定，使用虚拟索引的值
                val iter = iteratorVector[i]
                result[i] = when (iter) {
                    is DummyIndexIterator.Single -> iter.index
                    is DummyIndexIterator.Continuous -> iter.range.first
                    is DummyIndexIterator.Discrete -> iter.indices.firstOrNull() ?: 0
                }
            }
        }

        return result
    }

    /**
     * 元素迭代器
     * Element iterator
     */
    private class ElementIterator<out T : Any, S : Shape>(
        private val view: MultiArrayView<T, S>
    ) : Iterator<T> {
        private var currentIndex = 0
        private val size = view.shape.size

        override fun hasNext(): Boolean {
            return currentIndex < size
        }

        override fun next(): T {
            if (currentIndex >= size) {
                throw NoSuchElementException("Iterator has no more elements")
            }
            val result = view[currentIndex]
            currentIndex++
            return result
        }
    }

    /**
     * 转换为字符串表示
     * Convert to string representation
     */
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("MultiArrayView(")
        sb.append("shape=${shape}, ")
        sb.append("originShape=${origin.shape}, ")
        sb.append("size=$size")
        sb.append(")")
        return sb.toString()
    }
}

/**
 * 多维数组映射视图
 * Multi-dimensional array mapped view
 *
 * 用于维度转置和重映射操作。
 * Used for dimension transposition and remapping operations.
 */
class MappedMultiArrayView<out T : Any, S : Shape>(
    private val origin: AbstractMultiArray<T, S>,
    private val mapVector: MapVector
) : Collection<T> {

    init {
        require(mapVector.size == origin.dimension) {
            "Map vector size (${mapVector.size}) must match origin dimension (${origin.dimension})"
        }

        // Validate map indices / 验证映射索引
        val mapIndices = mapVector.filterIsInstance<MapIndex.Map>().map { it.index }

        // Check for duplicates / 检查重复索引
        val uniqueIndices = mapIndices.toSet()
        require(mapIndices.size == uniqueIndices.size) {
            "Duplicate map indices found: ${mapIndices.groupingBy { it }.eachCount().filter { it.value > 1 }}"
        }

        // Check bounds / 检查边界
        require(mapIndices.all { it in origin.shape.indices }) {
            "Out of bounds map index: ${mapIndices.find { it !in origin.shape.indices }}"
        }

        // Check contiguous coverage (0 to k-1) / 检查连续覆盖（0 到 k-1）
        val sortedIndices = mapIndices.sorted()
        require(sortedIndices == (0 until mapIndices.size).toList()) {
            "Non-contiguous map indices: expected 0..${mapIndices.size - 1}, got $sortedIndices"
        }
    }

    val shape: DynShape

    init {
        val shapeList = ArrayList<Int>()
        for (mapIndex in mapVector) {
            when (mapIndex) {
                is MapIndex.Dummy -> {
                    shapeList.add(mapIndex.dummy.lenOf(origin.shape, shapeList.size))
                }

                is MapIndex.Map -> {
                    shapeList.add(origin.shape[mapIndex.index])
                }
            }
        }
        shape = DynShape(shapeList.toIntArray())
    }

    override val size: Int get() = shape.size

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean {
        return elements.all { contains(it) }
    }

    override fun contains(element: @UnsafeVariance T): Boolean {
        for (item in this) {
            if (item == element) return true
        }
        return false
    }

    override fun isEmpty(): Boolean = shape.isEmpty()

    override fun iterator(): Iterator<T> {
        return iterator {
            for (i in 0 until shape.size) {
                yield(origin[mapVectorUnchecked(shape.vectorUnchecked(i))])
            }
        }
    }

    /**
     * 通过线性索引获取元素
     * Get element by linear index
     *
     * @param i 线性索引 / Linear index
     * @return 元素值 / Element value
     */
    operator fun get(i: Int): T {
        return origin[mapVectorUnchecked(shape.vectorUnchecked(i))]
    }

    /**
     * 通过 ULong 线性索引获取元素
     * Get element by ULong linear index
     *
     * @param i ULong 线性索引 / ULong linear index
     * @return 元素值 / Element value
     */
    operator fun get(i: ULong): T {
        return get(i.toInt())
    }

    /**
     * 通过 Indexed 接口获取元素
     * Get element by Indexed interface
     *
     * @param e Indexed 接口实例 / Indexed interface instance
     * @return 元素值 / Element value
     */
    operator fun get(e: Indexed): T {
        return origin[mapVectorUnchecked(shape.vectorUnchecked(e.index))]
    }

    /** 通过 IntArray 向量索引获取元素 / Get element by IntArray vector index */
    @JvmName("getByIntArray")
    operator fun get(v: IntArray): Ret<T> {
        return mapVector(v).map { origin[it] }
    }

    /** 通过 vararg Int 向量索引获取元素 / Get element by vararg Int vector index */
    @JvmName("getByInts")
    operator fun get(vararg v: Int): Ret<T> {
        return mapVector(v).map { origin[it] }
    }

    /**
     * 通过 vararg Any 创建子映射视图
     * Create sub-mapped view by vararg Any indices
     *
     * @param v 索引参数 / Index parameters
     * @return 子映射视图 / Sub-mapped view
     */
    operator fun get(vararg v: Any): MappedMultiArrayView<T, S> {
        val newMapVector = ArrayList<MapIndex>()
        val subDummyVector = shape.dummyVectorUnchecked(*v)
        var j = 0

        for ((i, mapIndex) in mapVector.withIndex()) {
            when (mapIndex) {
                is MapIndex.Dummy -> {
                    newMapVector.add(MapIndex.Dummy(subDummyVector[j]))
                    j++
                }

                is MapIndex.Map -> {
                    newMapVector.add(mapIndex)
                }
            }
        }

        return MappedMultiArrayView(origin, newMapVector)
    }

    /**
     * 将视图索引向量映射为原始数组索引向量
     * Map view index vector to original array index vector
     *
     * @param v 视图索引向量 / View index vector
     * @return 原始数组索引向量 / Original array index vector
     */
    private fun mapVector(v: IntArray): Ret<IntArray> {
        if (v.size != shape.dimension) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Mapped view index dimension mismatch: expected ${shape.dimension}, got ${v.size}."
            )
        }
        return Ok(mapVectorUnchecked(v))
    }

    private fun mapVectorUnchecked(v: IntArray): IntArray {
        val result = IntArray(origin.dimension)
        var viewIndex = 0

        for ((i, mapIndex) in mapVector.withIndex()) {
            when (mapIndex) {
                is MapIndex.Dummy -> {
                    val iter = mapIndex.dummy.iteratorOf(origin.shape, i)
                    result[i] = iter.get(v[viewIndex++]) ?: 0
                }

                is MapIndex.Map -> {
                    result[mapIndex.index] = v[viewIndex++]
                }
            }
        }

        return result
    }
}
