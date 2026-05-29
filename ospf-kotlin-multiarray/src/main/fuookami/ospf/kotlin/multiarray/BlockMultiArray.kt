/**
 * 分块多维数组模块
 * Block Multi-dimensional Array Module
 *
 * 本模块提供基于分块存储的稀疏多维数组实现。
 * This module provides sparse multi-dimensional array implementation
 * based on block storage.
 *
 * 特性：
 * Features:
 * - 稀疏存储：仅存储非默认值元素
 *   Sparse storage: Only stores non-default value elements
 * - 内存效率：适用于大规模稀疏数组
 *   Memory efficiency: Suitable for large-scale sparse arrays
 * - 延迟初始化：支持增量添加元素
 *   Lazy initialization: Supports incremental element addition
 *
 * 使用场景：
 * Use cases:
 * - 大规模稀疏矩阵
 *   Large-scale sparse matrices
 * - 仅少数元素非零的数组
 *   Arrays where most elements are zero/default
 * - 内存敏感的应用
 *   Memory-sensitive applications
 *
 * 示例：
 * Example:
 * ```kotlin
 * // 创建稀疏数组
 * // Create sparse array
 * val sparse = BlockMultiArray.empty<Int, Shape3>(Shape3(100, 100, 100))
 *
 * // 设置非零值
 * // Set non-zero values
 * sparse[intArrayOf(0, 0, 0)] = 1
 * sparse[intArrayOf(50, 50, 50)] = 2
 *
 * // 转换为稠密数组
 * // Convert to dense array
 * val dense = sparse.toMultiArray(defaultValue = 0)
 * ```
 *
 * @see MultiArray
 * @see Shape
 */
package fuookami.ospf.kotlin.multiarray

private class IndexKey private constructor(
    private val indices: IntArray,
    private val hash: Int
) {
    companion object {
        fun persistent(indices: IntArray): IndexKey {
            return IndexKey(indices.copyOf(), indices.contentHashCode())
        }

        fun transient(indices: IntArray): IndexKey {
            return IndexKey(indices, indices.contentHashCode())
        }
    }

    fun asIntArray(): IntArray = indices

    fun toListKey(): List<Int> = indices.toList()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is IndexKey) {
            return false
        }
        return indices.contentEquals(other.indices)
    }

    override fun hashCode(): Int = hash
}

private object InternalIndexKeyBlocks

/**
 * 将 List<Int> 键的 Map 转换为 IndexKey 键的 Map
 * Convert Map with List<Int> keys to Map with IndexKey keys
 *
 * @return 以 IndexKey 为键的可变映射 / Mutable map with IndexKey as keys
 */
private fun <T : Any> Map<List<Int>, T>.toIndexKeyBlocks(): MutableMap<IndexKey, T> {
    val converted = LinkedHashMap<IndexKey, T>(size)
    for ((indices, value) in this) {
        converted[IndexKey.persistent(indices.toIntArray())] = value
    }
    return converted
}

/**
 * BlockMultiArray - 分块存储的多维数组
 * BlockMultiArray - Multi-dimensional array with block storage
 *
 * @param T 元素类型 / Element type
 * @param S 形状类型 / Shape type
 * @param shape 数组形状 / Array shape
 * @param blocks 分块存储映射 / Block storage map
 */
