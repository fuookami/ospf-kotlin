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

/**
 * BlockMultiArray - 分块存储的多维数组
 * BlockMultiArray - Multi-dimensional array with block storage
 * 
 * @param T 元素类型
 * @param S 形状类型
 * @param blocks 分块列表
 */
class BlockMultiArray<T : Any, S : Shape> private constructor(
    val shape: S,
    private val blocks: MutableMap<IndexKey, T>
) : Collection<T> {
    constructor(shape: S) : this(shape, mutableMapOf())

    /**
     * 获取元素
     */
    operator fun get(vararg indices: Int): T? {
        return blocks[IndexKey.transient(indices)]
    }

    /**
     * 设置元素
     * Set element
     */
    operator fun set(indices: IntArray, value: T) {
        blocks[IndexKey.persistent(indices)] = value
    }

    /**
     * 获取或设置默认值
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
     * 检查是否包含值
     */
    fun contains(indices: IntArray): Boolean {
        return blocks.containsKey(IndexKey.transient(indices))
    }

    /**
     * 移除元素
     */
    fun remove(indices: IntArray): T? {
        return blocks.remove(IndexKey.transient(indices))
    }

    /**
     * 清除所有元素
     */
    fun clear() {
        blocks.clear()
    }

    /**
     * 获取已存储的元素数量
     */
    override val size: Int get() = blocks.size

    /**
     * 检查是否为空
     */
    override fun isEmpty(): Boolean = blocks.isEmpty()

    /**
     * 迭代器 - 只迭代已存储的值
     */
    override fun iterator(): Iterator<T> = blocks.values.iterator()

    /**
     * 检查是否包含所有元素
     */
    override fun containsAll(elements: Collection<T>): Boolean {
        return blocks.values.containsAll(elements)
    }

    /**
     * 检查是否包含元素
     */
    override fun contains(element: T): Boolean {
        return blocks.values.contains(element)
    }

    /**
     * 获取所有已存储的索引
     */
    fun indices(): Set<List<Int>> = blocks.keys
        .mapTo(LinkedHashSet(blocks.size)) { it.toListKey() }

    /**
     * 转换为 MultiArray
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
            return BlockMultiArray(array.shape, blocks)
        }

        /**
         * 创建空的 BlockMultiArray
         */
        fun <T : Any, S : Shape> empty(shape: S): BlockMultiArray<T, S> {
            return BlockMultiArray(shape)
        }
    }
}

/**
 * MutableBlockMultiArray - 可变的分块存储多维数组
 */
typealias MutableBlockMultiArray<T, S> = BlockMultiArray<T, S>

/**
 * 类型别名
 */
typealias BlockMultiArray1<T> = BlockMultiArray<T, Shape1>
typealias BlockMultiArray2<T> = BlockMultiArray<T, Shape2>
typealias BlockMultiArray3<T> = BlockMultiArray<T, Shape3>
typealias BlockMultiArray4<T> = BlockMultiArray<T, Shape4>
typealias BlockDynMultiArray<T> = BlockMultiArray<T, DynShape>
