/**
 * 访问顺序和迭代器模块
 * Access Order and Iterator Module
 *
 * 本模块定义多维数组的访问顺序和迭代器系统。
 * This module defines access order and iterator system for multi-dimensional arrays.
 *
 * 主要组件：
 * Main components:
 * - [AccessOrder]: 访问顺序枚举（行主序/列主序）
 *   Access order enum (RowMajor/ColumnMajor)
 * - [MultiIndexIterator]: 多维索引迭代器
 *   Multi-dimensional index iterator
 * - [MultiIndexSequence]: 多维索引序列
 *   Multi-dimensional index sequence
 *
 * 访问顺序：
 * Access order:
 * - [RowMajor]: 行主序（C 风格），最后一维变化最快
 *   Row-major (C style), last dimension varies fastest
 * - [ColumnMajor]: 列主序（Fortran 风格），第一维变化最快
 *   Column-major (Fortran style), first dimension varies fastest
 *
 * 迭代器特性：
 * Iterator features:
 * - 返回独立快照，后续迭代不影响历史值
 *   Returns independent snapshots, subsequent iterations don't affect historical values
 * - 正确实现 hasNext/next 契约
 *   Properly implements hasNext/next contract
 * - 支持指定访问顺序的迭代
 *   Supports iteration with specified access order
 *
 * 示例：
 * Example:
 * ```kotlin
 * // 按行主序迭代
 * // Iterate in row-major order
 * for (idx in shape.indices(AccessOrder.RowMajor)) {
 *     println(idx.toList())
 * }
 *
 * // 按列主序迭代
 * // Iterate in column-major order
 * for (idx in shape.indices(AccessOrder.ColumnMajor)) {
 *     println(idx.toList())
 * }
 * ```
 *
 * @see Shape
*/
package fuookami.ospf.kotlin.multiarray

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 访问顺序枚举
 * Access order enum
 *
 * 定义多维数组的访问顺序：
 * Defines the access order for multi-dimensional arrays:
 *
 * - RowMajor: 行优先，最后一个维度变化最快（C 风格）
 *   Row-major, last dimension varies fastest (C style)
 * - ColumnMajor: 列优先，第一个维度变化最快（Fortran 风格）
 *   Column-major, first dimension varies fastest (Fortran style)
*/
enum class AccessOrder {
    RowMajor,
    ColumnMajor;

    companion object {
        val Default = RowMajor
    }
}

/**
 * 迭代器位置
 * Iterator position
 *
 * 用于跟踪多维迭代的状态。
 * Used to track the state of multi-dimensional iteration.
 *
 * @param positions 各维度的当前位置 / Current position of each dimension
 * @param exhausted 是否已耗尽 / Whether exhausted
*/
data class IteratorPosition(
    val positions: IntArray,
    val exhausted: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IteratorPosition
        return positions.contentEquals(other.positions)
    }

    override fun hashCode(): Int {
        return positions.contentHashCode()
    }
}

/**
 * 多维索引迭代器
 * Multi-dimensional index iterator
 *
 * 按照指定的访问顺序迭代多维索引。
 * Iterates over multi-dimensional indices in the specified access order.
 *
 * @param shape 数组形状 / Array shape
 * @param accessOrder 访问顺序 / Access order
*/
class MultiIndexIterator(
    private val shape: Shape,
    private val accessOrder: AccessOrder = AccessOrder.Default
) : Iterator<IntArray> {

    private val totalSize = shape.size
    private var current: IntArray? = null
    private var count = 0

    override fun hasNext(): Boolean {
        return count < totalSize
    }

    override fun next(): IntArray {
        if (!hasNext()) {
            throw NoSuchElementException("Iterator has no more elements")
        }

        val current = this.current
        if (current == null) {
            // Initialize as first element - return copy
            val v = IntArray(shape.dimension)
            for (i in v.indices) {
                v[i] = 0
            }
            this.current = v
            count = 1
            return v.copyOf()  // Return independent snapshot
        }

        // Advance to next element
        val next = advance(current)
        if (next == null) {
            count = totalSize
            throw NoSuchElementException("Iterator has no more elements")
        }

        count++
        this.current = next
        return next.copyOf()  // Return independent snapshot
    }

    /**
     * 推进到下一个索引
     * Advance to the next index
     *
     * @param v Current multi-dimensional index to advance / 要推进的当前多维索引
     * @return Next index array, or null if iteration is exhausted / 下一个索引数组，迭代耗尽时返回 null
    */
    private fun advance(v: IntArray): IntArray? {
        when (accessOrder) {
            AccessOrder.RowMajor -> {
                // 从后向前推进
                var i = v.size - 1
                while (i >= 0) {
                    if (v[i] + 1 < shape[i]) {
                        v[i]++
                        return v
                    } else {
                        v[i] = 0
                        i--
                    }
                }
                return null
            }

            AccessOrder.ColumnMajor -> {
                // 从前向后推进
                for (i in v.indices) {
                    if (v[i] + 1 < shape[i]) {
                        v[i]++
                        return v
                    } else {
                        v[i] = 0
                    }
                }
                return null
            }
        }
    }

    /**
     * 获取已迭代的元素数量
     * Get the count of iterated elements
     *
     * @return Number of elements iterated so far / 已迭代的元素数量
    */
    fun count(): Int = count

    /**
     * 重置迭代器
     * Reset the iterator
    */
    fun reset() {
        current = null
        count = 0
    }
}