class BlockMultiArray<T : Any, S : Shape> private constructor(
    val shape: S,
    private val blocks: MutableMap<IndexKey, T>,
    @Suppress("UNUSED_PARAMETER")
    internalBlocks: InternalIndexKeyBlocks
) : Collection<T> {
    constructor(shape: S) : this(shape, mutableMapOf(), InternalIndexKeyBlocks)

    constructor(
        shape: S,
        blocks: Map<List<Int>, T>
    ) : this(shape, blocks.toIndexKeyBlocks(), InternalIndexKeyBlocks)

    /**
     * 获取元素
     * Get element
     *
     * @param indices 向量索引 / Vector indices
     * @return 元素值，如果不存在则返回 null / Element value, or null if not exists
     */
    operator fun get(vararg indices: Int): T? {
        return blocks[IndexKey.transient(indices)]
    }

    /**
     * 设置元素
     * Set element
     *
     * @param indices 向量索引 / Vector indices
     * @param value 要设置的值 / Value to set
     */
    operator fun set(indices: IntArray, value: T) {
        blocks[IndexKey.persistent(indices)] = value
    }

    /**
     * 获取或设置默认值
     * Get or set default value
     *
     * @param indices 向量索引 / Vector indices
     * @param defaultValue 默认值生成器 / Default value generator
     * @return 元素值 / Element value
     */
    fun getOrSet(indices: IntArray, defaultValue: () -> T): T {
        val key = IndexKey.transient(indices)
        val existed = blocks[key]
        if (existed != null) {
            return existed
        }
        val value = defaultValue()
        blocks[IndexKey.persistent(indices)] = value
        return value
    }

    /**
     * 检查是否包含指定索引的值
     * Check if value at specified index exists
     *
     * @param indices 向量索引 / Vector indices
     * @return 是否存在 / Whether exists
     */
    fun contains(indices: IntArray): Boolean {
        return blocks.containsKey(IndexKey.transient(indices))
    }

    /**
     * 移除指定索引的元素
     * Remove element at specified index
     *
     * @param indices 向量索引 / Vector indices
     * @return 被移除的元素，如果不存在则返回 null / Removed element, or null if not exists
     */
    fun remove(indices: IntArray): T? {
        return blocks.remove(IndexKey.transient(indices))
    }

    /**
     * 清除所有元素
     * Clear all elements
     */
    fun clear() {
        blocks.clear()
    }

    /**
     * 已存储的元素数量
     * Number of stored elements
     */
    override val size: Int get() = blocks.size

    /**
     * 检查是否为空
     * Check if empty
     */
    override fun isEmpty(): Boolean = blocks.isEmpty()

    /**
     * 迭代器 - 只迭代已存储的值
     * Iterator - only iterates stored values
     */
    override fun iterator(): Iterator<T> = blocks.values.iterator()

    /**
     * 检查是否包含所有元素
     * Check if contains all elements
     */
    override fun containsAll(elements: Collection<T>): Boolean {
        return blocks.values.containsAll(elements)
    }

    /**
     * 检查是否包含指定元素
     * Check if contains specified element
     */
    override fun contains(element: T): Boolean {
        return blocks.values.contains(element)
    }

    /**
     * 获取所有已存储的索引
     * Get all stored indices
     *
     * @return 已存储的索引集合 / Set of stored indices
     */
    fun indices(): Set<List<Int>> = blocks.keys
        .mapTo(LinkedHashSet(blocks.size)) { it.toListKey() }

    /**
     * 转换为 MultiArray
     * Convert to MultiArray
     *
     * @param defaultValue 默认填充值 / Default fill value
     * @return 不可变多维数组 / Immutable multi-array
     */
    fun toMultiArray(defaultValue: T): MultiArray<T, S> {
        // Initialize with default value instead of uninitialized
        val array = MutableMultiArray.newWith(shape, defaultValue)

        // Override with stored values
        for ((key, value) in blocks) {
            array[key.asIntArray()] = value
        }

        return array.toImmutable()
    }

    companion object {
        /**
         * 从 MultiArray 创建 BlockMultiArray
         * Create BlockMultiArray from MultiArray
         *
         * @param array 源多维数组 / Source multi-array
         * @param filter 元素过滤器，仅存储满足条件的元素 / Element filter, only stores elements satisfying the condition
         * @return 分块多维数组 / Block multi-array
         */
        fun <T : Any, S : Shape> fromMultiArray(
            array: MultiArray<T, S>,
            filter: (T) -> Boolean = { true }
        ): BlockMultiArray<T, S> {
            val blocks = mutableMapOf<IndexKey, T>()
            for (i in 0 until array.shape.size) {
                val vector = array.shape.vector(i)
                val value = array[vector]
                if (filter(value)) {
                    blocks[IndexKey.persistent(vector)] = value
                }
            }
            return BlockMultiArray(array.shape, blocks, InternalIndexKeyBlocks)
        }

        /**
         * 创建空的 BlockMultiArray
         * Create empty BlockMultiArray
         *
         * @param shape 数组形状 / Array shape
         * @return 空的分块多维数组 / Empty block multi-array
         */
        fun <T : Any, S : Shape> empty(shape: S): BlockMultiArray<T, S> {
            return BlockMultiArray(shape)
        }
    }
}

/** 可变分块多维数组类型别名 / Mutable block multi-array type alias */
typealias MutableBlockMultiArray<T, S> = BlockMultiArray<T, S>

/** 一维分块多维数组类型别名 / 1D block multi-array type alias */
typealias BlockMultiArray1<T> = BlockMultiArray<T, Shape1>

/** 二维分块多维数组类型别名 / 2D block multi-array type alias */
typealias BlockMultiArray2<T> = BlockMultiArray<T, Shape2>

/** 三维分块多维数组类型别名 / 3D block multi-array type alias */
typealias BlockMultiArray3<T> = BlockMultiArray<T, Shape3>

/** 四维分块多维数组类型别名 / 4D block multi-array type alias */
typealias BlockMultiArray4<T> = BlockMultiArray<T, Shape4>

/** 动态维度分块多维数组类型别名 / Dynamic dimension block multi-array type alias */
typealias BlockDynMultiArray<T> = BlockMultiArray<T, DynShape>
