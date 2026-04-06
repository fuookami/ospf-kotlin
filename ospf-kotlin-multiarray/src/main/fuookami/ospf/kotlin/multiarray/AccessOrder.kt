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
 * @author OSPF Kotlin Team
 * @since 1.0.0
 * @see Shape
 */
package fuookami.ospf.kotlin.multiarray

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
 */
class MultiIndexIterator(
    private val shape: Shape,
    private val accessOrder: AccessOrder = AccessOrder.Default
) : Iterator<IntArray> {

    private var current: IntArray? = null
    private var exhausted = false
    private var count = 0

    override fun hasNext(): Boolean {
        if (exhausted || shape.size == 0) {
            return false
        }

        // If not started yet, we have at least one element
        val current = this.current
        if (current == null) {
            return true
        }

        // Check if we can advance from current position
        val temp = current.copyOf()
        val canAdvance = advance(temp) != null
        return canAdvance
    }

    override fun next(): IntArray {
        if (exhausted) {
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
            exhausted = true
            throw NoSuchElementException("Iterator has no more elements")
        }

        count++
        this.current = next
        return next.copyOf()  // Return independent snapshot
    }

    /**
     * 推进到下一个索引
     * Advance to the next index
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
     */
    fun count(): Int = count

    /**
     * 重置迭代器
     * Reset the iterator
     */
    fun reset() {
        current = null
        exhausted = false
        count = 0
    }
}

/**
 * 多维索引序列
 * Multi-dimensional index sequence
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
 */
fun Shape.indices(order: AccessOrder = AccessOrder.Default): MultiIndexSequence {
    return MultiIndexSequence(this, order)
}

/**
 * 使用指定访问顺序迭代形状
 * Iterate over shape with specified access order
 */
fun Shape.iterate(order: AccessOrder = AccessOrder.Default): Sequence<IntArray> {
    return MultiIndexSequence(this, order)
}

/**
 * 使用指定访问顺序迭代虚拟向量
 * Iterate over dummy vector with specified access order
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
 */
fun <T : Any, S : Shape> MultiArrayView<T, S>.iterWithOrder(
    accessOrder: AccessOrder = AccessOrder.Default
): Sequence<T> = sequence {
    for (vec in shape.iterate(accessOrder)) {
        yield(this@iterWithOrder[vec])
    }
}

/**
 * 使用指定访问顺序迭代 AbstractMultiArray
 * Iterate over AbstractMultiArray with specified access order
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
 */
fun <T : Any, S : Shape> AbstractMultiArray<T, S>.flatten(
    accessOrder: AccessOrder = AccessOrder.Default
): List<T> {
    return iterWithOrder(accessOrder).toList()
}

/**
 * 从列表创建多维数组
 * Create multi-dimensional array from list
 */
fun <T : Any, S : Shape> MultiArray.Companion.fromList(
    shape: S,
    list: List<T>,
    accessOrder: AccessOrder = AccessOrder.Default
): MultiArray<T, S> {
    require(list.size == shape.size) {
        "List size (${list.size}) must match shape size (${shape.size})"
    }

    val array = MutableMultiArray(shape) { _, _ -> list[0] }
    var index = 0
    for (vec in shape.iterate(accessOrder)) {
        array[vec] = list[index++]
    }
    return array.toImmutable()
}

/**
 * 从列表创建可变多维数组
 * Create mutable multi-dimensional array from list
 */
fun <T : Any, S : Shape> MutableMultiArray.Companion.fromList(
    shape: S,
    list: List<T>,
    accessOrder: AccessOrder = AccessOrder.Default
): MutableMultiArray<T, S> {
    require(list.size == shape.size) {
        "List size (${list.size}) must match shape size (${shape.size})"
    }

    val array = MutableMultiArray(shape) { _, _ -> list[0] }
    var index = 0
    for (vec in shape.iterate(accessOrder)) {
        array[vec] = list[index++]
    }
    return array
}