/**
 * 将存储顺序转换为访问顺序
 * Convert storage order to access order
 *
 * @return 对应的访问顺序 / Corresponding access order
*/
private fun StorageOrder.toAccessOrder(): AccessOrder {
    return when (this) {
        StorageOrder.RowMajor -> AccessOrder.RowMajor
        StorageOrder.ColumnMajor -> AccessOrder.ColumnMajor
    }
}

/**
 * 按照存储顺序重新排列元素列表
 * Reorder element list according to storage order
 *
 * @param shape 数组形状 / Array shape
 * @param list 元素列表 / Element list
 * @param accessOrder 列表的访问顺序 / Access order of the list
 * @return 按存储顺序排列的数组 / Array reordered to storage order
*/
private fun <T : Any, S : Shape> reorderToStorageOrder(
    shape: S,
    list: List<T>,
    accessOrder: AccessOrder
): Array<Any?> {
    val reordered = arrayOfNulls<Any>(shape.size)
    var inputIndex = 0
    for (vector in shape.iterate(accessOrder)) {
        reordered[shape.indexUnchecked(vector)] = list[inputIndex++]
    }
    return reordered
}

/**
 * 多维索引序列
 * Multi-dimensional index sequence
 *
 * @param shape 数组形状 / Array shape
 * @param accessOrder 访问顺序 / Access order
*/
class MultiIndexSequence(
    private val shape: Shape,
    private val accessOrder: AccessOrder = AccessOrder.Default
) : Sequence<IntArray> {
    override fun iterator(): MultiIndexIterator {
        return MultiIndexIterator(shape, accessOrder)
    }
}

/**
 * 创建多维索引序列
 * Create multi-dimensional index sequence
 *
 * @param order 访问顺序 / Access order
 * @return 多维索引序列 / Multi-dimensional index sequence
*/
fun Shape.indices(order: AccessOrder = AccessOrder.Default): MultiIndexSequence {
    return MultiIndexSequence(this, order)
}

/**
 * 使用指定访问顺序迭代形状
 * Iterate over shape with specified access order
 *
 * @param order 访问顺序 / Access order
 * @return 索引序列 / Index sequence
*/
fun Shape.iterate(order: AccessOrder = AccessOrder.Default): Sequence<IntArray> {
    return MultiIndexSequence(this, order)
}

