package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.math.UInt64

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
        return origin[actualVector(shape.vector(i))]
    }

    /**
     * 通过 UInt64 线性索引获取元素
     * Get element by UInt64 linear index
     */
    operator fun get(i: UInt64): T {
        return get(i.toInt())
    }

    /**
     * 通过 Indexed 接口获取元素
     * Get element by Indexed interface
     */
    operator fun get(e: Indexed): T {
        return origin[actualVector(shape.vector(e.index))]
    }

    /**
     * 通过向量索引获取元素
     * Get element by vector index
     */
    @JvmName("getByIntArray")
    operator fun get(v: IntArray): T {
        return origin[actualVector(v)]
    }

    /**
     * 通过可变参数获取元素
     * Get element by vararg
     */
    @JvmName("getByInts")
    operator fun get(vararg v: Int): T {
        return origin[actualVector(v)]
    }

    /**
     * 通过 UInt64 迭代获取元素
     * Get element by UInt64 iterable
     */
    operator fun get(v: Iterable<UInt64>): T {
        return origin[actualVector(v.map { it.toInt() }.toIntArray())]
    }

    /**
     * 通过 Indexed 可变参数获取元素
     * Get element by Indexed vararg
     */
    operator fun get(vararg v: Indexed): T {
        return origin[actualVector(v.map { it.index }.toIntArray())]
    }

    /**
     * 通过任意类型数组创建子视图
     * Create sub-view by any type array
     */
    operator fun get(vararg v: Any): MultiArrayView<T, S> {
        val newDummyVector = ArrayList<DummyIndex>()
        val subDummyVector = shape.dummyVector(*v)
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
    private fun actualVector(v: IntArray): IntArray {
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
                yield(origin[mapVector(shape.vector(i))])
            }
        }
    }

    operator fun get(i: Int): T {
        return origin[mapVector(shape.vector(i))]
    }

    operator fun get(i: UInt64): T {
        return get(i.toInt())
    }

    operator fun get(e: Indexed): T {
        return origin[mapVector(shape.vector(e.index))]
    }

    @JvmName("getByIntArray")
    operator fun get(v: IntArray): T {
        return origin[mapVector(v)]
    }

    @JvmName("getByInts")
    operator fun get(vararg v: Int): T {
        return origin[mapVector(v)]
    }

    operator fun get(vararg v: Any): MappedMultiArrayView<T, S> {
        val newMapVector = ArrayList<MapIndex>()
        val subDummyVector = shape.dummyVector(*v)
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

    private fun mapVector(v: IntArray): IntArray {
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