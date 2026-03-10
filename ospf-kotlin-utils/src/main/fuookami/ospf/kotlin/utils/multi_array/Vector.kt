package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.math.*

/**
 * 虚拟索引范围接口
 * Dummy index range interface
 *
 * 定义虚拟索引范围的行为，支持动态克隆。
 * Defines behavior for dummy index ranges, supporting dynamic cloning.
 */
interface DummyIndexRange {
    /**
     * 获取范围的起始边界
     * Get the start bound of the range
     */
    fun start(): Int?
    
    /**
     * 获取范围的结束边界
     * Get the end bound of the range
     */
    fun end(): Int?
    
    /**
     * 是否包含结束边界
     * Whether the end bound is inclusive
     */
    fun isInclusive(): Boolean = false
    
    /**
     * 检查值是否在范围内
     * Check if a value is contained in the range
     */
    fun contains(v: Int, len: Int): Boolean
}

/**
 * 虚拟索引
 * Dummy index
 *
 * 表示多维数组的索引，支持三种形式：
 * Represents indices for multi-dimensional arrays, supporting three forms:
 *
 * - `Index`: 单个索引，支持负数（从末尾计数）
 *   Single index, supports negative numbers (counting from the end)
 * - `Range`: 范围索引，如 `0..5`, `1..=3`, `..`, `2..`
 *   Range index, e.g., `0..5`, `1..=3`, `..`, `2..`
 * - `IndexArray`: 离散索引数组，如 `[0, 2, 4]`
 *   Discrete index array, e.g., `[0, 2, 4]`
 * - `All`: 全范围索引
 *   Full range index
 */
sealed class DummyIndex {
    /**
     * 单个索引
     * Single index
     */
    data class Index(val index: Int) : DummyIndex()
    
    /**
     * 范围索引
     * Range index
     */
    data class Range(val range: DummyIndexRange) : DummyIndex()
    
    /**
     * 索引数组
     * Index array
     */
    data class IndexArray(val indices: List<Int>) : DummyIndex()
    
    /**
     * 全范围索引
     * Full range index
     */
    data object All : DummyIndex()
    
    /**
     * 计算虚拟索引在给定维度上的长度
     * Calculate the length of dummy index in a given dimension
     *
     * @param shape 形状
     * @param dimension 维度索引
     * @return 该维度上的索引数量
     */
    fun lenOf(shape: Shape, dimension: Int): Int {
        return when (this) {
            is Index -> 1
            is Range -> {
                val len = shape[dimension]
                val start = actualBound(range.start(), len) ?: 0
                val end = actualBound(range.end(), len) ?: len
                maxOf(0, end - start)
            }
            is IndexArray -> indices.size
            is All -> shape[dimension]
        }
    }
    
    /**
     * 将虚拟索引转换为迭代器
     * Convert dummy index to an iterator
     *
     * @param shape 形状
     * @param dimension 维度索引
     * @return 索引迭代器
     */
    fun iteratorOf(shape: Shape, dimension: Int): DummyIndexIterator {
        return when (this) {
            is Index -> {
                val actualIndex = shape.actualIndex(dimension, index)
                if (actualIndex != null) {
                    DummyIndexIterator.Single(actualIndex)
                } else {
                    DummyIndexIterator.Single(0)
                }
            }
            is Range -> {
                val len = shape[dimension]
                val start = actualBound(range.start(), len) ?: 0
                val end = actualBound(range.end(), len) ?: len
                if (start < end) {
                    DummyIndexIterator.Continuous(start until end)
                } else {
                    DummyIndexIterator.Continuous(0 until 0)
                }
            }
            is IndexArray -> {
                val validIndices = indices.mapNotNull { shape.actualIndex(dimension, it) }
                DummyIndexIterator.Discrete(validIndices)
            }
            is All -> {
                DummyIndexIterator.Continuous(0 until shape[dimension])
            }
        }
    }
    
    private fun actualBound(bound: Int?, len: Int): Int? {
        if (bound == null) return null
        return if (bound >= 0) {
            bound.coerceAtMost(len)
        } else {
            (len + bound).coerceAtLeast(0)
        }
    }
    
    companion object {
        /**
         * 从整数值创建单索引
         * Create single index from integer value
         */
        fun from(value: Int): DummyIndex = Index(value)
        
        /**
         * 从 IntRange 创建范围索引
         * Create range index from IntRange
         */
        fun from(range: IntRange): DummyIndex = Range(object : DummyIndexRange {
            override fun start() = range.first
            override fun end() = range.last + 1
            override fun contains(v: Int, len: Int) = v in range
        })
        
        /**
         * 从整数列表创建索引数组
         * Create index array from integer list
         */
        fun from(indices: List<Int>): DummyIndex = IndexArray(indices)
        
        /**
         * 创建全范围索引
         * Create full range index
         */
        fun all(): DummyIndex = All
    }
}

/**
 * 虚拟索引迭代器
 * Dummy index iterator
 *
 * 表示虚拟索引的迭代结果，支持三种模式：
 * Represents the iteration result of dummy indices, supporting three modes:
 */
sealed class DummyIndexIterator {
    /**
     * 单个索引
     * Single index
     */
    data class Single(val index: Int) : DummyIndexIterator()
    
    /**
     * 连续范围索引
     * Continuous range indices
     */
    data class Continuous(val range: IntRange) : DummyIndexIterator()
    
    /**
     * 离散索引集合
     * Discrete index collection
     */
    data class Discrete(val indices: List<Int>) : DummyIndexIterator()
    
    /**
     * 获取指定位置的索引值
     * Get the index value at the specified position
     *
     * @param i 位置索引
     * @return 该位置的索引值，如果超出范围则返回 null
     */
    fun get(i: Int): Int? {
        return when (this) {
            is Single -> if (i == 0) index else null
            is Continuous -> if (i < range.count()) range.first + i else null
            is Discrete -> indices.getOrNull(i)
        }
    }
    
    /**
     * 获取迭代器的长度
     * Get the length of the iterator
     */
    fun len(): Int {
        return when (this) {
            is Single -> 1
            is Continuous -> range.count()
            is Discrete -> indices.size
        }
    }
    
    /**
     * 检查迭代器是否为空
     * Check if the iterator is empty
     */
    fun isEmpty(): Boolean = len() == 0
}

/**
 * 映射索引
 * Map index
 *
 * 用于维度转置和重映射操作。
 * Used for dimension transposition and remapping operations.
 */
sealed class MapIndex {
    /**
     * 虚拟索引
     * Dummy index
     */
    data class Dummy(val dummy: DummyIndex) : MapIndex()
    
    /**
     * 映射占位符
     * Map placeholder
     *
     * @param index 映射到的目标维度索引
     */
    data class Map(val index: Int) : MapIndex()
    
    companion object {
        /**
         * 从 DummyIndex 创建 MapIndex
         * Create MapIndex from DummyIndex
         */
        fun from(dummy: DummyIndex): MapIndex = Dummy(dummy)
        
        /**
         * 创建映射占位符
         * Create map placeholder
         */
        fun map(index: Int): MapIndex = Map(index)
    }
}

/**
 * 全范围虚拟索引的便捷访问对象
 * Convenience access object for full range dummy index
 */
val _a: DummyIndex get() = DummyIndex.All

/**
 * 虚拟向量类型别名
 * Dummy vector type alias
 */
typealias DummyVector = List<DummyIndex>

/**
 * 映射向量类型别名
 * Map vector type alias
 */
typealias MapVector = List<MapIndex>

/**
 * 迭代器向量类型别名
 * Iterator vector type alias
 */
typealias IteratorVector = List<DummyIndexIterator>