/**
 * 使用指定访问顺序迭代虚拟向量
 * Iterate over dummy vector with specified access order
 *
 * @param shape 数组形状 / Array shape
 * @param accessOrder 访问顺序 / Access order
 * @return 索引序列 / Index sequence
*/
fun DummyVector.iterateWithOrder(
    shape: Shape,
    accessOrder: AccessOrder = AccessOrder.Default
): Sequence<IntArray> = sequence {
    val iteratorVector = shape.dummyToIteratorVector(this@iterateWithOrder)
    val lengths = iteratorVector.map { it.len() }.toIntArray()

    if (lengths.isEmpty() || lengths.any { it == 0 }) {
        return@sequence
    }

    val current = IntArray(lengths.size)
    var exhausted = false

    // 初始化
    for (i in current.indices) {
        current[i] = 0
    }

    while (!exhausted) {
        // 计算当前向量
        val vector = IntArray(shape.dimension)
        for (i in shape.indices) {
            vector[i] = iteratorVector[i].get(current[i]) ?: 0
        }
        yield(vector)

        // 推进
        when (accessOrder) {
            AccessOrder.RowMajor -> {
                var i = current.size - 1
                while (i >= 0) {
                    if (current[i] + 1 < lengths[i]) {
                        current[i]++
                        break
                    } else {
                        current[i] = 0
                        i--
                    }
                }
                if (i < 0) {
                    exhausted = true
                }
            }

            AccessOrder.ColumnMajor -> {
                var i = 0
                while (i < current.size) {
                    if (current[i] + 1 < lengths[i]) {
                        current[i]++
                        break
                    } else {
                        current[i] = 0
                        i++
                    }
                }
                if (i >= current.size) {
                    exhausted = true
                }
            }
        }
    }
}

/**
 * 使用指定访问顺序迭代 MultiArrayView
 * Iterate over MultiArrayView with specified access order
 *
 * @param accessOrder 访问顺序 / Access order
 * @return 元素序列 / Element sequence
*/
fun <T : Any, S : Shape> MultiArrayView<T, S>.iterWithOrder(
    accessOrder: AccessOrder = AccessOrder.Default
): Sequence<T> = sequence {
    for (vec in shape.iterate(accessOrder)) {
        when (val result = this@iterWithOrder[vec]) {
            is Ok -> yield(result.value)
            is Failed -> {}
            is Fatal -> {}
        }
    }
}

/**
 * 使用指定访问顺序迭代 AbstractMultiArray
 * Iterate over AbstractMultiArray with specified access order
 *
 * @param accessOrder 访问顺序 / Access order
 * @return 元素序列 / Element sequence
*/
fun <T : Any, S : Shape> AbstractMultiArray<T, S>.iterWithOrder(
    accessOrder: AccessOrder = AccessOrder.Default
): Sequence<T> = sequence {
    for (vec in shape.iterate(accessOrder)) {
        yield(this@iterWithOrder[vec])
    }
}

/**
 * 使用指定访问顺序进行枚举迭代
 * Enumerate iteration with specified access order
 *
 * @param accessOrder 访问顺序 / Access order
 * @return (线性索引, 向量坐标, 元素) 三元组序列 / (linear index, vector, element) triple sequence
*/
fun <T : Any, S : Shape> AbstractMultiArray<T, S>.enumerateWithOrder(
    accessOrder: AccessOrder = AccessOrder.Default
): Sequence<Triple<Int, IntArray, T>> = sequence {
    var linearIndex = 0
    for (vec in shape.iterate(accessOrder)) {
        yield(Triple(linearIndex++, vec, this@enumerateWithOrder[vec]))
    }
}

/**
 * 多维数组展平为列表
 * Flatten multi-dimensional array to list
 *
 * @param accessOrder 访问顺序 / Access order
 * @return 展平后的元素列表 / Flattened element list
*/
fun <T : Any, S : Shape> AbstractMultiArray<T, S>.flatten(
    accessOrder: AccessOrder = AccessOrder.Default
): List<T> {
    return iterWithOrder(accessOrder).toList()
}

/**
 * 从列表创建多维数组
 * Create multi-dimensional array from list
 *
 * @param shape 数组形状 / Array shape
 * @param list 元素列表 / Element list
 * @param accessOrder 列表的访问顺序 / Access order of the list
 * @return 不可变多维数组 / Immutable multi-array
*/
fun <T : Any, S : Shape> MultiArray.Companion.fromList(
    shape: S,
    list: List<T>,
    accessOrder: AccessOrder = AccessOrder.Default
): Ret<MultiArray<T, S>> {
    return fromListSafe(
        shape = shape,
        list = list,
        accessOrder = accessOrder
    )
}

