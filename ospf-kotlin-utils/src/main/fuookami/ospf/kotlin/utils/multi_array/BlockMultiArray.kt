package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.concept.Indexed

/**
 * BlockMultiArray - 分块存储的多维数组
 * 
 * 使用分块列表作为底层存储，适用于稀疏或大型数组
 * 
 * @param T 元素类型
 * @param S 形状类型
 * @param blocks 分块列表
 */
class BlockMultiArray<T : Any, S : Shape>(
    val shape: S,
    private val blocks: MutableMap<List<Int>, T> = mutableMapOf()
) : Collection<T> {
    
    /**
     * 获取元素
     */
    operator fun get(vararg indices: Int): T? {
        return blocks[indices.toList()]
    }
    
    /**
     * 设置元素
     * Set element
     */
    operator fun set(indices: IntArray, value: T) {
        blocks[indices.toList()] = value
    }
    
    /**
     * 获取或设置默认值
     */
    fun getOrSet(indices: IntArray, defaultValue: () -> T): T {
        return blocks.getOrPut(indices.toList()) { defaultValue() }
    }
    
    /**
     * 检查是否包含值
     */
    fun contains(indices: IntArray): Boolean {
        return blocks.containsKey(indices.toList())
    }
    
    /**
     * 移除元素
     */
    fun remove(indices: IntArray): T? {
        return blocks.remove(indices.toList())
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
    
    /**
     * 转换为 MultiArray
     */
    fun toMultiArray(defaultValue: T): MultiArray<T, S> {
        val array = MutableMultiArray<T, S>(shape)
        for ((indices, value) in blocks) {
            array[indices.toIntArray()] = value
        }
        // 填充默认值
        for (i in 0 until shape.size) {
            val vector = shape.vector(i)
            if (!blocks.containsKey(vector.toList())) {
                array[vector] = defaultValue
            }
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
            val blocks = mutableMapOf<List<Int>, T>()
            for (i in 0 until array.shape.size) {
                val vector = array.shape.vector(i)
                val value = array[vector]
                if (filter(value)) {
                    blocks[vector.toList()] = value
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