/**
 * 从列表创建多维数组，失败时返回 null
 * Create multi-dimensional array from list, returning null on failure
 *
 * @param shape 数组形状 / Array shape
 * @param list 元素列表 / Element list
 * @param accessOrder 列表的访问顺序 / Access order of the list
 * @return 不可变多维数组或 null / Immutable multi-array or null
*/
fun <T : Any, S : Shape> MultiArray.Companion.fromListOrNull(
    shape: S,
    list: List<T>,
    accessOrder: AccessOrder = AccessOrder.Default
): MultiArray<T, S>? {
    return fromListSafe(
        shape = shape,
        list = list,
        accessOrder = accessOrder
    ).value
}

/**
 * 从列表创建多维数组，失败时返回 Failed
 * Create multi-dimensional array from list, returning Failed on failure
 *
 * @param shape 数组形状 / Array shape
 * @param list 元素列表 / Element list
 * @param accessOrder 列表的访问顺序 / Access order of the list
 * @return 不可变多维数组结果 / Immutable multi-array result
*/
fun <T : Any, S : Shape> MultiArray.Companion.fromListSafe(
    shape: S,
    list: List<T>,
    accessOrder: AccessOrder = AccessOrder.Default
): Ret<MultiArray<T, S>> {
    if (list.size != shape.size) {
        return Failed(
            ErrorCode.IllegalArgument,
            "List size (${list.size}) must match shape size (${shape.size})."
        )
    }

    if (accessOrder == shape.storageOrder.toAccessOrder()) {
        return Ok(MultiArray(shape) { i, _ -> list[i] })
    }

    val reordered = reorderToStorageOrder(shape, list, accessOrder)
    return Ok(MultiArray(shape) { i, _ ->
        @Suppress("UNCHECKED_CAST")
        reordered[i] as T
    })
}

/**
 * 从列表创建可变多维数组
 * Create mutable multi-dimensional array from list
 *
 * @param shape 数组形状 / Array shape
 * @param list 元素列表 / Element list
 * @param accessOrder 列表的访问顺序 / Access order of the list
 * @return 可变多维数组 / Mutable multi-array
*/
fun <T : Any, S : Shape> MutableMultiArray.Companion.fromList(
    shape: S,
    list: List<T>,
    accessOrder: AccessOrder = AccessOrder.Default
): Ret<MutableMultiArray<T, S>> {
    return fromListSafe(
        shape = shape,
        list = list,
        accessOrder = accessOrder
    )
}

/**
 * 从列表创建可变多维数组，失败时返回 null
 * Create mutable multi-dimensional array from list, returning null on failure
 *
 * @param shape 数组形状 / Array shape
 * @param list 元素列表 / Element list
 * @param accessOrder 列表的访问顺序 / Access order of the list
 * @return 可变多维数组或 null / Mutable multi-array or null
*/
fun <T : Any, S : Shape> MutableMultiArray.Companion.fromListOrNull(
    shape: S,
    list: List<T>,
    accessOrder: AccessOrder = AccessOrder.Default
): MutableMultiArray<T, S>? {
    return fromListSafe(
        shape = shape,
        list = list,
        accessOrder = accessOrder
    ).value
}

/**
 * 从列表创建可变多维数组，失败时返回 Failed
 * Create mutable multi-dimensional array from list, returning Failed on failure
 *
 * @param shape 数组形状 / Array shape
 * @param list 元素列表 / Element list
 * @param accessOrder 列表的访问顺序 / Access order of the list
 * @return 可变多维数组结果 / Mutable multi-array result
*/
fun <T : Any, S : Shape> MutableMultiArray.Companion.fromListSafe(
    shape: S,
    list: List<T>,
    accessOrder: AccessOrder = AccessOrder.Default
): Ret<MutableMultiArray<T, S>> {
    if (list.size != shape.size) {
        return Failed(
            ErrorCode.IllegalArgument,
            "List size (${list.size}) must match shape size (${shape.size})."
        )
    }

    if (accessOrder == shape.storageOrder.toAccessOrder()) {
        return Ok(MutableMultiArray(shape) { i, _ -> list[i] })
    }

    val reordered = reorderToStorageOrder(shape, list, accessOrder)
    return Ok(MutableMultiArray(shape) { i, _ ->
        @Suppress("UNCHECKED_CAST")
        reordered[i] as T
    })
}
